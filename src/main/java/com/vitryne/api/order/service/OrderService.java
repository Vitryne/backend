package com.vitryne.api.order.service;

import com.vitryne.api.cart.entity.Cart;
import com.vitryne.api.cart.repository.CartItemRepository;
import com.vitryne.api.cart.repository.CartRepository;
import com.vitryne.api.client.entity.ClientAddress;
import com.vitryne.api.client.service.ClientAddressService;
import com.vitryne.api.order.dto.CancelOrderRequestDTO;
import com.vitryne.api.order.dto.CheckoutRequestDTO;
import com.vitryne.api.order.dto.CheckoutResponseDTO;
import com.vitryne.api.order.dto.CheckoutReviewResponseDTO;
import com.vitryne.api.order.dto.OrderResponseDTO;
import com.vitryne.api.order.dto.RejectOrderRequestDTO;
import com.vitryne.api.order.dto.UpdateOrderStatusRequestDTO;
import com.vitryne.api.order.entity.CheckoutIdempotency;
import com.vitryne.api.order.entity.Order;
import com.vitryne.api.order.entity.OrderItem;
import com.vitryne.api.order.exception.OrderNotFoundException;
import com.vitryne.api.order.repository.CheckoutIdempotencyRepository;
import com.vitryne.api.order.repository.OrderRepository;
import com.vitryne.api.payment.dto.PaymentResponseDTO;
import com.vitryne.api.payment.entity.Payment;
import com.vitryne.api.payment.service.PaymentSimulator;
import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OrderService {

    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9_-]{8,100}");

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final StockRepository stockRepository;
    private final PaymentSimulator paymentSimulator;
    private final ClientAddressService clientAddressService;
    private final CheckoutEvaluator checkoutEvaluator;
    private final OrderAcceptancePolicy acceptancePolicy;
    private final CheckoutIdempotencyRepository checkoutIdempotencyRepository;

    /**
     * Revisão da compra antes da confirmação. Somente leitura: não cria pedido,
     * não baixa estoque e não altera o carrinho.
     */
    @Transactional(readOnly = true)
    public CheckoutReviewResponseDTO review(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .filter(found -> !found.getItems().isEmpty())
                .orElseThrow(OrderService::emptyCart);

        Optional<ClientAddress> deliveryAddress = clientAddressService.findDeliveryAddress(userId);

        return CheckoutReviewResponseDTO.from(
                checkoutEvaluator.evaluate(cart, deliveryAddress.map(ClientAddress::getAddress).orElse(null)),
                deliveryAddress.map(ClientAddress::getId).orElse(null)
        );
    }

    /**
     * Converte o carrinho em pedido com os valores da revisão.
     *
     * Os valores são recalculados e precisam gerar o mesmo reviewToken
     * devolvido na revisão; caso contrário o cliente deve revisar de novo.
     * O pagamento simulado é registrado mesmo quando recusado; nesse caso
     * nenhum pedido é criado e carrinho e estoque permanecem inalterados.
     *
     * A Idempotency-Key identifica a confirmação dentro do cliente: repetir a
     * mesma chave com os mesmos dados devolve o resultado já registrado, sem
     * novo pagamento, pedido ou baixa de estoque. Reutilizá-la com outros
     * dados é recusado.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CheckoutResponseDTO checkout(Long userId, String idempotencyKey, CheckoutRequestDTO request) {
        validateIdempotencyKey(idempotencyKey);
        String requestHash = requestHash(request);

        // Mesmo lock das operações do carrinho: serializa checkout e edições,
        // inclusive repetições simultâneas da mesma confirmação.
        cartRepository.lockUserForCartMutation(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuário autenticado não encontrado"
                ));

        // Consultado depois do lock: uma repetição concorrente só chega aqui
        // quando a primeira já gravou (ou desfez) seu resultado.
        Optional<CheckoutIdempotency> previous =
                checkoutIdempotencyRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (previous.isPresent()) {
            return replay(previous.get(), requestHash);
        }

        List<Long> stockIds = cartItemRepository.findStockIdsByUserId(userId);
        if (stockIds.isEmpty()) {
            throw emptyCart();
        }

        // Trava as variações antes de carregar o carrinho, para que as
        // quantidades lidas sejam as atuais e não mudem até o commit.
        stockRepository.lockAllByIdForUpdate(stockIds);

        Cart cart = cartRepository.findByUserId(userId)
                .filter(found -> !found.getItems().isEmpty())
                .orElseThrow(OrderService::emptyCart);

        CheckoutEvaluator.Evaluation evaluation =
                checkoutEvaluator.evaluate(
                        cart,
                        clientAddressService.findDeliveryAddress(userId)
                                .map(ClientAddress::getAddress)
                                .orElse(null)
                );

        if (!evaluation.canConfirm()) {
            String reasons = evaluation.issues().stream()
                    .filter(issue -> issue.type().isBlocking())
                    .map(CheckoutEvaluator.Issue::message)
                    .collect(Collectors.joining("; "));

            throw new ResponseStatusException(HttpStatus.CONFLICT, reasons);
        }

        if (!evaluation.reviewToken().equals(request.reviewToken())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Os valores do pedido mudaram desde a revisão. Revise novamente antes de confirmar."
            );
        }

        Payment payment = paymentSimulator.process(
                userId,
                evaluation.totalAmount(),
                request.paymentMethod(),
                request.simulatedResult()
        );

        checkoutIdempotencyRepository.save(
                new CheckoutIdempotency(userId, idempotencyKey, requestHash, payment)
        );

        if (!payment.isApproved()) {
            return new CheckoutResponseDTO(PaymentResponseDTO.from(payment), null, false);
        }

        Order order = Order.place(
                userId,
                evaluation.store(),
                payment,
                evaluation.deliveryAddress(),
                evaluation.shippingAmount(),
                acceptancePolicy.deadlineFrom(Instant.now())
        );

        for (CheckoutEvaluator.Line line : evaluation.lines()) {
            line.stock().decreaseStock(line.quantity());
            order.addItem(line.stock(), line.quantity(), line.currentUnitPrice());
        }

        Order savedOrder = orderRepository.saveAndFlush(order);

        cart.getItems().clear();
        cart.setTotalValueForecast(ZERO_MONEY);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.saveAndFlush(cart);

        return new CheckoutResponseDTO(
                PaymentResponseDTO.from(payment),
                OrderResponseDTO.from(savedOrder),
                false
        );
    }

    private CheckoutResponseDTO replay(CheckoutIdempotency previous, String requestHash) {
        if (!previous.matches(requestHash)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "A Idempotency-Key já foi usada em outra confirmação. "
                            + "Gere uma nova chave para uma nova tentativa."
            );
        }

        Payment payment = previous.getPayment();
        OrderResponseDTO order = payment.isApproved()
                ? orderRepository.findByPaymentId(payment.getId())
                        .map(OrderResponseDTO::from)
                        .orElseThrow(() -> new IllegalStateException(
                                "Pagamento aprovado sem pedido: " + payment.getId()))
                : null;

        return new CheckoutResponseDTO(PaymentResponseDTO.from(payment), order, true);
    }

    private static void validateIdempotencyKey(String key) {
        if (key == null || !IDEMPOTENCY_KEY.matcher(key).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O header Idempotency-Key é obrigatório e deve ter de 8 a 100 caracteres "
                            + "(letras, números, - ou _). Use um UUID por confirmação."
            );
        }
    }

    /** Impressão digital do corpo da confirmação; compara repetições da mesma chave. */
    private static String requestHash(CheckoutRequestDTO request) {
        String canonical = "v1|token=" + request.reviewToken()
                + "|method=" + request.paymentMethod()
                + "|result=" + Objects.toString(request.simulatedResult(), "");

        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> listByCustomer(Long userId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO findByCustomer(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .map(OrderResponseDTO::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> listByStoreOwner(Long ownerId) {
        return orderRepository.findAllByStoreOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(OrderResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO findByStoreOwner(Long ownerId, Long orderId) {
        return orderRepository.findByIdAndStoreOwnerId(orderId, ownerId)
                .map(OrderResponseDTO::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /**
     * Avança o pedido da loja para a próxima etapa, inclusive o aceite
     * (AWAITING_ACCEPTANCE → CONFIRMED).
     */
    @Transactional
    public OrderResponseDTO updateStatus(
            Long ownerId,
            Long orderId,
            UpdateOrderStatusRequestDTO request
    ) {
        Order order = lockStoreOrder(ownerId, orderId);

        order.advanceTo(request.status());

        return OrderResponseDTO.from(orderRepository.saveAndFlush(order));
    }

    /** Recusa da loja, só para pedidos aguardando aceite; devolve o estoque. */
    @Transactional
    public OrderResponseDTO rejectByStore(Long ownerId, Long orderId, RejectOrderRequestDTO request) {
        Order order = lockStoreOrder(ownerId, orderId);

        cancel(order, () -> order.rejectByStore(request.reason()));

        return OrderResponseDTO.from(orderRepository.saveAndFlush(order));
    }

    /** Cancelamento pelo cliente até PREPARING; devolve o estoque. */
    @Transactional
    public OrderResponseDTO cancelByClient(Long userId, Long orderId, CancelOrderRequestDTO request) {
        orderRepository.lockByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        cancel(order, () -> order.cancelByClient(request.reason()));

        return OrderResponseDTO.from(orderRepository.saveAndFlush(order));
    }

    /**
     * Cancela, pelo sistema, o pedido vencido sem aceite mais antigo e devolve
     * o estoque. Retorna o id cancelado, ou vazio quando não há pedido vencido
     * livre. Usado pela rotina agendada, um pedido por transação.
     */
    @Transactional
    public Optional<Long> expireNextOverdueAcceptance() {
        Instant now = Instant.now();
        Optional<Long> orderId = orderRepository.lockNextOverdueAcceptance(now);
        if (orderId.isEmpty()) {
            return Optional.empty();
        }

        Order order = orderRepository.findWithDetailsById(orderId.get())
                .orElseThrow(() -> new OrderNotFoundException(orderId.get()));

        cancel(order, () -> order.expireAcceptance(now));
        orderRepository.saveAndFlush(order);

        return orderId;
    }

    /**
     * Operação única de cancelamento: aplica a regra do pedido (que valida
     * a etapa) e devolve as quantidades ao estoque.
     *
     * As variações são travadas pelo mesmo lock do checkout, em ordem de id,
     * para que a devolução não concorra com uma compra simultânea.
     */
    private void cancel(Order order, Runnable transition) {
        transition.run();

        Map<Long, Stock> stocks = stockRepository
                .lockAllByIdForUpdate(orderRepository.findStockIdsByOrderId(order.getId()))
                .stream()
                .collect(Collectors.toMap(Stock::getId, Function.identity()));

        for (OrderItem item : order.getItems()) {
            stocks.get(item.getStock().getId()).increaseStock(item.getQuantity());
        }
    }

    /** Trava o pedido antes de ler o status, para que transições concorrentes se serializem. */
    private Order lockStoreOrder(Long ownerId, Long orderId) {
        orderRepository.lockByIdAndStoreOwnerId(orderId, ownerId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return orderRepository.findByIdAndStoreOwnerId(orderId, ownerId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private static ResponseStatusException emptyCart() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "O carrinho está vazio"
        );
    }
}
