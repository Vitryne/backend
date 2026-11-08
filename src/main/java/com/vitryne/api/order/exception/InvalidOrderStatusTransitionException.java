package com.vitryne.api.order.exception;

import com.vitryne.api.order.enums.OrderStatus;

public class InvalidOrderStatusTransitionException extends RuntimeException {
    public InvalidOrderStatusTransitionException(OrderStatus current, OrderStatus requested) {
        super("Não é possível alterar o pedido de " + current + " para " + requested);
    }
}
