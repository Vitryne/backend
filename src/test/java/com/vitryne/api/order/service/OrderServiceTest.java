package com.vitryne.api.order.service;

import com.vitryne.api.address.entity.Address;
import com.vitryne.api.cart.entity.Cart;
import com.vitryne.api.cart.entity.CartItem;
import com.vitryne.api.cart.repository.CartItemRepository;
import com.vitryne.api.cart.repository.CartRepository;
import com.vitryne.api.client.entity.Client;
import com.vitryne.api.client.repository.ClientRepository;
import com.vitryne.api.order.dto.CheckoutRequestDTO;
import com.vitryne.api.order.dto.CheckoutResponseDTO;
import com.vitryne.api.order.dto.CheckoutReviewResponseDTO;
import com.vitryne.api.order.dto.OrderResponseDTO;
import com.vitryne.api.order.dto.UpdateOrderStatusRequestDTO;
import com.vitryne.api.order.entity.Order;
import com.vitryne.api.order.enums.OrderStatus;
import com.vitryne.api.order.enums.ReviewIssueType;
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
import org.mockito.Spy;
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

    @Mock
    private ClientRepository clientRepository;

    @Spy
    private CheckoutEvaluator checkoutEvaluator = new CheckoutEvaluator();

    @InjectMocks
    private OrderService service;

    private final Store store = Store.builder().id(1L).name("Loja").build();

    // ---------- revisão ----------

    @Test
    void revisaoDeveRetornarValoresSemAlterarCarrinhoNemEstoque() {
        Product product = product(1L, "19.90", "ACTIVE");
        Stock p = stock(STOCK_P, product, "P", 5);
        Cart cart = cart(line(p, 2, "25.00"));
        carrinhoEEndereco(cart);

        CheckoutReviewResponseDTO review = service.review(USER_ID);

        assertThat(review.canConfirm()).isTrue();
        assertThat(review.reviewToken()).isNotBlank();
        assertThat(review.storeId()).isEqualTo(1L);
        assertThat(review.deliveryAddress().city()).isEqualTo("Maringá");
        assertThat(review.itemsSubtotal()).isEqualByComparingTo("39.80");
        assertThat(review.shippingAmount()).isEqualByComparingTo("0.00");
        assertThat(review.totalAmount()).isEqualByComparingTo("39.80");
        assertThat(review.items().getFirst().cartUnitPrice()).isEqualByComparingTo("25.00");
        assertThat(review.items().getFirst().currentUnitPrice()).isEqualByComparingTo("19.90");
        assertThat(review.items().getFirst().availableQuantity()).isEqualTo(5);
        assertThat(review.issues()).singleElement()
                .satisfies(issue -> {
                    assertThat(issue.type()).isEqualTo(ReviewIssueType.PRICE_CHANGED);
                    assertThat(issue.blocking()).isFalse();
                });

        assertThat(p.getQuantity()).isEqualTo(5);
        assertThat(cart.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("25.00");
        verify(cartRepository, never()).saveAndFlush(any());
        verifyNoInteractions(orderRepository, paymentSimulator, stockRepository);
    }

    @Test
    void revisaoComCarrinhoVazioDeveRetornarConflito() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.review(USER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void revisaoSemClienteCadastradoDeveBloquearPorEndereco() {
        Cart cart = cart(line(stock(STOCK_P, product(1L, "19.90", "ACTIVE"), "P", 5), 1, "19.90"));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(clientRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        CheckoutReviewResponseDTO review = service.review(USER_ID);

        assertThat(review.canConfirm()).isFalse();
        assertThat(review.reviewToken()).isNull();
        assertThat(review.deliveryAddress()).isNull();
        assertThat(review.issues()).extracting(i -> i.type())
                .containsExactly(ReviewIssueType.INVALID_DELIVERY_ADDRESS);
    }

    // ---------- checkout ----------

    @Test
    void pagamentoAprovadoDeveCriarPedidoComValoresDaRevisao() {
        Product product = product(1L, "19.90", "ACTIVE");
        Stock p = stock(STOCK_P, product, "P", 5);
        Stock m = stock(STOCK_M, product, "M", 1);
        Cart cart = cart(line(p, 2, "15.00"), line(m, 1, "15.00"));
        prepararCheckout(cart);
        String token = service.review(USER_ID).reviewToken();

        simularPagamento(PaymentResult.APPROVED);
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        CheckoutResponseDTO response = service.checkout(
                USER_ID, new CheckoutRequestDTO(token, PaymentMethod.CREDIT_CARD, null)
        );

        verify(paymentSimulator).process(
                eq(USER_ID), argThat(v -> v.compareTo(new BigDecimal("59.70")) == 0),
                eq(PaymentMethod.CREDIT_CARD), isNull()
        );

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).saveAndFlush(captor.capture());
        Order order = captor.getValue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getStore()).isSameAs(store);
        assertThat(order.getShippingAmount()).isEqualByComparingTo("0.00");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("59.70");
        assertThat(order.getDeliveryAddress().getZipCode()).isEqualTo("87000000");
        assertThat(order.getItems()).hasSize(2);

        assertThat(p.getQuantity()).isEqualTo(3);
        assertThat(m.getQuantity()).isZero();

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).saveAndFlush(cart);

        assertThat(response.payment().result()).isEqualTo(PaymentResult.APPROVED);
        assertThat(response.order().shippingAmount()).isEqualByComparingTo("0.00");
        assertThat(response.order().itemsSubtotal()).isEqualByComparingTo("59.70");
        assertThat(response.order().deliveryAddress().state()).isEqualTo("PR");
    }

    @Test
    void precoAlteradoDepoisDaRevisaoDeveRecusarSemCobrar() {
        Product product = product(1L, "19.90", "ACTIVE");
        Stock p = stock(STOCK_P, product, "P", 5);
        Cart cart = cart(line(p, 2, "19.90"));
        prepararCheckout(cart);
        String token = service.review(USER_ID).reviewToken();

        ReflectionTestUtils.setField(product, "price", new BigDecimal("29.90"));

        assertThatThrownBy(() -> service.checkout(
                USER_ID, new CheckoutRequestDTO(token, PaymentMethod.PIX, null)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Revise novamente");

        verifyNoInteractions(paymentSimulator, orderRepository);
        assertThat(p.getQuantity()).isEqualTo(5);
    }

    @Test
    void pagamentoRecusadoDeveRegistrarPagamentoSemCriarPedidoNemAlterarCarrinhoOuEstoque() {
        Stock p = stock(STOCK_P, product(1L, "19.90", "ACTIVE"), "P", 5);
        Cart cart = cart(line(p, 2, "19.90"));
        prepararCheckout(cart);
        String token = service.review(USER_ID).reviewToken();
        simularPagamento(PaymentResult.DECLINED);

        CheckoutResponseDTO response = service.checkout(
                USER_ID, new CheckoutRequestDTO(token, PaymentMethod.PIX, PaymentResult.DECLINED)
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
                USER_ID, new CheckoutRequestDTO("token", PaymentMethod.PIX, null)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verifyNoInteractions(paymentSimulator);
    }

    @Test
    void estoqueInsuficienteNoCheckoutDeveRetornarConflitoSemProcessarPagamento() {
        Stock p = stock(STOCK_P, product(1L, "19.90", "ACTIVE"), "P", 1);
        Cart cart = cart(line(p, 2, "19.90"));
        prepararCheckout(cart);

        assertThatThrownBy(() -> service.checkout(
                USER_ID, new CheckoutRequestDTO("token", PaymentMethod.PIX, null)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Estoque insuficiente");

        verifyNoInteractions(paymentSimulator);
        assertThat(p.getQuantity()).isEqualTo(1);
    }

    @Test
    void produtoInativoNoCheckoutDeveRetornarConflito() {
        Stock p = stock(STOCK_P, product(1L, "19.90", "INACTIVE"), "P", 5);
        Cart cart = cart(line(p, 1, "19.90"));
        prepararCheckout(cart);

        assertThatThrownBy(() -> service.checkout(
                USER_ID, new CheckoutRequestDTO("token", PaymentMethod.PIX, null)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não está mais disponível");

        verifyNoInteractions(paymentSimulator);
    }

    // ---------- status ----------

    @Test
    void lojistaDeveMoverPedidoParaPreparing() {
        Order order = Order.confirm(USER_ID, store,
                new Payment(USER_ID, new BigDecimal("10.00"), PaymentMethod.PIX, PaymentResult.APPROVED),
                CheckoutEvaluatorTest.address(), new BigDecimal("0.00"));
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

    // ---------- apoio ----------

    private void carrinhoEEndereco(Cart cart) {
        lenient().when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        Client client = new Client(null, "Cliente", "11111111111", CheckoutEvaluatorTest.address());
        lenient().when(clientRepository.findByUserId(USER_ID)).thenReturn(Optional.of(client));
    }

    private void prepararCheckout(Cart cart) {
        carrinhoEEndereco(cart);
        List<Long> ids = cart.getItems().stream().map(i -> i.getStock().getId()).toList();
        when(cartRepository.lockUserForCartMutation(USER_ID)).thenReturn(Optional.of(USER_ID));
        when(cartItemRepository.findStockIdsByUserId(USER_ID)).thenReturn(ids);
        when(stockRepository.lockAllByIdForUpdate(anyCollection())).thenReturn(List.of());
    }

    private void simularPagamento(PaymentResult result) {
        when(paymentSimulator.process(eq(USER_ID), any(), any(), any()))
                .thenAnswer(i -> new Payment(USER_ID, i.getArgument(1), i.getArgument(2), result));
    }

    private Cart cart(CartItem... items) {
        Cart cart = Cart.builder()
                .id(20L)
                .userId(USER_ID)
                .totalValueForecast(new BigDecimal("0.00"))
                .build();

        long itemId = 30L;
        for (CartItem item : items) {
            item.setId(itemId++);
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

    private Product product(Long productId, String price, String status) {
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
