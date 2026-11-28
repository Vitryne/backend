package com.vitryne.api.order.service;

import com.vitryne.api.order.dto.CheckoutRequestDTO;
import com.vitryne.api.order.dto.CheckoutResponseDTO;
import com.vitryne.api.payment.enums.PaymentMethod;
import com.vitryne.api.support.PostgresIntegrationTest;
import com.vitryne.api.support.PurchaseFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checkout contra Postgres real: locks, transação e restrições do banco
 * não aparecem nos testes com mocks.
 */
class CheckoutConcurrencyIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbc;

    private PurchaseFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new PurchaseFixtures(jdbc);
    }

    @Test
    void doisClientesComprandoAUltimaUnidadeGeramUmUnicoPedidoSemEstoqueNegativo() throws Exception {
        long stockId = stockWithQuantity(1);
        long alice = consumerWithCart(stockId, 1);
        long bob = consumerWithCart(stockId, 1);
        CheckoutRequestDTO aliceRequest = request(alice);
        CheckoutRequestDTO bobRequest = request(bob);

        List<Outcome> outcomes = runConcurrently(
                () -> orderService.checkout(alice, newKey(), aliceRequest),
                () -> orderService.checkout(bob, newKey(), bobRequest)
        );

        assertThat(outcomes).filteredOn(Outcome::succeeded).hasSize(1);
        Outcome loser = outcomes.stream().filter(o -> !o.succeeded()).findFirst().orElseThrow();
        assertThat(loser.error()).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) loser.error()).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        assertThat(stockQuantity(stockId)).isZero();
        assertThat(count("SELECT count(*) FROM order_item WHERE stock_id = ?", stockId)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM payment WHERE user_id IN (?, ?)", alice, bob)).isEqualTo(1);

        long winner = outcomes.get(0).succeeded() ? alice : bob;
        long other = winner == alice ? bob : alice;
        assertThat(cartItems(winner)).isZero();
        assertThat(cartItems(other)).as("carrinho de quem perdeu é preservado").isEqualTo(1);
    }

    @Test
    void cliqueDuploComAMesmaChaveGeraUmPedidoEUmaBaixa() throws Exception {
        long stockId = stockWithQuantity(5);
        long user = consumerWithCart(stockId, 2);
        CheckoutRequestDTO request = request(user);
        String key = newKey();

        List<Outcome> outcomes = runConcurrently(
                () -> orderService.checkout(user, key, request),
                () -> orderService.checkout(user, key, request)
        );

        assertThat(outcomes).allMatch(Outcome::succeeded);
        assertThat(outcomes).extracting(o -> o.response().replayed()).containsExactlyInAnyOrder(false, true);
        assertThat(outcomes).extracting(o -> o.response().order().id()).containsOnly(
                outcomes.get(0).response().order().id()
        );

        assertThat(stockQuantity(stockId)).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM orders WHERE user_id = ?", user)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM payment WHERE user_id = ?", user)).isEqualTo(1);

        CheckoutResponseDTO later = orderService.checkout(user, key, request);
        assertThat(later.replayed()).isTrue();
        assertThat(later.order().id()).isEqualTo(outcomes.get(0).response().order().id());
        assertThat(stockQuantity(stockId)).isEqualTo(3);
    }

    @Test
    void mesmaChaveEmClientesDiferentesNaoCompartilhaResultado() {
        long stockId = stockWithQuantity(5);
        long alice = consumerWithCart(stockId, 1);
        long bob = consumerWithCart(stockId, 1);
        String sharedKey = newKey();

        CheckoutResponseDTO aliceResponse = orderService.checkout(alice, sharedKey, request(alice));
        CheckoutResponseDTO bobResponse = orderService.checkout(bob, sharedKey, request(bob));

        assertThat(bobResponse.replayed()).isFalse();
        assertThat(bobResponse.order().userId()).isEqualTo(bob);
        assertThat(bobResponse.order().id()).isNotEqualTo(aliceResponse.order().id());
        assertThat(stockQuantity(stockId)).isEqualTo(3);
    }

    @Test
    void chaveReutilizadaComOutrosDadosNaoGeraNovaCompra() {
        long stockId = stockWithQuantity(5);
        long user = consumerWithCart(stockId, 1);
        String key = newKey();
        CheckoutRequestDTO first = request(user);
        orderService.checkout(user, key, first);

        CheckoutRequestDTO different = new CheckoutRequestDTO(
                first.reviewToken(), PaymentMethod.CREDIT_CARD, null
        );

        assertThatThrownBy(() -> orderService.checkout(user, key, different))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(count("SELECT count(*) FROM payment WHERE user_id = ?", user)).isEqualTo(1);
        assertThat(stockQuantity(stockId)).isEqualTo(4);
    }

    @Test
    void bancoRecusaEstoqueNegativo() {
        long stockId = stockWithQuantity(1);

        assertThatThrownBy(() -> jdbc.update("UPDATE stock SET quantity = quantity - 2 WHERE id = ?", stockId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(stockQuantity(stockId)).isEqualTo(1);
    }

    // ---------- apoio ----------

    private record Outcome(CheckoutResponseDTO response, Throwable error) {
        boolean succeeded() {
            return error == null;
        }
    }

    @SafeVarargs
    private List<Outcome> runConcurrently(Callable<CheckoutResponseDTO>... calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.length);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<CheckoutResponseDTO>> futures = new ArrayList<>();
            for (Callable<CheckoutResponseDTO> call : calls) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return call.call();
                }));
            }
            start.countDown();

            List<Outcome> outcomes = new ArrayList<>();
            for (Future<CheckoutResponseDTO> future : futures) {
                try {
                    outcomes.add(new Outcome(future.get(30, TimeUnit.SECONDS), null));
                } catch (java.util.concurrent.ExecutionException e) {
                    outcomes.add(new Outcome(null, e.getCause()));
                }
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    private CheckoutRequestDTO request(long userId) {
        String token = orderService.review(userId).reviewToken();
        assertThat(token).as("revisão deve permitir confirmar").isNotNull();
        return new CheckoutRequestDTO(token, PaymentMethod.PIX, null);
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }

    private long stockWithQuantity(int quantity) {
        return fixtures.stock(quantity);
    }

    private long consumerWithCart(long stockId, int quantity) {
        return fixtures.consumerWithCart(stockId, quantity);
    }

    private int stockQuantity(long stockId) {
        return fixtures.stockQuantity(stockId);
    }

    private int cartItems(long userId) {
        return fixtures.cartItems(userId);
    }

    private int count(String sql, Object... args) {
        return fixtures.count(sql, args);
    }
}
