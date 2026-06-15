package com.vitryne.api.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ProdutoResponseDTO(
        Long id,
        String nome,
        String descricao,
        Double preco,
        Double precoPromocional,
        Double precoFinal,
        String tipo,
        String cor,
        Double avaliacao,
        String status,
        List<String> fotosUrls,
        List<TamanhoDisponivelDTO> tamanhosDisponiveis
) {
}
