package com.vitryne.api.dto;

import lombok.Builder;

@Builder
public record ItemCarrinhoResponseDTO(
        Long id,
        Long estoqueId,
        Integer quantidade,
        Double precoUnitario,
        Double subtotal
) {
}
