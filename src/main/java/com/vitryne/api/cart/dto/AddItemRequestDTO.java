package com.vitryne.api.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddItemRequestDTO(
        @NotNull @Positive Long stockId,
        @NotNull @Positive Integer quantity
) {}
