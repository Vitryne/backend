package com.vitryne.api.stock.entity;

import com.vitryne.api.product.entity.Product;
import com.vitryne.api.shared.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class StockTest {

    @Test
    void deveNormalizarTamanhoEAceitarQuantidadeZero() {
        Stock stock = stock(" m ", 0);

        assertThat(stock.getSize()).isEqualTo("M");
        assertThat(stock.getQuantity()).isZero();
        assertThat(stock.isAvailable()).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "ABCDEFGHIJK"})
    void deveRejeitarTamanhoInvalido(String size) {
        assertThatThrownBy(() -> stock(size, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, -10})
    void deveRejeitarQuantidadeInicialInvalida(Integer quantity) {
        assertThatThrownBy(() -> stock("M", quantity))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRejeitarEstoqueSemProduto() {
        assertThatThrownBy(() -> new Stock(null, "M", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void deveRejeitarMovimentacoesNaoPositivas(int quantity) {
        Stock stock = stock("M", 5);

        assertThatThrownBy(() -> stock.increaseStock(quantity))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> stock.decreaseStock(quantity))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(stock.getQuantity()).isEqualTo(5);
    }

    @Test
    void deveImpedirBaixaMaiorQueQuantidadeDisponivel() {
        Stock stock = stock("M", 5);

        assertThatThrownBy(() -> stock.decreaseStock(6))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(stock.getQuantity()).isEqualTo(5);
    }

    @Test
    void devePermitirBaixarTodoEstoque() {
        Stock stock = stock("M", 5);

        stock.decreaseStock(5);

        assertThat(stock.getQuantity()).isZero();
        assertThat(stock.isAvailable()).isFalse();
    }

    @Test
    void deveRejeitarOverflowSemAlterarQuantidade() {
        Stock stock = stock("M", Integer.MAX_VALUE);

        assertThatThrownBy(() -> stock.increaseStock(1))
                .isInstanceOf(ArithmeticException.class);

        assertThat(stock.getQuantity()).isEqualTo(Integer.MAX_VALUE);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1})
    void deveRejeitarAjusteInvalidoSemAlterarQuantidade(Integer quantity) {
        Stock stock = stock("M", 5);

        assertThatThrownBy(() -> stock.adjustQuantity(quantity))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(stock.getQuantity()).isEqualTo(5);
    }

    private Stock stock(String size, Integer quantity) {
        return new Stock(mock(Product.class), size, quantity);
    }
}