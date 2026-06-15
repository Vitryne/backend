package com.vitryne.api.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record CarrinhoResponseDTO(
        Long id,
        Long usuarioId,
        Double previsaoValorTotal,
        LocalDateTime atualizadoEm,
        List<ItemCarrinhoResponseDTO> itens
) {
}
