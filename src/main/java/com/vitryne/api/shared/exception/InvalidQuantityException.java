package com.vitryne.api.shared.exception;

public class InvalidQuantityException extends RuntimeException {
    public InvalidQuantityException(Integer quantity) {
        super("A quantidade deve ser maior que zero");
    }
}
