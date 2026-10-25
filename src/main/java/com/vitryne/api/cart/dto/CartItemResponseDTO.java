package com.vitryne.api.cart.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CartItemResponseDTO(
        Long id,
        Long stockId,
        Long productId,
        String productName,
        String photoUrl,
        String size,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
