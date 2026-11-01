package com.vitryne.api.product.entity;

import com.vitryne.api.shared.exception.InvalidInputException;
import com.vitryne.api.shared.exception.InvalidPromotionalPriceException;
import com.vitryne.api.store.entity.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ProductTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "0",
            "-1",
            "10.001",
            "10000000000.00"
    })
    void deveRejeitarPrecoInvalidoNoBuilder(String value) {
        BigDecimal price = value == null ? null : new BigDecimal(value);

        assertThatThrownBy(() -> builder()
                .price(price)
                .build()
        ).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void deveRejeitarProdutoSemLoja() {
        assertThatThrownBy(() -> builder()
                .store(null)
                .build()
        ).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void deveCriarSemPromocaoEComEstoquesInicializados() {
        Product product = builder().build();

        assertThat(product.getStocks()).isNotNull().isEmpty();
        assertThat(product.getPromotionalPrice()).isNull();
        assertThat(product.isItInSale()).isFalse();
        assertThat(product.calculateFinalPrice())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void deveRejeitarTamanhoDuplicadoAposNormalizacao() {
        Product product = builder().build();
        product.addStock(" m ", 5);

        assertThatThrownBy(() -> product.addStock("M", 2))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(product.getStocks()).hasSize(1);
        assertThat(product.getStocks().getFirst().getQuantity())
                .isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(longs = {-3600, 3600})
    void naoDeveReduzirPrecoAteValorDaPromocaoVigenteOuFutura(
            long startOffset
    ) {
        Product product = builder().build();
        Instant now = Instant.now();

        product.configureSale(
                new BigDecimal("80.00"),
                now.plusSeconds(startOffset),
                now.plusSeconds(86400)
        );

        assertThatThrownBy(() ->
                update(product, new BigDecimal("80.00"))
        ).isInstanceOf(InvalidInputException.class);

        assertThat(product.getName()).isEqualTo("Camiseta");
        assertThat(product.getPrice()).isEqualByComparingTo("100.00");
        assertThat(product.getPromotionalPrice())
                .isEqualByComparingTo("80.00");
    }

    @Test
    void promocaoInvalidaNaoDeveAlterarPromocaoAnterior() {
        Product product = builder().build();

        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now().plusSeconds(86400);

        product.configureSale(new BigDecimal("80.00"), start, end);

        assertThatThrownBy(() -> product.configureSale(
                new BigDecimal("70.001"),
                start.plusSeconds(60),
                end.plusSeconds(60)
        )).isInstanceOf(InvalidPromotionalPriceException.class);

        assertThat(product.getPromotionalPrice())
                .isEqualByComparingTo("80.00");
        assertThat(product.getPromotionalStartDate()).isEqualTo(start);
        assertThat(product.getPromotionalEndDate()).isEqualTo(end);
    }

    @Test
    void deveAplicarPrecoPromocionalQuandoPromocaoEstaVigente() {
        Product product = builder().build();

        product.configureSale(
                new BigDecimal("80.00"),
                Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(86400)
        );

        assertThat(product.isItInSale()).isTrue();
        assertThat(product.calculateFinalPrice())
                .isEqualByComparingTo("80.00");
    }

    private void update(Product product, BigDecimal price) {
        product.updateDetails(
                "Nome alterado",
                "Descrição alterada",
                price,
                "camiseta",
                "azul",
                3,
                List.of("https://example.com/new.jpg")
        );
    }

    private Product.ProductBuilder builder() {
        return Product.builder()
                .name("Camiseta")
                .description("Camiseta de algodão")
                .price(new BigDecimal("100.00"))
                .type("camiseta")
                .status("ACTIVE")
                .photoUrls(List.of("https://example.com/photo.jpg"))
                .store(mock(Store.class));
    }
}