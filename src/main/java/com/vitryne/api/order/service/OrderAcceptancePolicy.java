package com.vitryne.api.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Prazo para a loja aceitar um pedido novo. Configurável por
 * {@code vitryne.orders.acceptance-timeout} (padrão 30 minutos).
 */
@Component
public class OrderAcceptancePolicy {

    private final Duration timeout;

    public OrderAcceptancePolicy(@Value("${vitryne.orders.acceptance-timeout:30m}") Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("vitryne.orders.acceptance-timeout deve ser positivo");
        }
        this.timeout = timeout;
    }

    public Instant deadlineFrom(Instant placedAt) {
        return placedAt.plus(timeout);
    }
}
