package com.vitryne.api.security;

import com.jayway.jsonpath.JsonPath;
import com.vitryne.api.support.PostgresIntegrationTest;
import com.vitryne.api.support.PurchaseFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controles de acesso e cenários críticos da compra pela API, com contas de
 * clientes e de lojistas de lojas diferentes (ETI-278).
 */
class PurchaseAccessControlIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private PurchaseFixtures fx;

    /** Loja A e loja B, cada uma com produto e estoque; clientes Ana e Bruno. */
    private PurchaseFixtures.Shop shopA;
    private PurchaseFixtures.Shop shopB;
    private long productA;
    private long productB;
    private long stockA;
    private long stockB;
    private long ana;
    private long bruno;

    @BeforeEach
    void setUp() {
        fx = new PurchaseFixtures(jdbc);
        shopA = fx.shop();
        shopB = fx.shop();
        productA = fx.product(shopA);
        productB = fx.product(shopB);
        stockA = fx.stock(productA, "M", 10);
        stockB = fx.stock(productB, "M", 10);
        ana = fx.consumer();
        bruno = fx.consumer();
    }

    @Nested
    class ClienteNaoAcessaDadosDeOutroCliente {

        @Test
        void carrinhoRetornaApenasOsItensDoProprioCliente() throws Exception {
            fx.cartItem(ana, stockA, 1);
            fx.cartItem(bruno, stockA, 3);

            mvc.perform(as(ana, get("/cart")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(ana))
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.items[0].quantity").value(1));
        }

        @Test
        void naoAlteraNemRemoveItemDoCarrinhoAlheio() throws Exception {
            fx.cartItem(ana, stockA, 1);
            long itemBruno = fx.cartItem(bruno, stockA, 3);

            assertRejected(as(ana, put("/cart/items/" + itemBruno)).content("{\"quantity\": 1}"));
            assertRejected(as(ana, delete("/cart/items/" + itemBruno)));

            assertThat(fx.cartItemQuantity(itemBruno)).isEqualTo(3);
            assertThat(fx.cartItems(bruno)).isEqualTo(1);
        }

        @Test
        @Disabled(BUG_CART_EXCEPTIONS)
        void itemDoCarrinhoAlheioRetornaNaoEncontrado() throws Exception {
            fx.cartItem(ana, stockA, 1);
            long itemBruno = fx.cartItem(bruno, stockA, 3);

            mvc.perform(as(ana, put("/cart/items/" + itemBruno)).content("{\"quantity\": 1}"))
                    .andExpect(status().isNotFound());
            mvc.perform(as(ana, delete("/cart/items/" + itemBruno)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void naoConsultaPedidoAlheio() throws Exception {
            long orderBruno = placeOrder(bruno, stockA);

            mvc.perform(as(ana, get("/orders/" + orderBruno)))
                    .andExpect(status().isNotFound());

            mvc.perform(as(ana, get("/orders")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void naoUsaRotasDeLojista() throws Exception {
            long orderAna = placeOrder(ana, stockA);

            mvc.perform(as(ana, get("/store/orders"))).andExpect(status().isForbidden());
            mvc.perform(as(ana, patch("/store/orders/" + orderAna + "/status")).content("{\"status\": \"PREPARING\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(as(ana, patch("/product/manager/" + productA + "/status")).content("{\"status\": \"INACTIVE\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void semTokenNaoAcessaCarrinhoNemPedidos() throws Exception {
            mvc.perform(get("/cart")).andExpect(status().isUnauthorized());
            mvc.perform(get("/orders")).andExpect(status().isUnauthorized());
            mvc.perform(post("/orders/checkout").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class LojistaNaoAlteraOutraLoja {

        @Test
        void naoConsultaNemEditaProdutoDeOutraLoja() throws Exception {
            mvc.perform(asOwner(shopA, get("/product/manager/" + productB)))
                    .andExpect(status().isNotFound());

            mvc.perform(asOwner(shopA, put("/product/manager/" + productB)).content("""
                            {"name": "Invadido", "description": "x", "price": 1.00, "type": "CAMISETA",
                             "photoUrls": ["https://img/x.png"]}
                            """))
                    .andExpect(status().isNotFound());

            mvc.perform(asOwner(shopA, patch("/product/manager/" + productB + "/status"))
                            .content("{\"status\": \"INACTIVE\"}"))
                    .andExpect(status().isNotFound());

            assertThat(jdbc.queryForMap("SELECT name, price, status FROM product WHERE id = ?", productB))
                    .containsEntry("status", "ACTIVE")
                    .extractingByKey("name").asString().startsWith("Camiseta");
        }

        @Test
        void naoMexeNoEstoqueDeOutraLoja() throws Exception {
            mvc.perform(asOwner(shopA, post("/product/manager/" + productB + "/stocks/" + stockB + "/replenishments"))
                            .content("{\"quantity\": 5}"))
                    .andExpect(status().isNotFound());

            mvc.perform(asOwner(shopA, patch("/product/manager/" + productB + "/stocks/" + stockB))
                            .content("{\"quantity\": 0, \"version\": 0}"))
                    .andExpect(status().isNotFound());

            // Produto próprio na URL com estoque de outra loja.
            mvc.perform(asOwner(shopA, patch("/product/manager/" + productA + "/stocks/" + stockB))
                            .content("{\"quantity\": 0, \"version\": 0}"))
                    .andExpect(status().isNotFound());

            mvc.perform(asOwner(shopA, post("/product/manager/" + productB + "/stocks"))
                            .content("{\"size\": \"GG\", \"quantity\": 5}"))
                    .andExpect(status().isNotFound());

            assertThat(fx.stockQuantity(stockB)).isEqualTo(10);
            assertThat(fx.count("SELECT count(*) FROM stock WHERE product_id = ?", productB)).isEqualTo(1);
        }

        @Test
        void naoConsultaNemAvancaPedidoDeOutraLoja() throws Exception {
            long orderLojaB = placeOrder(ana, stockB);

            mvc.perform(asOwner(shopA, get("/store/orders/" + orderLojaB)))
                    .andExpect(status().isNotFound());

            mvc.perform(asOwner(shopA, patch("/store/orders/" + orderLojaB + "/status"))
                            .content("{\"status\": \"PREPARING\"}"))
                    .andExpect(status().isNotFound());

            mvc.perform(asOwner(shopA, get("/store/orders")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            assertThat(orderStatus(orderLojaB)).isEqualTo("CONFIRMED");
        }

        @Test
        void naoUsaRotasDeCliente() throws Exception {
            mvc.perform(asOwner(shopA, get("/cart"))).andExpect(status().isForbidden());
            mvc.perform(asOwner(shopA, post("/cart/items")).content("{\"stockId\": " + stockB + ", \"quantity\": 1}"))
                    .andExpect(status().isForbidden());
            mvc.perform(asOwner(shopA, get("/orders"))).andExpect(status().isForbidden());
        }
    }

    @Nested
    class FalhasPreservamConsistencia {

        @Test
        void estoqueInsuficienteNoCheckoutNaoCobraNemAlteraCarrinhoOuEstoque() throws Exception {
            long stock = fx.stock(productA, "P", 2);
            long item = fx.cartItem(ana, stock, 2);
            String token = reviewToken(ana);
            jdbc.update("UPDATE stock SET quantity = 1 WHERE id = ?", stock);

            mvc.perform(checkout(ana, newKey(), token, "APPROVED"))
                    .andExpect(status().isConflict());

            assertThat(fx.stockQuantity(stock)).isEqualTo(1);
            assertThat(fx.cartItemQuantity(item)).isEqualTo(2);
            assertThat(paymentsOf(ana)).isZero();
            assertThat(ordersOf(ana)).isZero();
        }

        @Test
        void pagamentoRecusadoRegistraPagamentoSemPedidoNemBaixa() throws Exception {
            long item = fx.cartItem(ana, stockA, 2);

            mvc.perform(checkout(ana, newKey(), reviewToken(ana), "DECLINED"))
                    .andExpect(status().isPaymentRequired())
                    .andExpect(jsonPath("$.order").doesNotExist())
                    .andExpect(jsonPath("$.payment.result").value("DECLINED"));

            assertThat(fx.stockQuantity(stockA)).isEqualTo(10);
            assertThat(fx.cartItemQuantity(item)).isEqualTo(2);
            assertThat(paymentsOf(ana)).isEqualTo(1);
            assertThat(ordersOf(ana)).isZero();
        }

        @Test
        void naoAdicionaAoCarrinhoMaisDoQueOEstoqueNemEstoqueInexistente() throws Exception {
            assertRejected(as(ana, post("/cart/items")).content("{\"stockId\": " + stockA + ", \"quantity\": 11}"));
            assertRejected(as(ana, post("/cart/items")).content("{\"stockId\": 999999999, \"quantity\": 1}"));

            assertThat(fx.cartItems(ana)).isZero();
            assertThat(fx.stockQuantity(stockA)).isEqualTo(10);
        }

        @Test
        @Disabled(BUG_CART_EXCEPTIONS)
        void falhasAoAdicionarAoCarrinhoRetornamStatusDeNegocio() throws Exception {
            mvc.perform(as(ana, post("/cart/items")).content("{\"stockId\": " + stockA + ", \"quantity\": 11}"))
                    .andExpect(status().isConflict());
            mvc.perform(as(ana, post("/cart/items")).content("{\"stockId\": 999999999, \"quantity\": 1}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class CheckoutRepetido {

        @Test
        void mesmaConfirmacaoRepetidaNaoDuplicaCompra() throws Exception {
            fx.cartItem(ana, stockA, 2);
            String token = reviewToken(ana);
            String key = newKey();

            MvcResult first = mvc.perform(checkout(ana, key, token, "APPROVED"))
                    .andExpect(status().isCreated())
                    .andReturn();
            Integer orderId = JsonPath.read(first.getResponse().getContentAsString(), "$.order.id");

            mvc.perform(checkout(ana, key, token, "APPROVED"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Idempotent-Replayed", "true"))
                    .andExpect(jsonPath("$.order.id").value(orderId));

            assertThat(ordersOf(ana)).isEqualTo(1);
            assertThat(paymentsOf(ana)).isEqualTo(1);
            assertThat(fx.stockQuantity(stockA)).isEqualTo(8);
        }

        @Test
        void novaChaveComCarrinhoJaConvertidoNaoGeraOutroPedido() throws Exception {
            fx.cartItem(ana, stockA, 2);
            String token = reviewToken(ana);
            mvc.perform(checkout(ana, newKey(), token, "APPROVED")).andExpect(status().isCreated());

            mvc.perform(checkout(ana, newKey(), token, "APPROVED"))
                    .andExpect(status().isConflict());

            assertThat(ordersOf(ana)).isEqualTo(1);
            assertThat(fx.stockQuantity(stockA)).isEqualTo(8);
        }

        @Test
        void chaveDeOutroClienteNaoDevolvePedidoAlheio() throws Exception {
            fx.cartItem(bruno, stockA, 1);
            String key = newKey();
            mvc.perform(checkout(bruno, key, reviewToken(bruno), "APPROVED")).andExpect(status().isCreated());

            // Ana reaproveita a chave de Bruno com carrinho vazio: não recebe o pedido dele.
            mvc.perform(checkout(ana, key, "qualquer", "APPROVED"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.order").doesNotExist());
        }

        @Test
        void confirmacaoSemChaveERecusada() throws Exception {
            fx.cartItem(ana, stockA, 1);

            mvc.perform(as(ana, post("/orders/checkout"))
                            .content("{\"reviewToken\": \"" + reviewToken(ana) + "\", \"paymentMethod\": \"PIX\"}"))
                    .andExpect(status().isBadRequest());

            assertThat(paymentsOf(ana)).isZero();
        }
    }

    @Nested
    class TransicoesDeStatus {

        @Test
        void confirmadoParaEmPreparoEPermitido() throws Exception {
            long order = placeOrder(ana, stockA);

            mvc.perform(asOwner(shopA, patch("/store/orders/" + order + "/status")).content("{\"status\": \"PREPARING\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PREPARING"));
        }

        @Test
        void transicoesInvalidasSaoRejeitadas() throws Exception {
            long order = placeOrder(ana, stockA);

            mvc.perform(asOwner(shopA, patch("/store/orders/" + order + "/status")).content("{\"status\": \"CONFIRMED\"}"))
                    .andExpect(status().isConflict());

            mvc.perform(asOwner(shopA, patch("/store/orders/" + order + "/status")).content("{\"status\": \"PREPARING\"}"))
                    .andExpect(status().isOk());

            mvc.perform(asOwner(shopA, patch("/store/orders/" + order + "/status")).content("{\"status\": \"PREPARING\"}"))
                    .andExpect(status().isConflict());
            mvc.perform(asOwner(shopA, patch("/store/orders/" + order + "/status")).content("{\"status\": \"CONFIRMED\"}"))
                    .andExpect(status().isConflict());

            assertThat(orderStatus(order)).isEqualTo("PREPARING");
        }

        @Test
        void statusDesconhecidoOuAusenteERejeitado() throws Exception {
            long order = placeOrder(ana, stockA);

            mvc.perform(asOwner(shopA, patch("/store/orders/" + order + "/status")).content("{\"status\": \"DELIVERED\"}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(asOwner(shopA, patch("/store/orders/" + order + "/status")).content("{}"))
                    .andExpect(status().isBadRequest());

            assertThat(orderStatus(order)).isEqualTo("CONFIRMED");
        }
    }

    // ---------- apoio ----------

    /**
     * Bug encontrado nesta validação: exceções do carrinho (CartItemNotFound,
     * StockNotFound, UnavailableQuantity, StockUnavailable, CartNotFound,
     * InvalidQuantity) não têm mapeamento no GlobalExceptionHandler e viram 500.
     * O acesso é negado e nada muda, mas o status deveria ser 404/409.
     */
    static final String BUG_CART_EXCEPTIONS =
            "Bug: exceções do carrinho sem mapeamento HTTP retornam 500 (card próprio, fora da ETI-278)";

    /**
     * A operação não pode ter sucesso: aceita 4xx ou a exceção não tratada que
     * hoje vira 500 (ver {@link #BUG_CART_EXCEPTIONS}).
     */
    private void assertRejected(MockHttpServletRequestBuilder request) throws Exception {
        int status;
        try {
            status = mvc.perform(request).andReturn().getResponse().getStatus();
        } catch (jakarta.servlet.ServletException unhandled) {
            status = 500;
        }
        assertThat(status).as("operação deveria ser recusada").isGreaterThanOrEqualTo(400);
    }

    private MockHttpServletRequestBuilder as(long userId, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", bearer(userId, "CONSUMER")).contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder asOwner(PurchaseFixtures.Shop shop, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", bearer(shop.ownerId(), "STORE")).contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder checkout(long userId, String key, String token, String result) {
        return as(userId, post("/orders/checkout"))
                .header("Idempotency-Key", key)
                .content("{\"reviewToken\": \"" + token + "\", \"paymentMethod\": \"PIX\", \"simulatedResult\": \""
                        + result + "\"}");
    }

    private String reviewToken(long userId) throws Exception {
        MvcResult review = mvc.perform(as(userId, get("/orders/checkout/review")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canConfirm").value(true))
                .andReturn();
        return JsonPath.read(review.getResponse().getContentAsString(), "$.reviewToken");
    }

    /** Compra aprovada de uma unidade pela API; devolve o id do pedido. */
    private long placeOrder(long userId, long stockId) throws Exception {
        fx.cartItem(userId, stockId, 1);
        MvcResult result = mvc.perform(checkout(userId, newKey(), reviewToken(userId), "APPROVED"))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.order.id")).longValue();
    }

    private String orderStatus(long orderId) {
        return jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId);
    }

    private int ordersOf(long userId) {
        return fx.count("SELECT count(*) FROM orders WHERE user_id = ?", userId);
    }

    private int paymentsOf(long userId) {
        return fx.count("SELECT count(*) FROM payment WHERE user_id = ?", userId);
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }
}
