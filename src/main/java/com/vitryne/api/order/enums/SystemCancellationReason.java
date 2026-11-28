package com.vitryne.api.order.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Motivo do cancelamento automático: ACCEPTANCE_TIMEOUT (não aceito no prazo)")
public enum SystemCancellationReason {
    ACCEPTANCE_TIMEOUT
}
