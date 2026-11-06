package com.vitryne.api.order.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long orderId) {
        super("Pedido não encontrado: " + orderId);
    }
}
