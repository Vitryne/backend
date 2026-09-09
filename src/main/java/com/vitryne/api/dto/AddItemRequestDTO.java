package com.vitryne.api.dto;

import jakarta.validation.constraints.NotNull;

public record AddItemRequestDTO(
        @NotNull
        Long stockId,
        @NotNull
        Integer quantity
) {
}
