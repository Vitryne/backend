package com.vitryne.api.exception;

public class NullPromotionArgumentException extends RuntimeException {
    public NullPromotionArgumentException() {
        super("todos argumentos devem ser preenchidos para registrar uma promoção");
    }
}
