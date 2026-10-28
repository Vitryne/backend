package com.vitryne.api.product.exception;

public class ProductDontBelongToStoreException extends RuntimeException {
    public ProductDontBelongToStoreException(Long id, Long ownerId) {
        super("o produto de id " +  id + " nao pertence ao lojista de id " + ownerId);
    }
}
