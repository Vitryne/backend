package com.vitryne.api.product.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DeleteProductsRequestDTO(
        @NotEmpty(message = "Informe ao menos um produto")
        @Size(max = 100, message = "Informe no máximo 100 produtos por vez")
        List<@NotNull(message = "O id do produto é obrigatório") Long> ids
) {
}
