package com.vitryne.api.shared.exception;

public class CartNotFoundException extends RuntimeException {
    public CartNotFoundException(Long userId) {
        super("Carrinho não encontrado");
    }
}
