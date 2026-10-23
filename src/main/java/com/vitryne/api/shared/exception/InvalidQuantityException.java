package com.vitryne.api.shared.exception;

public class InvalidQuantityException extends RuntimeException {
    public InvalidQuantityException(Integer quantity) {
        super(String.format("Invalid quantity: (%d)", quantity));
    }
}
