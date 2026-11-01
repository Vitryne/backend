package com.vitryne.api.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        @Schema(example = "cliente@email.com")
        @NotBlank @Email @Size(max = 255)
        String email,

        @Schema(
                example = "SenhaTeste123!",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank @Size(max = 72)
        String password
) {
    @Override
    public String toString() {
        return "LoginRequestDTO[REDACTED]";
    }
}
