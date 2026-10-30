package com.vitryne.api.shared.exception;

public class NoProductsInThatStoreException extends RuntimeException {
    public NoProductsInThatStoreException(Long id) {
        super("No product found in this store: " + id);
    }
}
