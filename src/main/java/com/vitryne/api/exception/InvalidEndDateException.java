package com.vitryne.api.exception;

public class InvalidEndDateException extends RuntimeException {
  public InvalidEndDateException() {
    super("A data de fim da promocao deve ser uma data futura");
  }
}
