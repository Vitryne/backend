package com.vitryne.api.exception;

public class EstoqueIndisponivelException extends RuntimeException {
    public EstoqueIndisponivelException(String tamanho) {
        super("Estoque indisponível para o tamanho: " + tamanho);
    }
}