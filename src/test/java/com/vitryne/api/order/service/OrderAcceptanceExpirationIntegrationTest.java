package com.vitryne.api.order.service;

import com.vitryne.api.order.dto.CheckoutRequestDTO;
import com.vitryne.api.order.dto.OrderResponseDTO;
import com.vitryne.api.order.dto.UpdateOrderStatusRequestDTO;
import com.vitryne.api.order.enums.OrderStatus;
import com.vitryne.api.order.exception.InvalidOrderStatusTransitionException;
import com.vitryne.api.payment.enums.PaymentMethod;
import com.vitryne.api.support.PostgresIntegrationTest;
import com.vitryne.api.support.PurchaseFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Cancelamento automático do pedido não aceito no prazo, contra Postgres real (ETI-412). */
class OrderAcceptanceExpirationIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderAcceptanceExpirationJob job;

    @Autowired
    private JdbcTemplate jdbc;

    private PurchaseFixtures fx;
    private PurchaseFixtures.Shop shop;
    private long stockId;

    @BeforeEach
    void setUp() {
        fx = new PurchaseFixtures(jdbc);
        shop = fx.shop();
        stockId = fx.stock(fx.product(shop), "M", 10);
        // Começa sem pedidos vencidos de testes anteriores.
        job.expireOverdueOrders();
    }

    @Test
    void pedidoNasceComPrazoDeTrintaMinutosNaResposta() {
        OrderResponseDTO order = placeOrder(fx.consumer(), 1);

        assertThat(order.status()).isEqualTo(OrderStatus.AWAITING_ACCEPTANCE);
        assertThat(order.acceptanceDeadline())
                .isCloseTo(order.createdAt().plus(Duration.ofMinutes(30)), within(Duration.ofSeconds(5)));

        OrderResponseDTO accepted = orderService.updateStatus(shop.ownerId(), order.id(),
                new UpdateOrderStatusRequestDTO(OrderStatus.CONFIRMED));
        assertThat(accepted.acceptanceDeadline()).isNull();
    }

    @Test
    void pedidoVencidoECanceladoComMotivoEDevolveOEstoque() {
        long order = placeOrder(fx.consumer(), 3).id();
        assertThat(fx.stockQuantity(stockId)).isEqualTo(7);
        expireDeadline(order);

        assertThat(job.expireOverdueOrders()).isEqualTo(1);

        assertThat(jdbc.queryForMap(
                "SELECT status, canceled_by, cancellation_reason, canceled_at FROM orders WHERE id = ?", order))
                .containsEntry("status", "CANCELED")
                .containsEntry("canceled_by", "SYSTEM")
                .containsEntry("cancellation_reason", "ACCEPTANCE_TIMEOUT")
                .extractingByKey("canceled_at").isNotNull();
        assertThat(fx.stockQuantity(stockId)).isEqualTo(10);
    }

    @Test
    void pedidoNoPrazoEPedidoAceitoSaoMantidos() {
        long noPrazo = placeOrder(fx.consumer(), 1).id();
        long aceito = placeOrder(fx.consumer(), 1).id();
        orderService.updateStatus(shop.ownerId(), aceito, new UpdateOrderStatusRequestDTO(OrderStatus.CONFIRMED));
        // Prazo do pedido aceito já passou, mas ele saiu de AWAITING_ACCEPTANCE.
        expireDeadline(aceito);

        assertThat(job.expireOverdueOrders()).isZero();

        assertThat(status(noPrazo)).isEqualTo("AWAITING_ACCEPTANCE");
        assertThat(status(aceito)).isEqualTo("CONFIRMED");
        assertThat(fx.stockQuantity(stockId)).isEqualTo(8);
    }

    /** Aceite e expiração ao mesmo tempo: o pedido termina aceito ou cancelado, nunca os dois. */
    @RepeatedTest(5)
    void aceiteNoInstanteDaExpiracaoTemUmUnicoResultado() throws Exception {
        long order = placeOrder(fx.consumer(), 1).id();
        expireDeadline(order);

        List<Outcome> outcomes = runConcurrently(
                () -> orderService.updateStatus(shop.ownerId(), order,
                        new UpdateOrderStatusRequestDTO(OrderStatus.CONFIRMED)),
                () -> orderService.expireNextOverdueAcceptance().orElse(null)
        );

        Outcome accept = outcomes.get(0);
        Outcome expire = outcomes.get(1);
        assertThat(expire.error()).isNull();

        if (status(order).equals("CONFIRMED")) {
            assertThat(accept.error()).isNull();
            assertThat(expire.value()).isNull();
            assertThat(fx.stockQuantity(stockId)).isEqualTo(9);
        } else {
            assertThat(status(order)).isEqualTo("CANCELED");
            assertThat(accept.error()).isInstanceOf(InvalidOrderStatusTransitionException.class);
            assertThat(expire.value()).isEqualTo(order);
            assertThat(fx.stockQuantity(stockId)).isEqualTo(10);
        }
    }

    /** Várias instâncias rodando a rotina juntas: cada pedido vencido é cancelado uma única vez. */
    @RepeatedTest(3)
    void variasInstanciasCancelamCadaPedidoUmaVez() throws Exception {
        int orders = 6;
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < orders; i++) {
            long order = placeOrder(fx.consumer(), 1).id();
            expireDeadline(order);
            ids.add(order);
        }
        assertThat(fx.stockQuantity(stockId)).isEqualTo(4);

        List<Outcome> outcomes = runConcurrently(
                job::expireOverdueOrders, job::expireOverdueOrders, job::expireOverdueOrders
        );

        assertThat(outcomes).allSatisfy(o -> assertThat(o.error()).isNull());
        assertThat(outcomes.stream().mapToInt(o -> (Integer) o.value()).sum()).isEqualTo(orders);
        assertThat(ids).allSatisfy(id -> assertThat(status(id)).isEqualTo("CANCELED"));
        assertThat(fx.stockQuantity(stockId)).isEqualTo(10);
    }

    @Test
    void bancoExigePrazoEmPedidoAguardandoAceite() {
        long order = placeOrder(fx.consumer(), 1).id();

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        jdbc.update("UPDATE orders SET acceptance_deadline = NULL WHERE id = ?", order))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    // ---------- apoio ----------

    private OrderResponseDTO placeOrder(long userId, int quantity) {
        fx.cartItem(userId, stockId, quantity);
        String token = orderService.review(userId).reviewToken();
        return orderService.checkout(userId, UUID.randomUUID().toString(),
                new CheckoutRequestDTO(token, PaymentMethod.PIX, null)).order();
    }

    /** Leva o prazo de aceite para o passado. */
    private void expireDeadline(long orderId) {
        jdbc.update("UPDATE orders SET acceptance_deadline = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.now().minus(Duration.ofMinutes(1))), orderId);
    }

    private String status(long orderId) {
        return jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId);
    }

    private record Outcome(Object value, Throwable error) {
    }

    private List<Outcome> runConcurrently(Callable<?>... calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.length);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Callable<?> call : calls) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return call.call();
                }));
            }
            start.countDown();

            List<Outcome> outcomes = new ArrayList<>();
            for (Future<?> future : futures) {
                try {
                    outcomes.add(new Outcome(future.get(30, TimeUnit.SECONDS), null));
                } catch (ExecutionException e) {
                    outcomes.add(new Outcome(null, e.getCause()));
                }
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }
}
