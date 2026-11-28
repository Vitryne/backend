package com.vitryne.api.order.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShippingPolicyTest {

    @Test
    void cobraOFreteFixoSoQuandoHaItens() {
        ShippingPolicy policy = new ShippingPolicy(new BigDecimal("10.9"));

        assertThat(policy.shippingFor(true)).isEqualTo(new BigDecimal("10.90"));
        assertThat(policy.shippingFor(false)).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void aceitaFreteZero() {
        assertThat(new ShippingPolicy(BigDecimal.ZERO).shippingFor(true)).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void recusaConfiguracaoNegativaOuComMaisDeDuasCasas() {
        assertThatThrownBy(() -> new ShippingPolicy(new BigDecimal("-1.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não pode ser negativo");
        assertThatThrownBy(() -> new ShippingPolicy(new BigDecimal("10.905")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duas casas decimais");
    }
}
