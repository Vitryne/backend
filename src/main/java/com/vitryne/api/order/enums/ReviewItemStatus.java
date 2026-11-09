package com.vitryne.api.order.enums;

/** Situação de um item na revisão. Quando há mais de um problema, prevalece o mais grave. */
public enum ReviewItemStatus {
    OK,
    PRICE_CHANGED,
    INSUFFICIENT_QUANTITY,
    OUT_OF_STOCK,
    PRODUCT_INACTIVE
}
