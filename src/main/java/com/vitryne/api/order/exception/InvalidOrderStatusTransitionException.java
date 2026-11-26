package com.vitryne.api.order.exception;

import com.vitryne.api.order.enums.OrderStatus;

public class InvalidOrderStatusTransitionException extends RuntimeException {
    public InvalidOrderStatusTransitionException(OrderStatus current, OrderStatus requested) {
        super(current.isFinal()
                ? "O pedido está " + current + " e não pode mais ser alterado"
                : "Não é possível alterar o pedido de " + current + " para " + requested
                        + "; a próxima etapa é " + current.next());
    }

    public InvalidOrderStatusTransitionException(String message) {
        super(message);
    }
}
