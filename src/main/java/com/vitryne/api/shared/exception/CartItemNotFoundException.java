package com.vitryne.api.shared.exception;

public class CartItemNotFoundException extends RuntimeException {
    public CartItemNotFoundException(Long itemId) {
        super("Item " + itemId + " não encontrado no seu carrinho");
    }
}
