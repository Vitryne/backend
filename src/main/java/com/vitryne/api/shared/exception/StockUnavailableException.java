package com.vitryne.api.shared.exception;

public class StockUnavailableException extends RuntimeException {
    public StockUnavailableException(String size) {
        super("O tamanho " + size + " está esgotado");
    }
}
