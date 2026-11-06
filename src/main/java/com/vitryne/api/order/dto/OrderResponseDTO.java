package com.vitryne.api.order.dto;

import com.vitryne.api.order.entity.Order;
import com.vitryne.api.order.enums.OrderStatus;
import com.vitryne.api.payment.dto.PaymentResponseDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponseDTO(
        Long id,
        Long userId,
        Long storeId,
        String storeName,
        OrderStatus status,
        BigDecimal totalAmount,
        PaymentResponseDTO payment,
        List<OrderItemResponseDTO> items,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponseDTO from(Order order) {
        return new OrderResponseDTO(
                order.getId(),
                order.getUserId(),
                order.getStore().getId(),
                order.getStore().getName(),
                order.getStatus(),
                order.getTotalAmount(),
                PaymentResponseDTO.from(order.getPayment()),
                order.getItems().stream().map(OrderItemResponseDTO::from).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
