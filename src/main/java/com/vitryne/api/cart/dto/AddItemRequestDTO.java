package com.vitryne.api.cart.dto;

import jakarta.validation.constraints.NotNull;

public record AddItemRequestDTO(
        @NotNull
        Long stockId,
        @NotNull
        Integer quantity
) {
}
