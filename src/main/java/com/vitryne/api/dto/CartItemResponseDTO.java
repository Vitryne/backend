package com.vitryne.api.dto;

import lombok.Builder;

@Builder
public record CartItemResponseDTO(
        Long id,
        Long stockId,
        Long productId,
        String productName,
        String photoUrl,
        String size,
        Integer quantity,
        Double unitPrice,
        Double subtotal
) {}
