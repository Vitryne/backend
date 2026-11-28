package com.vitryne.api.store;

import com.vitryne.api.support.PostgresIntegrationTest;
import com.vitryne.api.support.PurchaseFixtures;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Loja exposta no catálogo e no carrinho para o app (ETI-410). */
class StoreExposureIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManagerFactory emf;

    private PurchaseFixtures fx;
    private PurchaseFixtures.Shop shopA;
    private PurchaseFixtures.Shop shopB;
    private long productA;
    private long productB;
    private long stockA;

    @BeforeEach
    void setUp() {
        fx = new PurchaseFixtures(jdbc);
        shopA = fx.shop();
        shopB = fx.shop();
        productA = fx.product(shopA);
        productB = fx.product(shopB);
        stockA = fx.stock(productA, "M", 10);
        fx.stock(productB, "M", 10);
    }

    @AfterEach
    void disableStatistics() {
        statistics().setStatisticsEnabled(false);
    }

    @Test
    void catalogoRetornaALojaDeCadaProduto() throws Exception {
        mvc.perform(get("/product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + productA + ")].store.id").value(contains((int) shopA.storeId())))
                .andExpect(jsonPath("$[?(@.id == " + productA + ")].store.name").value(contains(storeName(shopA))))
                .andExpect(jsonPath("$[?(@.id == " + productB + ")].store.id").value(contains((int) shopB.storeId())))
                .andExpect(jsonPath("$[?(@.id == " + productB + ")].store.name").value(contains(storeName(shopB))));
    }

    @Test
    void detalheDoProdutoRetornaALoja() throws Exception {
        mvc.perform(get("/product/client/" + productB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.store.id").value(shopB.storeId()))
                .andExpect(jsonPath("$.store.name").value(storeName(shopB)));
    }

    /** Mais produtos de lojas diferentes não aumentam o número de consultas do catálogo. */
    @Test
    void catalogoCarregaALojaSemConsultaPorProduto() throws Exception {
        long before = statementsFor(get("/product"));

        for (int i = 0; i < 3; i++) {
            fx.stock(fx.product(fx.shop()), "P", 1);
        }

        assertThat(statementsFor(get("/product"))).isEqualTo(before);
    }

    @Test
    void carrinhoVazioRetornaLojaNula() throws Exception {
        long client = fx.consumer();

        mvc.perform(as(client, get("/cart")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.store").isEmpty());
    }

    @Test
    void carrinhoRetornaALojaDosItens() throws Exception {
        long client = fx.consumerWithCart(stockA, 1);

        mvc.perform(as(client, get("/cart")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.store.id").value(shopA.storeId()))
                .andExpect(jsonPath("$.store.name").value(storeName(shopA)));
    }

    @Test
    void operacoesDoCarrinhoRetornamALoja() throws Exception {
        long client = fx.consumer();

        mvc.perform(as(client, post("/cart/items")).content("{\"stockId\": " + stockA + ", \"quantity\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.store.id").value(shopA.storeId()))
                .andExpect(jsonPath("$.store.name").value(storeName(shopA)));

        long item = jdbc.queryForObject(
                "SELECT ci.id FROM cart_item ci JOIN cart c ON c.id = ci.cart_id WHERE c.user_id = ?",
                Long.class, client
        );

        mvc.perform(as(client, put("/cart/items/" + item)).content("{\"quantity\": 2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.store.id").value(shopA.storeId()));

        mvc.perform(as(client, delete("/cart/items/" + item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.store").isEmpty());
    }

    @Test
    void esvaziarOCarrinhoZeraALoja() throws Exception {
        long client = fx.consumerWithCart(stockA, 1);

        mvc.perform(as(client, delete("/cart/items")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.store").isEmpty());
    }

    // ---------- apoio ----------

    private String storeName(PurchaseFixtures.Shop shop) {
        return jdbc.queryForObject("SELECT name FROM store WHERE id = ?", String.class, shop.storeId());
    }

    private Statistics statistics() {
        return emf.unwrap(SessionFactory.class).getStatistics();
    }

    private long statementsFor(MockHttpServletRequestBuilder request) throws Exception {
        Statistics stats = statistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        mvc.perform(request).andExpect(status().isOk());
        return stats.getPrepareStatementCount();
    }

    private MockHttpServletRequestBuilder as(long userId, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", bearer(userId, "CONSUMER")).contentType(MediaType.APPLICATION_JSON);
    }
}
