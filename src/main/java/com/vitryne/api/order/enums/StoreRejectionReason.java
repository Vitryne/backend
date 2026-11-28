package com.vitryne.api.order.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Justificativa da recusa pela loja:
        ITEM_UNAVAILABLE (item indisponível), STORE_CLOSED (loja fechada),
        OUT_OF_DELIVERY_AREA (endereço fora da área de entrega),
        HIGH_DEMAND (alta demanda, sem capacidade de atender)
        """)
public enum StoreRejectionReason {
    ITEM_UNAVAILABLE,
    STORE_CLOSED,
    OUT_OF_DELIVERY_AREA,
    HIGH_DEMAND
}
