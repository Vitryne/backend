package com.vitryne.api.cart.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record CartResponseDTO(
        Long id,
        Long userId,
        BigDecimal totalValueForecast,
        LocalDateTime updatedAt,
        List<CartItemResponseDTO> items
) {
}
