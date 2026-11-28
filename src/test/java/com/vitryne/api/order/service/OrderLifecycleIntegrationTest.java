package com.vitryne.api.order.service;

import com.vitryne.api.order.dto.CancelOrderRequestDTO;
import com.vitryne.api.order.dto.CheckoutRequestDTO;
import com.vitryne.api.order.dto.OrderResponseDTO;
import com.vitryne.api.order.dto.RejectOrderRequestDTO;
import com.vitryne.api.order.dto.UpdateOrderStatusRequestDTO;
import com.vitryne.api.order.enums.ClientCancellationReason;
import com.vitryne.api.order.enums.OrderStatus;
import com.vitryne.api.order.enums.StoreRejectionReason;
import com.vitryne.api.order.exception.InvalidOrderStatusTransitionException;
import com.vitryne.api.payment.enums.PaymentMethod;
import com.vitryne.api.support.PostgresIntegrationTest;
import com.vitryne.api.support.PurchaseFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ciclo de vida do pedido contra Postgres real (ETI-409): locks do pedido e
 * do estoque e restrições do banco não aparecem nos testes com mocks.
 */
class OrderLifecycleIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbc;

    private PurchaseFixtures fx;
    private PurchaseFixtures.Shop shop;
    private long stockId;

    @BeforeEach
    void setUp() {
        fx = new PurchaseFixtures(jdbc);
        shop = fx.shop();
        stockId = fx.stock(fx.product(shop), "M", 5);
    }

    /** Recusa da loja e cancelamento do cliente ao mesmo tempo: só um vale e o estoque volta uma vez. */
    @RepeatedTest(5)
    void recusaECancelamentoSimultaneosDevolvemOEstoqueUmaVez() throws Exception {
        long client = fx.consumer();
        long order = placeOrder(client, 2);
        assertThat(fx.stockQuantity(stockId)).isEqualTo(3);

        List<Outcome> outcomes = runConcurrently(
                () -> orderService.rejectByStore(shop.ownerId(), order,
                        new RejectOrderRequestDTO(StoreRejectionReason.ITEM_UNAVAILABLE)),
                () -> orderService.cancelByClient(client, order,
                        new CancelOrderRequestDTO(ClientCancellationReason.CHANGED_MIND))
        );

        assertThat(outcomes).filteredOn(Outcome::succeeded).hasSize(1);
        assertThat(outcomes).filteredOn(o -> !o.succeeded())
                .singleElement()
                .satisfies(o -> assertThat(o.error()).isInstanceOf(InvalidOrderStatusTransitionException.class));
        assertThat(fx.stockQuantity(stockId)).isEqualTo(5);
        assertThat(status(order)).isEqualTo("CANCELED");
    }

    /** Aceite e recusa ao mesmo tempo: o pedido termina aceito ou recusado, nunca os dois. */
    @RepeatedTest(5)
    void aceiteERecusaSimultaneosTemUmUnicoResultado() throws Exception {
        long client = fx.consumer();
        long order = placeOrder(client, 1);

        List<Outcome> outcomes = runConcurrently(
                () -> orderService.updateStatus(shop.ownerId(), order,
                        new UpdateOrderStatusRequestDTO(OrderStatus.CONFIRMED)),
                () -> orderService.rejectByStore(shop.ownerId(), order,
                        new RejectOrderRequestDTO(StoreRejectionReason.STORE_CLOSED))
        );

        assertThat(outcomes).filteredOn(Outcome::succeeded).hasSize(1);
        OrderStatus winner = outcomes.stream().filter(Outcome::succeeded).findFirst().orElseThrow()
                .response().status();
        assertThat(status(order)).isEqualTo(winner.name());
        assertThat(fx.stockQuantity(stockId)).isEqualTo(winner == OrderStatus.CANCELED ? 5 : 4);
    }

    @Test
    void cancelamentoDevolveEstoqueQueOutroClientePodeComprar() {
        long ana = fx.consumer();
        long order = placeOrder(ana, 5);
        assertThat(fx.stockQuantity(stockId)).isZero();

        orderService.cancelByClient(ana, order, new CancelOrderRequestDTO(ClientCancellationReason.OTHER));

        assertThat(fx.stockQuantity(stockId)).isEqualTo(5);
        long bruno = fx.consumer();
        placeOrder(bruno, 5);
        assertThat(fx.stockQuantity(stockId)).isZero();
    }

    @Test
    void cancelamentoGravaQuemMotivoEData() {
        long client = fx.consumer();
        long order = placeOrder(client, 1);

        orderService.rejectByStore(shop.ownerId(), order,
                new RejectOrderRequestDTO(StoreRejectionReason.OUT_OF_DELIVERY_AREA));

        assertThat(jdbc.queryForMap(
                "SELECT status, canceled_by, cancellation_reason, canceled_at FROM orders WHERE id = ?", order))
                .containsEntry("status", "CANCELED")
                .containsEntry("canceled_by", "STORE")
                .containsEntry("cancellation_reason", "OUT_OF_DELIVERY_AREA")
                .extractingByKey("canceled_at").isNotNull();
    }

    @Test
    void bancoExigeDadosDoCancelamentoSomenteEmPedidoCancelado() {
        long order = placeOrder(fx.consumer(), 1);

        assertThatThrownBy(() -> jdbc.update("UPDATE orders SET status = 'CANCELED' WHERE id = ?", order))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE orders SET canceled_by = 'STORE', cancellation_reason = 'X', canceled_at = now() WHERE id = ?",
                order))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE orders SET status = 'SHIPPED' WHERE id = ?", order))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(status(order)).isEqualTo("AWAITING_ACCEPTANCE");
    }

    @Test
    void pedidoAntigoConfirmadoSegueOFluxo() {
        long order = placeOrder(fx.consumer(), 1);
        // Pedidos criados antes da ETI-409 nasceram CONFIRMED.
        jdbc.update("UPDATE orders SET status = 'CONFIRMED' WHERE id = ?", order);

        OrderResponseDTO response = orderService.updateStatus(shop.ownerId(), order,
                new UpdateOrderStatusRequestDTO(OrderStatus.PREPARING));

        assertThat(response.status()).isEqualTo(OrderStatus.PREPARING);
    }

    // ---------- apoio ----------

    private long placeOrder(long userId, int quantity) {
        fx.cartItem(userId, stockId, quantity);
        String token = orderService.review(userId).reviewToken();
        return orderService.checkout(userId, UUID.randomUUID().toString(),
                new CheckoutRequestDTO(token, PaymentMethod.PIX, null)).order().id();
    }

    private String status(long orderId) {
        return jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId);
    }

    private record Outcome(OrderResponseDTO response, Throwable error) {
        boolean succeeded() {
            return error == null;
        }
    }

    @SafeVarargs
    private List<Outcome> runConcurrently(Callable<OrderResponseDTO>... calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.length);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<OrderResponseDTO>> futures = new ArrayList<>();
            for (Callable<OrderResponseDTO> call : calls) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return call.call();
                }));
            }
            start.countDown();

            List<Outcome> outcomes = new ArrayList<>();
            for (Future<OrderResponseDTO> future : futures) {
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
