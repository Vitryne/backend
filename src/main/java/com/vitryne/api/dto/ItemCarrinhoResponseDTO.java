package com.vitryne.api.dto;

import lombok.Builder;

@Builder
public record ItemCarrinhoResponseDTO(
        Long id,
        Long estoqueId,
        Long produtoId,
        String nomeProduto,
        String fotoUrl,
        String tamanho,
        Integer quantidade,
        Double precoUnitario,
        Double subtotal
) {}
