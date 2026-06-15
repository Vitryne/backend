package com.vitryne.api.exception;

public class ItemCarrinhoNaoEncontradoException extends RuntimeException {
    public ItemCarrinhoNaoEncontradoException(Long itemId) {
        super("Item não encontrado no carrinho: " + itemId);
    }
}
