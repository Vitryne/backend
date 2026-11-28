package com.vitryne.api.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Frete do MVP: valor fixo por pedido (uma loja por carrinho), configurado
 * por vitryne.orders.shipping-fee. Única fonte do frete para carrinho,
 * revisão e checkout.
 */
@Component
public class ShippingPolicy {

    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

    private final BigDecimal fixedFee;

    public ShippingPolicy(@Value("${vitryne.orders.shipping-fee:10.90}") BigDecimal fixedFee) {
        if (fixedFee == null || fixedFee.signum() < 0) {
            throw new IllegalStateException("vitryne.orders.shipping-fee (SHIPPING_FEE) não pode ser negativo");
        }
        try {
            this.fixedFee = fixedFee.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalStateException(
                    "vitryne.orders.shipping-fee (SHIPPING_FEE) deve ter no máximo duas casas decimais"
            );
        }
    }

    /** Frete de um pedido com itens; carrinho vazio não tem frete. */
    public BigDecimal shippingFor(boolean hasItems) {
        return hasItems ? fixedFee : ZERO_MONEY;
    }
}
