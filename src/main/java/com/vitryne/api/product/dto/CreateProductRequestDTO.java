package com.vitryne.api.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequestDTO(
        @NotBlank
        String name,

        @NotBlank
        String description,

        @NotNull
        @Positive
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotBlank
        String type,

        String color,

        @PositiveOrZero
        Integer stockWarningThreshold,

        @NotEmpty
        List<@NotBlank String> photoUrls,

        @NotEmpty
        List<@NotNull @Valid CreateStockRequestDTO> stocks
) {
}