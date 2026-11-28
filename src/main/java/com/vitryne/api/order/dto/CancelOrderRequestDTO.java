package com.vitryne.api.order.dto;

import com.vitryne.api.order.enums.ClientCancellationReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CancelOrderRequestDTO(
        @Schema(description = "Motivo do cancelamento", example = "CHANGED_MIND")
        @NotNull(message = "O motivo do cancelamento é obrigatório")
        ClientCancellationReason reason
) {
}
