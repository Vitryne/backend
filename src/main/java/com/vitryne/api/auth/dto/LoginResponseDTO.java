package com.vitryne.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponseDTO(
        @Schema(description = "JWT para o header Authorization: Bearer <token>")
        String accessToken,

        @Schema(example = "Bearer")
        String tokenType,

        @Schema(description = "Validade do token em segundos", example = "3600")
        long expiresIn
) {}
