package com.vitryne.api.cart.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateItemRequestDTO(
        @NotNull
        Integer quantity
) {
}
