package com.vitryne.api.stock.dto;

import java.time.Instant;

public record StockManagementResponseDTO(
        Long id,
        Long productId,
        String size,
        Integer quantity,
        boolean available,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}