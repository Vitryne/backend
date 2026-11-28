package com.vitryne.api.security;

import com.jayway.jsonpath.JsonPath;
import com.vitryne.api.support.PostgresIntegrationTest;
import com.vitryne.api.support.PurchaseFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
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
        void itemDoCarrinhoAlheioRetornaNaoEncontrado() throws Exception {
            fx.cartItem(ana, stockA, 1);
            long itemBruno = fx.cartItem(bruno, stockA, 3);

            mvc.perform(as(ana, put("/cart/items/" + itemBruno)).content("{\"quantity\": 1}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Item " + itemBruno + " não encontrado no seu carrinho"));
            mvc.perform(as(ana, delete("/cart/items/" + itemBruno)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void operarItemSemCarrinhoRetornaNaoEncontrado() throws Exception {
            long itemBruno = fx.cartItem(bruno, stockA, 3);

            mvc.perform(as(ana, delete("/cart/items/" + itemBruno)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Carrinho não encontrado"));
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
        void naoCancelaPedidoAlheio() throws Exception {
            long orderBruno = placeOrder(bruno, stockA);

            mvc.perform(as(ana, post("/orders/" + orderBruno + "/cancel")).content("{\"reason\": \"CHANGED_MIND\"}"))
                    .andExpect(status().isNotFound());

            assertThat(orderStatus(orderBruno)).isEqualTo("AWAITING_ACCEPTANCE");
            assertThat(fx.stockQuantity(stockA)).isEqualTo(9);
        }

        @Test
        void naoUsaRotasDeLojista() throws Exception {
            long orderAna = placeOrder(ana, stockA);

            mvc.perform(as(ana, get("/store/orders"))).andExpect(status().isForbidden());
            mvc.perform(as(ana, patch("/store/orders/" + orderAna + "/status")).content("{\"status\": \"CONFIRMED\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(as(ana, post("/store/orders/" + orderAna + "/reject")).content("{\"reason\": \"STORE_CLOSED\"}"))
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
                            .content("{\"status\": \"CONFIRMED\"}"))
                    .andExpect(status().isNotFound());
            mvc.perform(asOwner(shopA, post("/store/orders/" + orderLojaB + "/reject"))
                            .content("{\"reason\": \"STORE_CLOSED\"}"))
                    .andExpect(status().isNotFound());

            mvc.perform(asOwner(shopA, get("/store/orders")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            assertThat(orderStatus(orderLojaB)).isEqualTo("AWAITING_ACCEPTANCE");
            assertThat(fx.stockQuantity(stockB)).isEqualTo(9);
        }

        @Test
        void naoUsaRotasDeCliente() throws Exception {
            mvc.perform(asOwner(shopA, get("/cart"))).andExpect(status().isForbidden());
            mvc.perform(asOwner(shopA, post("/cart/items")).content("{\"stockId\": " + stockB + ", \"quantity\": 1}"))
                    .andExpect(status().isForbidden());
            mvc.perform(asOwner(shopA, get("/orders"))).andExpect(status().isForbidden());

            long orderAna = placeOrder(ana, stockA);
            mvc.perform(asOwner(shopA, post("/orders/" + orderAna + "/cancel")).content("{\"reason\": \"OTHER\"}"))
                    .andExpect(status().isForbidden());
            assertThat(orderStatus(orderAna)).isEqualTo("AWAITING_ACCEPTANCE");
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
        void falhasAoAdicionarAoCarrinhoRetornamStatusDeNegocio() throws Exception {
            mvc.perform(as(ana, post("/cart/items")).content("{\"stockId\": " + stockA + ", \"quantity\": 11}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            "Quantidade solicitada (11) maior que o estoque disponível (10) para o tamanho M"));
            mvc.perform(as(ana, post("/cart/items")).content("{\"stockId\": 999999999, \"quantity\": 1}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Estoque 999999999 não encontrado"));

            long esgotado = fx.stock(productA, "GG", 0);
            mvc.perform(as(ana, post("/cart/items")).content("{\"stockId\": " + esgotado + ", \"quantity\": 1}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("O tamanho GG está esgotado"));
        }

        @Test
        void alterarQuantidadeAcimaDoEstoqueRetornaConflito() throws Exception {
            long item = fx.cartItem(ana, stockA, 1);

            mvc.perform(as(ana, put("/cart/items/" + item)).content("{\"quantity\": 11}"))
                    .andExpect(status().isConflict());

            assertThat(fx.cartItemQuantity(item)).isEqualTo(1);
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
        void pedidoNasceAguardandoAceite() throws Exception {
            long order = placeOrder(ana, stockA);

            mvc.perform(as(ana, get("/orders/" + order)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AWAITING_ACCEPTANCE"))
                    .andExpect(jsonPath("$.cancellation").doesNotExist());
        }

        @Test
        void lojaAceitaEAvancaUmaEtapaPorVezAteAEntrega() throws Exception {
            long order = placeOrder(ana, stockA);

            for (String next : new String[]{"CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "OUT_FOR_DELIVERY", "DELIVERED"}) {
                advance(order, next)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(next));
            }

            assertThat(fx.stockQuantity(stockA)).as("entrega não devolve estoque").isEqualTo(9);
        }

        @Test
        void pularRepetirOuVoltarEtapaRetorna422() throws Exception {
            long order = placeOrder(ana, stockA);

            advance(order, "PREPARING")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.message").value(
                            "Não é possível alterar o pedido de AWAITING_ACCEPTANCE para PREPARING; "
                                    + "a próxima etapa é CONFIRMED"));
            advance(order, "DELIVERED").andExpect(status().isUnprocessableContent());

            advance(order, "CONFIRMED").andExpect(status().isOk());
            advance(order, "CONFIRMED").andExpect(status().isUnprocessableContent());
            advance(order, "AWAITING_ACCEPTANCE").andExpect(status().isUnprocessableContent());
            advance(order, "CANCELED").andExpect(status().isUnprocessableContent());

            assertThat(orderStatus(order)).isEqualTo("CONFIRMED");
        }

        @Test
        void statusDesconhecidoOuAusenteERejeitado() throws Exception {
            long order = placeOrder(ana, stockA);

            advance(order, "SHIPPED").andExpect(status().isBadRequest());
            mvc.perform(asOwner(shopA, patch("/store/orders/" + order + "/status")).content("{}"))
                    .andExpect(status().isBadRequest());

            assertThat(orderStatus(order)).isEqualTo("AWAITING_ACCEPTANCE");
        }

        @Test
        void lojaRecusaComJustificativaEOEstoqueVolta() throws Exception {
            long order = placeOrder(ana, stockA);
            assertThat(fx.stockQuantity(stockA)).isEqualTo(9);

            reject(order, "ITEM_UNAVAILABLE")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELED"))
                    .andExpect(jsonPath("$.cancellation.canceledBy").value("STORE"))
                    .andExpect(jsonPath("$.cancellation.reason").value("ITEM_UNAVAILABLE"))
                    .andExpect(jsonPath("$.cancellation.canceledAt").exists());

            assertThat(fx.stockQuantity(stockA)).isEqualTo(10);
            mvc.perform(as(ana, get("/orders/" + order)))
                    .andExpect(jsonPath("$.status").value("CANCELED"))
                    .andExpect(jsonPath("$.cancellation.canceledBy").value("STORE"));
        }

        @Test
        void recusaExigeJustificativaDaListaESoAntesDoAceite() throws Exception {
            long order = placeOrder(ana, stockA);

            mvc.perform(asOwner(shopA, post("/store/orders/" + order + "/reject")).content("{}"))
                    .andExpect(status().isBadRequest());
            reject(order, "PORQUE_SIM").andExpect(status().isBadRequest());

            advance(order, "CONFIRMED").andExpect(status().isOk());
            reject(order, "STORE_CLOSED").andExpect(status().isUnprocessableContent());

            assertThat(orderStatus(order)).isEqualTo("CONFIRMED");
            assertThat(fx.stockQuantity(stockA)).isEqualTo(9);
        }

        @Test
        void clienteCancelaAteOPreparoEOEstoqueVolta() throws Exception {
            long order = placeOrder(ana, stockA);
            advance(order, "CONFIRMED").andExpect(status().isOk());
            advance(order, "PREPARING").andExpect(status().isOk());

            cancel(ana, order, "CHANGED_MIND")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELED"))
                    .andExpect(jsonPath("$.cancellation.canceledBy").value("CLIENT"))
                    .andExpect(jsonPath("$.cancellation.reason").value("CHANGED_MIND"));

            assertThat(fx.stockQuantity(stockA)).isEqualTo(10);
        }

        @Test
        void clienteNaoCancelaAPartirDoProntoParaColeta() throws Exception {
            long order = placeOrder(ana, stockA);
            for (String next : new String[]{"CONFIRMED", "PREPARING", "READY_FOR_PICKUP"}) {
                advance(order, next).andExpect(status().isOk());
            }

            cancel(ana, order, "CHANGED_MIND")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.message").value(
                            "O pedido não pode mais ser cancelado; ele está READY_FOR_PICKUP"));

            assertThat(orderStatus(order)).isEqualTo("READY_FOR_PICKUP");
            assertThat(fx.stockQuantity(stockA)).isEqualTo(9);
        }

        @Test
        void cancelamentoExigeMotivo() throws Exception {
            long order = placeOrder(ana, stockA);

            mvc.perform(as(ana, post("/orders/" + order + "/cancel")).content("{}"))
                    .andExpect(status().isBadRequest());

            assertThat(orderStatus(order)).isEqualTo("AWAITING_ACCEPTANCE");
        }

        @Test
        void canceladoEEntregueSaoFinais() throws Exception {
            long cancelado = placeOrder(ana, stockA);
            cancel(ana, cancelado, "OTHER").andExpect(status().isOk());

            advance(cancelado, "CONFIRMED").andExpect(status().isUnprocessableContent());
            reject(cancelado, "STORE_CLOSED").andExpect(status().isUnprocessableContent());
            cancel(ana, cancelado, "OTHER").andExpect(status().isUnprocessableContent());
            assertThat(fx.stockQuantity(stockA)).as("estoque devolvido uma única vez").isEqualTo(10);

            long entregue = placeOrder(bruno, stockA);
            for (String next : new String[]{"CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "OUT_FOR_DELIVERY", "DELIVERED"}) {
                advance(entregue, next).andExpect(status().isOk());
            }
            advance(entregue, "DELIVERED")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.message").value("O pedido está DELIVERED e não pode mais ser alterado"));
            cancel(bruno, entregue, "OTHER").andExpect(status().isUnprocessableContent());

            assertThat(orderStatus(cancelado)).isEqualTo("CANCELED");
            assertThat(orderStatus(entregue)).isEqualTo("DELIVERED");
        }

        private ResultActions advance(long order, String next) throws Exception {
            return mvc.perform(asOwner(shopA, patch("/store/orders/" + order + "/status"))
                    .content("{\"status\": \"" + next + "\"}"));
        }

        private ResultActions reject(long order, String reason) throws Exception {
            return mvc.perform(asOwner(shopA, post("/store/orders/" + order + "/reject"))
                    .content("{\"reason\": \"" + reason + "\"}"));
        }

        private ResultActions cancel(long user, long order, String reason)
                throws Exception {
            return mvc.perform(as(user, post("/orders/" + order + "/cancel"))
                    .content("{\"reason\": \"" + reason + "\"}"));
        }
    }

    // ---------- apoio ----------

    /** A operação deve ser recusada como erro do cliente (4xx), nunca 2xx nem 500. */
    private void assertRejected(MockHttpServletRequestBuilder request) throws Exception {
        int status = mvc.perform(request).andReturn().getResponse().getStatus();
        assertThat(status).as("operação deveria ser recusada com 4xx").isBetween(400, 499);
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
