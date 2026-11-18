package com.vitryne.api.order.service;

import com.vitryne.api.cart.entity.Cart;
import com.vitryne.api.cart.repository.CartItemRepository;
import com.vitryne.api.cart.repository.CartRepository;
import com.vitryne.api.client.entity.ClientAddress;
import com.vitryne.api.client.service.ClientAddressService;
import com.vitryne.api.order.dto.CheckoutRequestDTO;
import com.vitryne.api.order.dto.CheckoutResponseDTO;
import com.vitryne.api.order.dto.CheckoutReviewResponseDTO;
import com.vitryne.api.order.dto.OrderResponseDTO;
import com.vitryne.api.order.dto.UpdateOrderStatusRequestDTO;
import com.vitryne.api.order.entity.Order;
import com.vitryne.api.order.exception.OrderNotFoundException;
import com.vitryne.api.order.repository.OrderRepository;
import com.vitryne.api.payment.dto.PaymentResponseDTO;
import com.vitryne.api.payment.entity.Payment;
import com.vitryne.api.payment.service.PaymentSimulator;
import com.vitryne.api.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OrderService {

    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final StockRepository stockRepository;
    private final PaymentSimulator paymentSimulator;
    private final ClientAddressService clientAddressService;
    private final CheckoutEvaluator checkoutEvaluator;

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
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CheckoutResponseDTO checkout(Long userId, CheckoutRequestDTO request) {
        // Mesmo lock das operações do carrinho: serializa checkout e edições
        // e impede pedido duplicado por cliques repetidos.
        cartRepository.lockUserForCartMutation(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuário autenticado não encontrado"
                ));

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

        if (!payment.isApproved()) {
            return new CheckoutResponseDTO(PaymentResponseDTO.from(payment), null);
        }

        Order order = Order.confirm(
                userId,
                evaluation.store(),
                payment,
                evaluation.deliveryAddress(),
                evaluation.shippingAmount()
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
                OrderResponseDTO.from(savedOrder)
        );
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

    @Transactional
    public OrderResponseDTO updateStatus(
            Long ownerId,
            Long orderId,
            UpdateOrderStatusRequestDTO request
    ) {
        Order order = orderRepository.findByIdAndStoreOwnerId(orderId, ownerId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.changeStatus(request.status());

        return OrderResponseDTO.from(orderRepository.saveAndFlush(order));
    }

    private static ResponseStatusException emptyCart() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "O carrinho está vazio"
        );
    }
}
