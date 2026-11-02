package com.vitryne.api.shared.exception;

public class InvalidPromotionalPriceException extends RuntimeException {
    public InvalidPromotionalPriceException() {
        super("O preco promocional deve ser inferior ao preco padrao");
    }
}
