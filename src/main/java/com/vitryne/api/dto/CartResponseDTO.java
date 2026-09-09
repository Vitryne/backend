package com.vitryne.api.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record CartResponseDTO(
        Long id,
        Long userId,
        Double totalValueForecast,
        LocalDateTime updatedAt,
        List<CartItemResponseDTO> items
) {
}
