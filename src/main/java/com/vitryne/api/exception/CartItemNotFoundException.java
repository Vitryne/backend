package com.vitryne.api.exception;

public class CartItemNotFoundException extends RuntimeException {
    public CartItemNotFoundException(Long itemId) {
        super("Item not found in cart: " + itemId);
    }
}
