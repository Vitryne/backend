package com.vitryne.api.order.dto;

import com.vitryne.api.order.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponseDTO(
        Long id,
        Long stockId,
        String productName,
        String size,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
    public static OrderItemResponseDTO from(OrderItem item) {
        return new OrderItemResponseDTO(
                item.getId(),
                item.getStock().getId(),
                item.getProductName(),
                item.getSize(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}
