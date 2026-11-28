package com.vitryne.api.store.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /stores pela API, sem token, contra Postgres real. */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "jwt.public.key=classpath:keys/dev/public.pem",
        "jwt.private.key=classpath:keys/dev/private.pem",
        "vitryne.scheduling.enabled=false",
        "vitryne.images.cloudinary-url="
})
class StoreCatalogIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

    private static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 1_000_000);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void listaSoLojasAtivasComProdutoAtivoSemToken() throws Exception {
        long visivel = store(true, "ACTIVE");
        long inativa = store(false, "ACTIVE");
        long semProdutoAtivo = store(true, "INACTIVE");
        long semProduto = store(true, null);

        mvc.perform(get("/stores").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(hasItem((int) visivel)))
                .andExpect(jsonPath("$.content[*].id").value(not(hasItem((int) inativa))))
                .andExpect(jsonPath("$.content[*].id").value(not(hasItem((int) semProdutoAtivo))))
                .andExpect(jsonPath("$.content[*].id").value(not(hasItem((int) semProduto))));
    }

    @Test
    void trazOsCamposDaVitrineESemDadosSensiveis() throws Exception {
        long id = store(true, "ACTIVE");
        jdbc.update("INSERT INTO store_tag (store_id, tag) VALUES (?, 'casual')", id);
        jdbc.update("""
                INSERT INTO store_opening_period (store_id, day_of_week, opens_at, closes_at)
                VALUES (?, 'TUESDAY', '13:00', '18:00'), (?, 'MONDAY', '08:00', '12:00')
                """, id, id);
        String item = "$.content[?(@.id == " + id + ")]";

        mvc.perform(get("/stores").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(item + ".name").value(hasItem("Loja " + id)))
                .andExpect(jsonPath(item + ".mainCategory").value(hasItem("Moda")))
                .andExpect(jsonPath(item + ".photoUrl").value(hasItem("https://example.com/logo.jpg")))
                .andExpect(jsonPath(item + ".bannerUrl").value(hasItem("https://example.com/banner.jpg")))
                .andExpect(jsonPath(item + ".preparationTimeMinutes").value(hasItem(30)))
                .andExpect(jsonPath(item + ".tags[0]").value(hasItem("casual")))
                .andExpect(jsonPath(item + ".openingPeriods[0].dayOfWeek").value(hasItem("MONDAY")))
                .andExpect(jsonPath(item + ".openingPeriods[0].opensAt").value(hasItem("08:00:00")))
                .andExpect(jsonPath(item + ".isOpen").exists())
                .andExpect(jsonPath(item + ".cnpj").doesNotExist())
                .andExpect(jsonPath(item + ".phone").doesNotExist())
                .andExpect(jsonPath(item + ".legalName").doesNotExist())
                .andExpect(jsonPath(item + ".bankAccount").doesNotExist())
                .andExpect(jsonPath(item + ".address").doesNotExist())
                .andExpect(jsonPath(item + ".owner").doesNotExist())
                .andExpect(jsonPath(item + ".active").doesNotExist());
    }

    @Test
    void paginaEmOrdemAlfabetica() throws Exception {
        store(true, "ACTIVE");
        store(true, "ACTIVE");

        mvc.perform(get("/stores").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalPages").value(greaterThanOrEqualTo(2)));

        String body = mvc.perform(get("/stores").param("size", "50"))
                .andReturn().getResponse().getContentAsString();
        List<String> names = JsonPath.read(body, "$.content[*].name");
        assertThat(names).hasSizeGreaterThanOrEqualTo(2).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
    }

    @Test
    void paginacaoInvalidaRetorna400() throws Exception {
        mvc.perform(get("/stores").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O tamanho da página deve estar entre 1 e 50"));
        mvc.perform(get("/stores").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    /**
     * @param productStatus status do único produto da loja; null cria a loja sem produto
     * @return id da loja
     */
    private long store(boolean active, String productStatus) {
        long n = SEQ.incrementAndGet();
        long owner = jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, role) VALUES (?, 'x', 'STORE') RETURNING id",
                Long.class, "lojista" + n + "@teste.com"
        );
        long store = jdbc.queryForObject("""
                INSERT INTO store (owner_id, name, active, cnpj, phone, legal_name, main_category,
                                   photo_url, banner_url, preparation_time_minutes)
                VALUES (?, 'tmp', ?, ?, '44999999999', 'Razão Social', 'Moda',
                        'https://example.com/logo.jpg', 'https://example.com/banner.jpg', 30)
                RETURNING id
                """, Long.class, owner, active, String.format("%014d", n));
        jdbc.update("UPDATE store SET name = ? WHERE id = ?", "Loja " + store, store);
        if (productStatus != null) {
            jdbc.update("""
                    INSERT INTO product (name, price, type, status, store_id, updated_at)
                    VALUES ('Camiseta', 19.90, 'CAMISETA', ?, ?, now())
                    """, productStatus, store);
        }
        return store;
    }
}
