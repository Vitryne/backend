package com.vitryne.api.auth.dto.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vitryne.api.auth.dto.AddressRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record RegisterStoreRequestDTO(
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

        @NotBlank @Size(max = 255)
        String name,

        @NotBlank @Size(max = 20)
        String cnpj,

        @NotBlank @Size(max = 25)
        String phone,

        @NotBlank @Size(max = 255)
        String legalName,

        @Size(max = 2000)
        String description,

        @NotNull @Valid
        AddressRequestDTO address,

        @NotNull @Valid
        StoreBankAccountRequestDTO bankAccount,

        @NotBlank @Size(max = 100)
        String mainCategory,

        @Size(max = 20)
        List<@NotBlank @Size(max = 50) String> tags,

        @NotNull @Positive @Digits(integer = 4, fraction = 2)
        BigDecimal deliveryRadiusKm,

        @NotNull @Positive @Max(1440)
        Integer preparationTimeMinutes,

        @NotEmpty @Size(max = 28)
        List<@NotNull @Valid OpeningPeriodRequestDTO> openingPeriods,

        @Size(max = 2048)
        String photoUrl,

        @Size(max = 2048)
        String bannerUrl
) {
    @Override
    public String toString() {
        return "RegisterStoreRequestDTO[REDACTED]";
    }
}