package com.vitryne.api.exception;

public class EstoqueNaoEncontradoException extends RuntimeException {
    public EstoqueNaoEncontradoException(Long id) {
        super("Estoque não encontrado " + id);
    }
}
