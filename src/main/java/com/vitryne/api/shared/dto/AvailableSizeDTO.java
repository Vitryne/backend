package com.vitryne.api.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AvailableSizeDTO(
        @Schema(
                description = "ID da variação de estoque, usado para adicionar ao carrinho",
                example = "15"
        )
        Long id,

        @Schema(
                description = "Tamanho da variação",
                example = "M"
        )
        String size,

        @Schema(
                description = "Quantidade disponível no momento da consulta",
                example = "5"
        )
        Integer quantity,

        @Schema(
                description = "Indica se a variação possui quantidade maior que zero",
                example = "true"
        )
        boolean available
) {
}