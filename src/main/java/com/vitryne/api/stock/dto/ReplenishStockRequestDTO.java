package com.vitryne.api.stock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReplenishStockRequestDTO(
        @NotNull
        @Positive
        Integer quantity
) {
}