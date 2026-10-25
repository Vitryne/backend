package com.vitryne.api.auth.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vitryne.api.auth.dto.AddressRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record RegisterClientRequestDTO(
        @NotBlank @Size(max = 150)
        String name,

        @NotBlank @Size(max = 20)
        String cpf,

        @NotBlank @Email @Size(max = 255)
        String email,

        @Schema(
                description = """
        Senha com pelo menos 8 caracteres e no máximo 72 bytes em UTF-8.
        A confirmação da senha é feita no frontend.
        """,
                example = "SenhaTeste123!",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @NotNull @Valid
        AddressRequestDTO address
) {
    @Override
    public String toString() {
        return "RegisterClientRequestDTO[REDACTED]";
    }
}