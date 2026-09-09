package com.vitryne.api.exception;

public class StockUnavailableException extends RuntimeException {
    public StockUnavailableException(String size) {
        super("Stock unavailable for size: " + size);
    }
}
