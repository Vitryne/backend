package com.vitryne.api.shared.dto;

import com.vitryne.api.shared.enums.UserRole;

import java.time.Instant;

public record RegistrationResponseDTO(
        Long userId,
        Long clientId,
        Long storeId,
        String email,
        UserRole role,
        Instant createdAt
) {}