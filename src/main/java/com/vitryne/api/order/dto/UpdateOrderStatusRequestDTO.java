package com.vitryne.api.order.dto;

import com.vitryne.api.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequestDTO(
        @Schema(description = "Novo status do pedido", example = "PREPARING")
        @NotNull(message = "O status é obrigatório")
        OrderStatus status
) {
}
