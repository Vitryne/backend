package com.vitryne.api.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Exceções do carrinho devem virar 4xx com mensagem, nunca 500 (ETI-396). */
class GlobalExceptionHandlerCartTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void recursoInexistenteRetorna404() {
        assertResponse(handler.handleCartNotFound(new CartItemNotFoundException(7L)),
                HttpStatus.NOT_FOUND, "Item 7 não encontrado no seu carrinho");
        assertResponse(handler.handleCartNotFound(new CartNotFoundException(10L)),
                HttpStatus.NOT_FOUND, "Carrinho não encontrado");
        assertResponse(handler.handleCartNotFound(new StockNotFoundException(3L)),
                HttpStatus.NOT_FOUND, "Estoque 3 não encontrado");
    }

    @Test
    void faltaDeEstoqueRetorna409() {
        assertResponse(handler.handleCartStockConflict(new UnavailableQuantityException("M", 2, 5)),
                HttpStatus.CONFLICT, "Quantidade solicitada (5) maior que o estoque disponível (2) para o tamanho M");
        assertResponse(handler.handleCartStockConflict(new StockUnavailableException("G")),
                HttpStatus.CONFLICT, "O tamanho G está esgotado");
    }

    @Test
    void quantidadeInvalidaRetorna400() {
        assertResponse(handler.handleInvalidQuantity(new InvalidQuantityException(0)),
                HttpStatus.BAD_REQUEST, "A quantidade deve ser maior que zero");
    }

    private static void assertResponse(ResponseEntity<Map<String, Object>> response, HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).containsEntry("message", message);
    }
}
