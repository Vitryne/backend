package com.vitryne.api.dto;

public record TamanhoDisponivelDTO(
        String tamanho,
        Integer quantidade,
        Boolean disponivel
) {
}
