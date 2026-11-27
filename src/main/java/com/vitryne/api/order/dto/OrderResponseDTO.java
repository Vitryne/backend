package com.vitryne.api.order.dto;

import com.vitryne.api.order.entity.Order;
import com.vitryne.api.order.enums.OrderStatus;
import com.vitryne.api.payment.dto.PaymentResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponseDTO(
        Long id,
        Long userId,
        Long storeId,
        String storeName,
        OrderStatus status,

        @Schema(description = """
                Horário-limite para a loja aceitar o pedido; presente só em AWAITING_ACCEPTANCE
                (null nos demais status). Vencido o prazo, o pedido é cancelado automaticamente
                (canceledBy SYSTEM, motivo ACCEPTANCE_TIMEOUT). Use para o contador de aceite.
                """, example = "2026-11-27T18:30:00Z")
        Instant acceptanceDeadline,

        BigDecimal itemsSubtotal,
        BigDecimal shippingAmount,
        BigDecimal totalAmount,
        DeliveryAddressDTO deliveryAddress,
        PaymentResponseDTO payment,
        List<OrderItemResponseDTO> items,
        OrderCancellationDTO cancellation,
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
                order.getStatus() == OrderStatus.AWAITING_ACCEPTANCE ? order.getAcceptanceDeadline() : null,
                order.getTotalAmount().subtract(order.getShippingAmount()),
                order.getShippingAmount(),
                order.getTotalAmount(),
                DeliveryAddressDTO.from(order.getDeliveryAddress()),
                PaymentResponseDTO.from(order.getPayment()),
                order.getItems().stream().map(OrderItemResponseDTO::from).toList(),
                OrderCancellationDTO.from(order),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
