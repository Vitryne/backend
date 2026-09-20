package com.vitryne.api.exception;

public class NullPromotionArgument extends RuntimeException {
    public NullPromotionArgument() {
        super("todos argumentos devem ser preenchidos para registrar uma promoção");
    }
}
