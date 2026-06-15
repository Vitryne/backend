package com.vitryne.api.exception;

public class CarrinhoNaoEncontradoException extends RuntimeException {
    public CarrinhoNaoEncontradoException(Long usuarioId) {
        super("Carrinho não encontrado para o usuário: " + usuarioId);
    }
}
