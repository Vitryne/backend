package com.vitryne.api.security;

import com.jayway.jsonpath.JsonPath;
import com.vitryne.api.support.PostgresIntegrationTest;
import com.vitryne.api.support.PurchaseFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /client/me e GET /store/me: dados do usuário do token, por papel (ETI-411). */
class MeEndpointsIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private PurchaseFixtures fx;

    @BeforeEach
    void setUp() {
        fx = new PurchaseFixtures(jdbc);
    }

    // ---------- /client/me ----------

    @Test
    void clienteVeNomeEmailEEnderecoSelecionado() throws Exception {
        long user = fx.consumer();
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT u.email, c.name, c.selected_address_id
                FROM users u JOIN client c ON c.user_id = u.id WHERE u.id = ?
                """, user);

        mvc.perform(get("/client/me").header("Authorization", bearer(user, "CONSUMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(row.get("name")))
                .andExpect(jsonPath("$.email").value(row.get("email")))
                .andExpect(jsonPath("$.selectedAddress.id").value(row.get("selected_address_id")))
                .andExpect(jsonPath("$.selectedAddress.selected").value(true))
                .andExpect(jsonPath("$.selectedAddress.zipCode").value("87000000"))
                .andExpect(jsonPath("$.selectedAddress.city").value("Maringá"));
    }

    @Test
    void clienteSemEnderecoSelecionadoRecebeEnderecoNulo() throws Exception {
        long user = fx.user("semendereco" + System.nanoTime() + "@teste.com", "CONSUMER");
        jdbc.update("INSERT INTO client (user_id, name, cpf) VALUES (?, 'Sem Endereço', '99988877766')", user);

        mvc.perform(get("/client/me").header("Authorization", bearer(user, "CONSUMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sem Endereço"))
                .andExpect(jsonPath("$.selectedAddress").isEmpty());
    }

    @Test
    void perfilDoClienteNaoExpoeSenhaNemCpf() throws Exception {
        long user = fx.consumer();

        String body = mvc.perform(get("/client/me").header("Authorization", bearer(user, "CONSUMER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(keys(body)).containsExactlyInAnyOrder("name", "email", "selectedAddress");

        mvc.perform(get("/client/me").header("Authorization", bearer(user, "CONSUMER")))
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void clienteMeExigeTokenEPapelDeCliente() throws Exception {
        PurchaseFixtures.Shop shop = fx.shop();

        mvc.perform(get("/client/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/client/me").header("Authorization", bearer(shop.ownerId(), "STORE")))
                .andExpect(status().isForbidden());
    }

    // ---------- /store/me ----------

    @Test
    void lojistaVeOsDadosDaPropriaLoja() throws Exception {
        PurchaseFixtures.Shop shop = completeShop();

        mvc.perform(get("/store/me").header("Authorization", bearer(shop.ownerId(), "STORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shop.storeId()))
                .andExpect(jsonPath("$.name").value(storeName(shop)))
                .andExpect(jsonPath("$.photoUrl").value("https://img/loja.png"))
                .andExpect(jsonPath("$.mainCategory").value("Moda feminina"))
                .andExpect(jsonPath("$.deliveryRadiusKm").value(5.5))
                .andExpect(jsonPath("$.preparationTimeMinutes").value(30))
                .andExpect(jsonPath("$.address.zipCode").value("87010000"))
                .andExpect(jsonPath("$.address.street").value("Avenida Brasil"))
                .andExpect(jsonPath("$.address.number").value("1000"))
                .andExpect(jsonPath("$.address.city").value("Maringá"))
                .andExpect(jsonPath("$.address.state").value("PR"));
    }

    @Test
    void cadaLojistaRecebeSomenteASuaLoja() throws Exception {
        PurchaseFixtures.Shop a = completeShop();
        PurchaseFixtures.Shop b = completeShop();

        mvc.perform(get("/store/me").header("Authorization", bearer(a.ownerId(), "STORE")))
                .andExpect(jsonPath("$.id").value(a.storeId()));
        // Parâmetro de id é ignorado: a loja vem sempre do token.
        mvc.perform(get("/store/me").param("id", String.valueOf(a.storeId()))
                        .header("Authorization", bearer(b.ownerId(), "STORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(b.storeId()));
    }

    @Test
    void lojaNaoExpoeDocumentosNemDadosBancarios() throws Exception {
        PurchaseFixtures.Shop shop = completeShop();

        String body = mvc.perform(get("/store/me").header("Authorization", bearer(shop.ownerId(), "STORE")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(keys(body)).containsExactlyInAnyOrder(
                "id", "name", "photoUrl", "address", "mainCategory",
                "deliveryRadiusKm", "preparationTimeMinutes");
        assertThat(body).doesNotContain(String.format("%014d", shop.storeId()), "12345678000199",
                "99999-9", "pix@loja.com", "Loja LTDA");

        mvc.perform(get("/store/me").header("Authorization", bearer(shop.ownerId(), "STORE")))
                .andExpect(jsonPath("$.cnpj").doesNotExist())
                .andExpect(jsonPath("$.legalName").doesNotExist())
                .andExpect(jsonPath("$.bankAccount").doesNotExist())
                .andExpect(jsonPath("$.owner").doesNotExist());
    }

    @Test
    void lojaComDadosOpcionaisVaziosRetornaNulos() throws Exception {
        PurchaseFixtures.Shop shop = fx.shop();

        mvc.perform(get("/store/me").header("Authorization", bearer(shop.ownerId(), "STORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shop.storeId()))
                .andExpect(jsonPath("$.photoUrl").isEmpty())
                .andExpect(jsonPath("$.deliveryRadiusKm").isEmpty());
    }

    @Test
    void storeMeExigeTokenEPapelDeLojista() throws Exception {
        long client = fx.consumer();

        mvc.perform(get("/store/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/store/me").header("Authorization", bearer(client, "CONSUMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void lojistaSemLojaRecebeNaoEncontrado() throws Exception {
        long owner = fx.user("semloja" + System.nanoTime() + "@teste.com", "STORE");

        mvc.perform(get("/store/me").header("Authorization", bearer(owner, "STORE")))
                .andExpect(status().isNotFound());
    }

    // ---------- apoio ----------

    /** Loja com todos os dados do cadastro, inclusive os sensíveis que não podem vazar. */
    private PurchaseFixtures.Shop completeShop() {
        PurchaseFixtures.Shop shop = fx.shop();
        long bank = jdbc.queryForObject("""
                INSERT INTO store_bank_account
                    (holder_name, holder_document, account_type, bank_code, agency, account_number, pix_key)
                VALUES ('Titular', '12345678000199', 'CHECKING', '001', '1234', '99999-9', 'pix@loja.com')
                RETURNING id
                """, Long.class);
        jdbc.update("""
                UPDATE store SET photo_url = 'https://img/loja.png', main_category = 'Moda feminina',
                    delivery_radius_km = 5.5, preparation_time_minutes = 30,
                    zip_code = '87010000', street = 'Avenida Brasil', address_number = '1000',
                    neighborhood = 'Centro', city = 'Maringá', state = 'PR',
                    cnpj = ?, legal_name = 'Loja LTDA', bank_account_id = ?
                WHERE id = ?
                """, String.format("%014d", shop.storeId()), bank, shop.storeId());
        assertThat(storeName(shop)).isNotBlank();
        return shop;
    }

    private static java.util.Set<String> keys(String json) {
        Map<String, Object> map = JsonPath.read(json, "$");
        return map.keySet();
    }

    private String storeName(PurchaseFixtures.Shop shop) {
        return jdbc.queryForObject("SELECT name FROM store WHERE id = ?", String.class, shop.storeId());
    }
}
