package com.vitryne.api.shared.exception;

public class StockNotFoundException extends RuntimeException {
    public StockNotFoundException(Long id) {
        super("Stock not found: " + id);
    }
}
