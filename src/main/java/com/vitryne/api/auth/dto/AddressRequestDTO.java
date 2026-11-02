package com.vitryne.api.auth.dto;

import jakarta.validation.constraints.*;

public record AddressRequestDTO(
        @NotBlank @Size(max = 10)
        String zipCode,

        @NotBlank @Size(max = 200)
        String street,

        @NotBlank @Size(max = 20)
        String number,

        @NotBlank @Size(max = 100)
        String neighborhood,

        @Size(max = 150)
        String complement,

        @NotBlank @Size(max = 100)
        String city,

        @NotBlank @Pattern(regexp = "[a-zA-Z]{2}")
        String state
) {}