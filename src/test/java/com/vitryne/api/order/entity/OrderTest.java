package com.vitryne.api.order.entity;

import com.vitryne.api.address.entity.Address;
import com.vitryne.api.order.enums.CanceledBy;
import com.vitryne.api.order.enums.ClientCancellationReason;
import com.vitryne.api.order.enums.OrderStatus;
import com.vitryne.api.order.enums.StoreRejectionReason;
import com.vitryne.api.order.exception.InvalidOrderStatusTransitionException;
import com.vitryne.api.payment.entity.Payment;
import com.vitryne.api.payment.enums.PaymentMethod;
import com.vitryne.api.payment.enums.PaymentResult;
import com.vitryne.api.product.entity.Product;
import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.store.entity.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final Instant DEADLINE = Instant.parse("2026-11-27T18:30:00Z");

    private final Store store = Store.builder().id(1L).name("Loja").build();

    @Test
    void pedidoConfirmadoDeveSomarItensComSnapshotDoProduto() {
        Order order = confirm(PaymentResult.APPROVED);
        Stock stock = new Stock(product(), "m", 5);

        OrderItem item = order.addItem(stock, 3, new BigDecimal("19.90"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_ACCEPTANCE);
        assertThat(order.getShippingAmount()).isEqualByComparingTo("0.00");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("59.70");
        assertThat(order.getDeliveryAddress().getCity()).isEqualTo("Maringá");
        assertThat(item.getProductName()).isEqualTo("Camiseta");
        assertThat(item.getSize()).isEqualTo("M");
        assertThat(item.getSubtotal()).isEqualByComparingTo("59.70");
        assertThat(item.getOrder()).isSameAs(order);
    }

    @Test
    void naoDeveCriarPedidoComPagamentoRecusado() {
        assertThatThrownBy(() -> confirm(PaymentResult.DECLINED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lojaAvancaUmaEtapaPorVezAteAEntrega() {
        Order order = confirm(PaymentResult.APPROVED);

        for (OrderStatus next : List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING,
                OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED)) {
            order.advanceTo(next);
            assertThat(order.getStatus()).isEqualTo(next);
        }

        assertThat(order.getCanceledBy()).isNull();
    }

    @Test
    void naoPulaNemRepeteNemVoltaEtapa() {
        Order order = confirm(PaymentResult.APPROVED);

        assertThatThrownBy(() -> order.advanceTo(OrderStatus.PREPARING))
                .isInstanceOf(InvalidOrderStatusTransitionException.class)
                .hasMessageContaining("a próxima etapa é CONFIRMED");
        assertThatThrownBy(() -> order.advanceTo(OrderStatus.AWAITING_ACCEPTANCE))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);

        order.advanceTo(OrderStatus.CONFIRMED);
        assertThatThrownBy(() -> order.advanceTo(OrderStatus.CONFIRMED))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThatThrownBy(() -> order.advanceTo(OrderStatus.AWAITING_ACCEPTANCE))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThatThrownBy(() -> order.advanceTo(OrderStatus.DELIVERED))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThatThrownBy(() -> order.advanceTo(null))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelarNaoEUmaEtapaDoAndamento() {
        Order order = confirm(PaymentResult.APPROVED);

        assertThatThrownBy(() -> order.advanceTo(OrderStatus.CANCELED))
                .isInstanceOf(InvalidOrderStatusTransitionException.class)
                .hasMessageContaining("use a recusa");

        order.advanceTo(OrderStatus.CONFIRMED);
        assertThatThrownBy(() -> order.advanceTo(OrderStatus.CANCELED))
                .isInstanceOf(InvalidOrderStatusTransitionException.class)
                .hasMessageContaining("já aceito");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void lojaRecusaPedidoAguardandoAceiteComJustificativa() {
        Order order = confirm(PaymentResult.APPROVED);

        order.rejectByStore(StoreRejectionReason.ITEM_UNAVAILABLE);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getCanceledBy()).isEqualTo(CanceledBy.STORE);
        assertThat(order.getCancellationReason()).isEqualTo("ITEM_UNAVAILABLE");
        assertThat(order.getCanceledAt()).isNotNull();
    }

    @Test
    void lojaNaoRecusaDepoisDoAceiteNemSemJustificativa() {
        Order semMotivo = confirm(PaymentResult.APPROVED);
        assertThatThrownBy(() -> semMotivo.rejectByStore(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(semMotivo.getStatus()).isEqualTo(OrderStatus.AWAITING_ACCEPTANCE);

        Order aceito = confirm(PaymentResult.APPROVED);
        aceito.advanceTo(OrderStatus.CONFIRMED);
        assertThatThrownBy(() -> aceito.rejectByStore(StoreRejectionReason.STORE_CLOSED))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(aceito.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(aceito.getCanceledBy()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"AWAITING_ACCEPTANCE", "CONFIRMED", "PREPARING"})
    void clienteCancelaAteOPreparo(OrderStatus status) {
        Order order = orderAt(status);

        order.cancelByClient(ClientCancellationReason.CHANGED_MIND);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getCanceledBy()).isEqualTo(CanceledBy.CLIENT);
        assertThat(order.getCancellationReason()).isEqualTo("CHANGED_MIND");
        assertThat(order.getCanceledAt()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"READY_FOR_PICKUP", "OUT_FOR_DELIVERY", "DELIVERED"})
    void clienteNaoCancelaAPartirDoProntoParaColeta(OrderStatus status) {
        Order order = orderAt(status);

        assertThatThrownBy(() -> order.cancelByClient(ClientCancellationReason.CHANGED_MIND))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(status);
        assertThat(order.getCanceledBy()).isNull();
    }

    @Test
    void entregueECanceladoSaoFinais() {
        Order entregue = orderAt(OrderStatus.DELIVERED);
        Order cancelado = confirm(PaymentResult.APPROVED);
        cancelado.cancelByClient(ClientCancellationReason.OTHER);

        for (Order order : List.of(entregue, cancelado)) {
            OrderStatus status = order.getStatus();
            for (OrderStatus next : OrderStatus.values()) {
                assertThatThrownBy(() -> order.advanceTo(next))
                        .isInstanceOf(InvalidOrderStatusTransitionException.class);
            }
            assertThatThrownBy(() -> order.rejectByStore(StoreRejectionReason.STORE_CLOSED))
                    .isInstanceOf(InvalidOrderStatusTransitionException.class);
            assertThatThrownBy(() -> order.cancelByClient(ClientCancellationReason.OTHER))
                    .isInstanceOf(InvalidOrderStatusTransitionException.class);
            assertThat(order.getStatus()).isEqualTo(status);
        }

        assertThat(cancelado.getCancellationReason()).isEqualTo("OTHER");
    }

    @Test
    void pedidoNasceComPrazoDeAceite() {
        assertThat(confirm(PaymentResult.APPROVED).getAcceptanceDeadline()).isEqualTo(DEADLINE);
        assertThatThrownBy(() -> Order.place(
                10L, store, payment(PaymentResult.APPROVED), address(), new BigDecimal("0.00"), null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sistemaCancelaPedidoVencidoSemAceite() {
        Order order = confirm(PaymentResult.APPROVED);

        order.expireAcceptance(DEADLINE);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getCanceledBy()).isEqualTo(CanceledBy.SYSTEM);
        assertThat(order.getCancellationReason()).isEqualTo("ACCEPTANCE_TIMEOUT");
        assertThat(order.getCanceledAt()).isNotNull();
    }

    @Test
    void naoExpiraPedidoNoPrazo() {
        Order order = confirm(PaymentResult.APPROVED);

        assertThatThrownBy(() -> order.expireAcceptance(DEADLINE.minusSeconds(1)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_ACCEPTANCE);
        assertThat(order.getCanceledBy()).isNull();
    }

    @Test
    void naoExpiraPedidoJaAceitoOuCancelado() {
        Order aceito = confirm(PaymentResult.APPROVED);
        aceito.advanceTo(OrderStatus.CONFIRMED);
        Order recusado = confirm(PaymentResult.APPROVED);
        recusado.rejectByStore(StoreRejectionReason.STORE_CLOSED);

        assertThatThrownBy(() -> aceito.expireAcceptance(DEADLINE.plusSeconds(60)))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThatThrownBy(() -> recusado.expireAcceptance(DEADLINE.plusSeconds(60)))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        assertThat(aceito.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(recusado.getCanceledBy()).isEqualTo(CanceledBy.STORE);
    }

    /** Pedido levado até o status pelo fluxo normal. */
    private Order orderAt(OrderStatus target) {
        Order order = confirm(PaymentResult.APPROVED);
        while (order.getStatus() != target) {
            order.advanceTo(order.getStatus().next());
        }
        return order;
    }

    @Test
    void freteDeveComporOTotal() {
        Order order = Order.place(10L, store, payment(PaymentResult.APPROVED), address(), new BigDecimal("5.00"), DEADLINE);
        order.addItem(new Stock(product(), "P", 5), 1, new BigDecimal("19.90"));

        assertThat(order.getTotalAmount()).isEqualByComparingTo("24.90");
    }

    @Test
    void naoDeveCriarPedidoSemEndereco() {
        assertThatThrownBy(() -> Order.place(
                10L, store, payment(PaymentResult.APPROVED), null, new BigDecimal("0.00"), DEADLINE
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private Order confirm(PaymentResult result) {
        return Order.place(10L, store, payment(result), address(), new BigDecimal("0.00"), DEADLINE);
    }

    private Address address() {
        return Address.builder()
                .zipCode("87000000").street("Rua").number("1").neighborhood("Centro")
                .city("Maringá").state("PR").build();
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
