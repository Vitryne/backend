package com.vitryne.api.product.dto;

import jakarta.validation.constraints.*;

public record CreateStockRequestDTO(
        @NotBlank
        @Size(max = 10)
        String size,

        @NotNull
        @PositiveOrZero
        Integer quantity
) {}