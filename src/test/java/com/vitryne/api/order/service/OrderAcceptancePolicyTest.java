package com.vitryne.api.order.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderAcceptancePolicyTest {

    @Test
    void prazoSomaOTempoConfiguradoAoMomentoDoPedido() {
        Instant placedAt = Instant.parse("2026-11-27T18:00:00Z");

        assertThat(new OrderAcceptancePolicy(Duration.ofMinutes(30)).deadlineFrom(placedAt))
                .isEqualTo(Instant.parse("2026-11-27T18:30:00Z"));
        assertThat(new OrderAcceptancePolicy(Duration.ofMinutes(10)).deadlineFrom(placedAt))
                .isEqualTo(Instant.parse("2026-11-27T18:10:00Z"));
    }

    @Test
    void prazoPrecisaSerPositivo() {
        assertThatThrownBy(() -> new OrderAcceptancePolicy(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderAcceptancePolicy(Duration.ofMinutes(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
