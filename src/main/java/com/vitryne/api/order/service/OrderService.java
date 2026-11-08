package com.vitryne.api.order.service;

import com.vitryne.api.cart.entity.Cart;
import com.vitryne.api.cart.entity.CartItem;
import com.vitryne.api.cart.repository.CartItemRepository;
import com.vitryne.api.cart.repository.CartRepository;
import com.vitryne.api.order.dto.CheckoutRequestDTO;
import com.vitryne.api.order.dto.CheckoutResponseDTO;
import com.vitryne.api.order.dto.OrderResponseDTO;
import com.vitryne.api.order.dto.UpdateOrderStatusRequestDTO;
import com.vitryne.api.order.entity.Order;
import com.vitryne.api.order.exception.OrderNotFoundException;
import com.vitryne.api.order.repository.OrderRepository;
import com.vitryne.api.payment.dto.PaymentResponseDTO;
import com.vitryne.api.payment.entity.Payment;
import com.vitryne.api.payment.service.PaymentSimulator;
import com.vitryne.api.product.entity.Product;
import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.stock.repository.StockRepository;
import com.vitryne.api.store.entity.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class OrderService {

    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");
    private static final int MAX_INTEGER_DIGITS = 10;

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final StockRepository stockRepository;
    private final PaymentSimulator paymentSimulator;

    /**
     * Converte o carrinho do cliente em pedido.
     *
     * O valor é sempre recalculado no backend com o preço atual dos produtos.
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
        Map<Long, Stock> lockedStocks = new LinkedHashMap<>();
        stockRepository.lockAllByIdForUpdate(stockIds)
                .forEach(stock -> lockedStocks.put(stock.getId(), stock));

        Cart cart = cartRepository.findByUserId(userId)
                .filter(found -> !found.getItems().isEmpty())
                .orElseThrow(OrderService::emptyCart);

        List<CheckoutLine> lines = cart.getItems().stream()
                .map(item -> toCheckoutLine(item, lockedStocks))
                .toList();

        Store store = resolveSingleStore(lines);
        BigDecimal total = calculateTotal(lines);

        Payment payment = paymentSimulator.process(
                userId,
                total,
                request.paymentMethod(),
                request.simulatedResult()
        );

        if (!payment.isApproved()) {
            return new CheckoutResponseDTO(PaymentResponseDTO.from(payment), null);
        }

        Order order = Order.confirm(userId, store, payment);
        for (CheckoutLine line : lines) {
            line.stock().decreaseStock(line.quantity());
            order.addItem(line.stock(), line.quantity(), line.unitPrice());
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

    private CheckoutLine toCheckoutLine(CartItem item, Map<Long, Stock> lockedStocks) {
        Stock stock = lockedStocks.get(item.getStock().getId());
        if (stock == null) {
            // Item incluído entre a leitura dos ids e a carga do carrinho
            // não é possível com o lock do usuário; tratado por segurança.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O carrinho foi alterado. Atualize-o e tente novamente."
            );
        }

        Product product = stock.getProduct();

        if (!"ACTIVE".equals(product.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O produto " + product.getName() + " não está mais ativo"
            );
        }

        if (item.getQuantity() > stock.getQuantity()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    String.format(
                            "Estoque insuficiente para %s (tamanho %s): disponível %d, solicitado %d",
                            product.getName(),
                            stock.getSize(),
                            stock.getQuantity(),
                            item.getQuantity()
                    )
            );
        }

        return new CheckoutLine(stock, item.getQuantity(), product.calculateFinalPrice());
    }

    private Store resolveSingleStore(List<CheckoutLine> lines) {
        Store store = lines.getFirst().stock().getProduct().getStore();

        boolean multipleStores = lines.stream()
                .map(line -> line.stock().getProduct().getStore().getId())
                .anyMatch(storeId -> !store.getId().equals(storeId));

        if (multipleStores) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O carrinho contém produtos de mais de uma loja"
            );
        }

        return store;
    }

    private BigDecimal calculateTotal(List<CheckoutLine> lines) {
        BigDecimal total = lines.stream()
                .map(CheckoutLine::subtotal)
                .reduce(ZERO_MONEY, BigDecimal::add);

        if (total.precision() - total.scale() > MAX_INTEGER_DIGITS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O valor total do pedido excede o limite permitido"
            );
        }

        return total;
    }

    private static ResponseStatusException emptyCart() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "O carrinho está vazio"
        );
    }

    private record CheckoutLine(Stock stock, int quantity, BigDecimal unitPrice) {
        BigDecimal subtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
