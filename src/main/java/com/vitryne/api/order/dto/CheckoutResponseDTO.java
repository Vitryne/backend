package com.vitryne.api.order.dto;

import com.vitryne.api.payment.dto.PaymentResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

public record CheckoutResponseDTO(
        PaymentResponseDTO payment,

        @Schema(description = "Pedido criado. Nulo quando o pagamento é recusado.", nullable = true)
        OrderResponseDTO order
) {
}
