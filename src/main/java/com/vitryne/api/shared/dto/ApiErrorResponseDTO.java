package com.vitryne.api.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Erro da operação de cadastro")
public record ApiErrorResponseDTO(

        @Schema(
                description = "Descrição geral do erro",
                example = "Dados de cadastro inválidos"
        )
        String message,

        @Schema(
                description = """
            Erros por campo. Presente nas falhas de Bean Validation.
            Pode estar ausente nos erros de negócio e conflitos.
            """
        )
        Map<String, String> fields
) {}