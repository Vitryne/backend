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
import com.vitryne.api.order.enums.OrderStatus;
import com.vitryne.api.order.exception.OrderNotFoundException;
import com.vitryne.api.order.repository.OrderRepository;
import com.vitryne.api.payment.entity.Payment;
import com.vitryne.api.payment.enums.PaymentMethod;
import com.vitryne.api.payment.enums.PaymentResult;
import com.vitryne.api.payment.service.PaymentSimulator;
import com.vitryne.api.product.entity.Product;
import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.stock.repository.StockRepository;
import com.vitryne.api.store.entity.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long OWNER_ID = 99L;
    private static final Long STOCK_P = 101L;
    private static final Long STOCK_M = 102L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private PaymentSimulator paymentSimulator;

    @InjectMocks
    private OrderService service;

    private final Store store = Store.builder().id(1L).name("Loja").build();

    @Test
    void pagamentoAprovadoDeveCriarPedidoBaixarEstoqueEEsvaziarCarrinho() {
        Product product = product(1L, "19.90", "ACTIVE", store);
        Stock p = stock(STOCK_P, product, "P", 5);
        Stock m = stock(STOCK_M, product, "M", 1);
        Cart cart = cart(line(p, 2, "15.00"), line(m, 1, "15.00"));

        prepararCheckout(cart, p, m);
        simularPagamento(PaymentResult.APPROVED);
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        CheckoutResponseDTO response = service.checkout(
                USER_ID, new CheckoutRequestDTO(PaymentMethod.CREDIT_CARD, null)
        );

        // Valor recalculado com o preço atual (19.90), não o do carrinho (15.00).
        verify(paymentSimulator).process(
                eq(USER_ID), argThat(v -> v.compareTo(new BigDecimal("59.70")) == 0),
                eq(PaymentMethod.CREDIT_CARD), isNull()
        );

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).saveAndFlush(captor.capture());
        Order order = captor.getValue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getStore()).isSameAs(store);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("59.70");
        assertThat(order.getItems()).hasSize(2);

        assertThat(p.getQuantity()).isEqualTo(3);
        assertThat(m.getQuantity()).isZero();

        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getTotalValueForecast()).isEqualByComparingTo("0.00");
        verify(cartRepository).saveAndFlush(cart);

        assertThat(response.payment().result()).isEqualTo(PaymentResult.APPROVED);
        assertThat(response.payment().amount()).isEqualByComparingTo("59.70");
        assertThat(response.order()).isNotNull();
        assertThat(response.order().totalAmount()).isEqualByComparingTo("59.70");
    }

    @Test
    void pagamentoRecusadoDeveRegistrarPagamentoSemCriarPedidoNemAlterarCarrinhoOuEstoque() {
        Product product = product(1L, "19.90", "ACTIVE", store);
        Stock p = stock(STOCK_P, product, "P", 5);
        Cart cart = cart(line(p, 2, "19.90"));

        prepararCheckout(cart, p);
        simularPagamento(PaymentResult.DECLINED);

        CheckoutResponseDTO response = service.checkout(
                USER_ID, new CheckoutRequestDTO(PaymentMethod.PIX, PaymentResult.DECLINED)
        );

        assertThat(response.order()).isNull();
        assertThat(response.payment().result()).isEqualTo(PaymentResult.DECLINED);
        assertThat(response.payment().amount()).isEqualByComparingTo("39.80");
        assertThat(p.getQuantity()).isEqualTo(5);
        assertThat(cart.getItems()).hasSize(1);
        verify(orderRepository, never()).saveAndFlush(any());
        verify(cartRepository, never()).saveAndFlush(any());
    }

    @Test
    void carrinhoVazioDeveRetornarConflitoSemProcessarPagamento() {
        when(cartRepository.lockUserForCartMutation(USER_ID)).thenReturn(Optional.of(USER_ID));
        when(cartItemRepository.findStockIdsByUserId(USER_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.checkout(
                USER_ID, new CheckoutRequestDTO(PaymentMethod.PIX, null)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verifyNoInteractions(paymentSimulator);
    }

    @Test
    void estoqueInsuficienteNoCheckoutDeveRetornarConflitoSemProcessarPagamento() {
        Stock p = stock(STOCK_P, product(1L, "19.90", "ACTIVE", store), "P", 1);
        Cart cart = cart(line(p, 2, "19.90"));
        prepararCheckout(cart, p);

        assertThatThrownBy(() -> service.checkout(
                USER_ID, new CheckoutRequestDTO(PaymentMethod.PIX, null)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Estoque insuficiente");

        verifyNoInteractions(paymentSimulator);
        assertThat(p.getQuantity()).isEqualTo(1);
    }

    @Test
    void produtoInativoNoCheckoutDeveRetornarConflito() {
        Stock p = stock(STOCK_P, product(1L, "19.90", "INACTIVE", store), "P", 5);
        Cart cart = cart(line(p, 1, "19.90"));
        prepararCheckout(cart, p);

        assertThatThrownBy(() -> service.checkout(
                USER_ID, new CheckoutRequestDTO(PaymentMethod.PIX, null)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não está mais ativo");

        verifyNoInteractions(paymentSimulator);
    }

    @Test
    void lojistaDeveMoverPedidoParaPreparing() {
        Order order = Order.confirm(USER_ID, store,
                new Payment(USER_ID, new BigDecimal("10.00"), PaymentMethod.PIX, PaymentResult.APPROVED));
        when(orderRepository.findByIdAndStoreOwnerId(5L, OWNER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        OrderResponseDTO response = service.updateStatus(
                OWNER_ID, 5L, new UpdateOrderStatusRequestDTO(OrderStatus.PREPARING)
        );

        assertThat(response.status()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    void pedidoDeOutraLojaDeveRetornarNaoEncontrado() {
        when(orderRepository.findByIdAndStoreOwnerId(5L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(
                OWNER_ID, 5L, new UpdateOrderStatusRequestDTO(OrderStatus.PREPARING)
        )).isInstanceOf(OrderNotFoundException.class);
    }

    private void prepararCheckout(Cart cart, Stock... stocks) {
        List<Long> ids = cart.getItems().stream().map(i -> i.getStock().getId()).toList();
        when(cartRepository.lockUserForCartMutation(USER_ID)).thenReturn(Optional.of(USER_ID));
        when(cartItemRepository.findStockIdsByUserId(USER_ID)).thenReturn(ids);
        when(stockRepository.lockAllByIdForUpdate(anyCollection())).thenReturn(List.of(stocks));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
    }

    private void simularPagamento(PaymentResult result) {
        when(paymentSimulator.process(eq(USER_ID), any(), any(), any()))
                .thenAnswer(i -> new Payment(
                        USER_ID, i.getArgument(1), i.getArgument(2), result
                ));
    }

    private Cart cart(CartItem... items) {
        Cart cart = Cart.builder()
                .id(20L)
                .userId(USER_ID)
                .totalValueForecast(new BigDecimal("0.00"))
                .build();

        for (CartItem item : items) {
            item.setCart(cart);
            cart.getItems().add(item);
        }

        return cart;
    }

    private CartItem line(Stock stock, int quantity, String unitPrice) {
        return CartItem.builder()
                .stock(stock)
                .quantity(quantity)
                .unitPrice(new BigDecimal(unitPrice))
                .build();
    }

    private Product product(Long productId, String price, String status, Store store) {
        Product product = Product.builder()
                .name("Camiseta")
                .description("Camiseta de algodão")
                .price(new BigDecimal(price))
                .type("camiseta")
                .status(status)
                .photoUrls(List.of("https://example.com/camiseta.jpg"))
                .store(store)
                .build();

        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }

    private Stock stock(Long id, Product product, String size, int quantity) {
        Stock stock = new Stock(product, size, quantity);
        ReflectionTestUtils.setField(stock, "id", id);
        return stock;
    }
}
