package com.vitryne.api.order.enums;

public enum ReviewIssueType {
    PRICE_CHANGED(false),
    INSUFFICIENT_QUANTITY(true),
    OUT_OF_STOCK(true),
    PRODUCT_INACTIVE(true),
    MULTIPLE_STORES(true),
    INVALID_DELIVERY_ADDRESS(true);

    private final boolean blocking;

    ReviewIssueType(boolean blocking) {
        this.blocking = blocking;
    }

    /** Indica se o problema impede a confirmação (alteração de preço apenas informa). */
    public boolean isBlocking() {
        return blocking;
    }
}
