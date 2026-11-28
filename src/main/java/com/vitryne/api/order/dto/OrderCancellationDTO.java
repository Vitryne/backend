package com.vitryne.api.order.dto;

import com.vitryne.api.order.entity.Order;
import com.vitryne.api.order.enums.CanceledBy;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Dados do cancelamento; presente apenas em pedidos CANCELED")
public record OrderCancellationDTO(
        CanceledBy canceledBy,
        @Schema(description = """
                Código do motivo. Com canceledBy STORE, um valor de StoreRejectionReason;
                com CLIENT, um valor de ClientCancellationReason; com SYSTEM,
                ACCEPTANCE_TIMEOUT (não aceito no prazo).
                """, example = "ITEM_UNAVAILABLE")
        String reason,
        Instant canceledAt
) {
    public static OrderCancellationDTO from(Order order) {
        if (order.getCanceledBy() == null) {
            return null;
        }

        return new OrderCancellationDTO(
                order.getCanceledBy(),
                order.getCancellationReason(),
                order.getCanceledAt()
        );
    }
}
