package com.vitryne.api.shared.exception;

public class InvalidStartDateException extends RuntimeException {
    public InvalidStartDateException() {
        super("A data de inicio da promocao deve ser anterior a do fim da promocao");
    }
}
