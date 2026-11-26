package com.vitryne.api.order.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Motivo do cancelamento pelo cliente:
        CHANGED_MIND (desistiu da compra), ORDERED_BY_MISTAKE (pediu por engano),
        WRONG_ADDRESS (endereço de entrega errado), TAKING_TOO_LONG (demora no atendimento),
        OTHER (outro motivo)
        """)
public enum ClientCancellationReason {
    CHANGED_MIND,
    ORDERED_BY_MISTAKE,
    WRONG_ADDRESS,
    TAKING_TOO_LONG,
    OTHER
}
