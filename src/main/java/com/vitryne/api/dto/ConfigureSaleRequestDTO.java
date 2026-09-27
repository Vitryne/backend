package com.vitryne.api.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConfigureSaleRequestDTO(
        @NotNull
        BigDecimal promoPrice,
        @NotNull
        LocalDateTime startDate,
        @NotNull
        LocalDateTime endDate
) {
}
