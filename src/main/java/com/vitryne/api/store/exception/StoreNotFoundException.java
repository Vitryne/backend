package com.vitryne.api.store.exception;

public class StoreNotFoundException extends RuntimeException {
    public StoreNotFoundException(Long id) {
        super("A loja com o ownerId " + id + " não foi encontrada");
    }
}
