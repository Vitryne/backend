package com.vitryne.api.exception;

public class StockNotFoundException extends RuntimeException {
    public StockNotFoundException(Long id) {
        super("Stock not found: " + id);
    }
}
