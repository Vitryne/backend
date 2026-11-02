package com.vitryne.api.shared.exception;

public class StockUnavailableException extends RuntimeException {
    public StockUnavailableException(String size) {
        super("Stock unavailable for size: " + size);
    }
}
