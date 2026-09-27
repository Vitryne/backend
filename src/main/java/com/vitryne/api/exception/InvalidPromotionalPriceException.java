package com.vitryne.api.exception;

public class InvalidPromotionalPriceException extends RuntimeException {
    public InvalidPromotionalPriceException() {
        super("O preco promocional deve ser inferior ao preco padrao");
    }
}
