package com.vitryne.api.stock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AdjustStockRequestDTO(
        @NotNull
        @PositiveOrZero
        Integer quantity,

        @NotNull
        @PositiveOrZero
        Long version
) {
}