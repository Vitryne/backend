package com.vitryne.api.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String size, Integer available, Integer requested) {
        super(String.format(
                "Insufficient stock for size %s: available %d, requested %d",
                size, available, requested
        ));
    }
}
