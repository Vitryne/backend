package com.vitryne.api.dto;

public record AdicionarItemRequestDTO(
        Long estoqueId,
        Integer quantidade,
        Double precoUnitario
) {
}
