package com.vitryne.api.order.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Quem cancelou o pedido: STORE (recusa da loja), CLIENT ou
        SYSTEM (cancelamento automático por falta de aceite no prazo)
        """)
public enum CanceledBy {
    CLIENT,
    STORE,
    SYSTEM
}
