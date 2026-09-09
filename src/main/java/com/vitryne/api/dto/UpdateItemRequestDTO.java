package com.vitryne.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateItemRequestDTO(
        @NotNull
        Integer quantity
) {
}
