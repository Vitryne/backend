package com.vitryne.api.exception;

public class EstoqueInsuficienteException extends RuntimeException {
    public EstoqueInsuficienteException(String tamanho, Integer disponivel, Integer solicitado) {
        super(String.format(
                "Estoque insuficiente para o tamanho %s: disponível %d, solicitado %d",
                tamanho, disponivel, solicitado
        ));
    }
}
