package com.vitryne.api.exception;

public class QuantidadeIndisponivelException extends RuntimeException {
    public QuantidadeIndisponivelException(String tamanho, Integer disponivel, Integer solicitado) {
        super(String.format(
                "Quantidade solicitada (%d) excede o estoque disponível (%d) para o tamanho %s",
                solicitado, disponivel, tamanho));
    }
}