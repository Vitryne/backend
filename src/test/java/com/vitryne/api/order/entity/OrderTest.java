package com.vitryne.api.order.entity;

import com.vitryne.api.order.enums.OrderStatus;
import com.vitryne.api.order.exception.InvalidOrderStatusTransitionException;
import com.vitryne.api.payment.entity.Payment;
import com.vitryne.api.payment.enums.PaymentMethod;
import com.vitryne.api.payment.enums.PaymentResult;
import com.vitryne.api.product.entity.Product;
import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.store.entity.Store;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private final Store store = Store.builder().id(1L).name("Loja").build();

    @Test
    void pedidoConfirmadoDeveSomarItensComSnapshotDoProduto() {
        Order order = Order.confirm(10L, store, payment(PaymentResult.APPROVED));
        Stock stock = new Stock(product(), "m", 5);

        OrderItem item = order.addItem(stock, 3, new BigDecimal("19.90"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("59.70");
        assertThat(item.getProductName()).isEqualTo("Camiseta");
        assertThat(item.getSize()).isEqualTo("M");
        assertThat(item.getSubtotal()).isEqualByComparingTo("59.70");
        assertThat(item.getOrder()).isSameAs(order);
    }

    @Test
    void naoDeveCriarPedidoComPagamentoRecusado() {
        assertThatThrownBy(() -> Order.confirm(10L, store, payment(PaymentResult.DECLINED)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void devePermitirApenasConfirmedParaPreparing() {
        Order order = Order.confirm(10L, store, payment(PaymentResult.APPROVED));

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.CONFIRMED))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);

        order.changeStatus(OrderStatus.PREPARING);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.PREPARING))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    private Payment payment(PaymentResult result) {
        return new Payment(10L, new BigDecimal("59.70"), PaymentMethod.PIX, result);
    }

    private Product product() {
        return Product.builder()
                .name("Camiseta")
                .description("Camiseta de algodão")
                .price(new BigDecimal("19.90"))
                .type("camiseta")
                .status("ACTIVE")
                .photoUrls(List.of("https://example.com/camiseta.jpg"))
                .store(store)
                .build();
    }
}
