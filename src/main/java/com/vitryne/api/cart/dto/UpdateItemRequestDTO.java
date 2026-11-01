package com.vitryne.api.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateItemRequestDTO(
        @NotNull
        @Positive
        Integer quantity
) {
}
