package com.vitryne.api.order.dto;

import com.vitryne.api.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequestDTO(
        @Schema(description = "Próxima etapa do pedido; CONFIRMED aceita um pedido AWAITING_ACCEPTANCE", example = "CONFIRMED")
        @NotNull(message = "O status é obrigatório")
        OrderStatus status
) {
}
