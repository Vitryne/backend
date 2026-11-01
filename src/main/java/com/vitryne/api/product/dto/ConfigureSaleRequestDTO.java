package com.vitryne.api.product.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record ConfigureSaleRequestDTO(
        @NotNull
        BigDecimal promoPrice,
        @NotNull
        Instant startDate,
        @NotNull
        Instant endDate
) {
}
