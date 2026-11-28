package com.vitryne.api.order.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ciclo de vida do pedido.
 *
 * <pre>
 * AWAITING_ACCEPTANCE → CONFIRMED → PREPARING → READY_FOR_PICKUP → OUT_FOR_DELIVERY → DELIVERED
 *         ↘ CANCELED (recusa da loja ou cancelamento pelo cliente)
 * </pre>
 *
 * A loja avança uma etapa por vez; DELIVERED e CANCELED são finais.
 */
@Schema(description = """
        Status do pedido. Fluxo: AWAITING_ACCEPTANCE → CONFIRMED → PREPARING → READY_FOR_PICKUP
        → OUT_FOR_DELIVERY → DELIVERED. CANCELED é a saída por recusa da loja ou cancelamento
        pelo cliente. DELIVERED e CANCELED são finais.
        """)
public enum OrderStatus {
    /** Pagamento aprovado, aguardando a loja aceitar ou recusar. */
    AWAITING_ACCEPTANCE,
    /** Loja aceitou o pedido. */
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELED;

    /** Próxima etapa do fluxo; nulo para os status finais. */
    public OrderStatus next() {
        return switch (this) {
            case AWAITING_ACCEPTANCE -> CONFIRMED;
            case CONFIRMED -> PREPARING;
            case PREPARING -> READY_FOR_PICKUP;
            case READY_FOR_PICKUP -> OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> DELIVERED;
            case DELIVERED, CANCELED -> null;
        };
    }

    public boolean isFinal() {
        return this == DELIVERED || this == CANCELED;
    }

    /** A loja só recusa antes de aceitar. */
    public boolean storeCanReject() {
        return this == AWAITING_ACCEPTANCE;
    }

    /** O cliente cancela até o pedido ficar pronto para coleta. */
    public boolean clientCanCancel() {
        return this == AWAITING_ACCEPTANCE || this == CONFIRMED || this == PREPARING;
    }
}
