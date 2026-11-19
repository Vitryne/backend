package com.vitryne.api.shared.exception;

public class UnavailableQuantityException extends RuntimeException {
    public UnavailableQuantityException(String size, Integer available, Integer requested) {
        super(String.format(
                "Quantidade solicitada (%d) maior que o estoque disponível (%d) para o tamanho %s",
                requested, available, size));
    }
}
