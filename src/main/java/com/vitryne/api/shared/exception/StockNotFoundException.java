package com.vitryne.api.shared.exception;

public class StockNotFoundException extends RuntimeException {
    public StockNotFoundException(Long id) {
        super("Estoque " + id + " não encontrado");
    }
}
