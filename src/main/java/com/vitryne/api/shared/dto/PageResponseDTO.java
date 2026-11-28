package com.vitryne.api.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados com formato estável. Evita serializar o Page do Spring,
 * cujo JSON muda entre versões.
 */
@Schema(description = "Página de resultados")
public record PageResponseDTO<T>(
        @Schema(description = "Itens da página")
        List<T> content,

        @Schema(description = "Número da página, começando em 0", example = "0")
        int page,

        @Schema(description = "Tamanho da página pedido", example = "20")
        int size,

        @Schema(description = "Total de itens em todas as páginas", example = "42")
        long totalElements,

        @Schema(description = "Total de páginas", example = "3")
        int totalPages
) {
    public static <E, T> PageResponseDTO<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponseDTO<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
