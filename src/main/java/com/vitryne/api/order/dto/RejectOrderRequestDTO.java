package com.vitryne.api.order.dto;

import com.vitryne.api.order.enums.StoreRejectionReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RejectOrderRequestDTO(
        @Schema(description = "Justificativa da recusa", example = "ITEM_UNAVAILABLE")
        @NotNull(message = "A justificativa da recusa é obrigatória")
        StoreRejectionReason reason
) {
}
