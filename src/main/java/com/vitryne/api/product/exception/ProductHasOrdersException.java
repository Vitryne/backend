package com.vitryne.api.product.exception;

import java.util.List;

public class ProductHasOrdersException extends RuntimeException {
    public ProductHasOrdersException(List<Long> ids) {
        super("Os produtos " + ids + " possuem pedidos e não podem ser excluídos. "
                + "Desative-os para removê-los da vitrine.");
    }
}
