package com.vitryne.api.order.dto;

import com.vitryne.api.payment.enums.PaymentMethod;
import com.vitryne.api.payment.enums.PaymentResult;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequestDTO(
        @Schema(description = "Forma de pagamento", example = "CREDIT_CARD")
        @NotNull(message = "A forma de pagamento é obrigatória")
        PaymentMethod paymentMethod,

        @Schema(
                description = "Resultado desejado para a simulação. Quando omitido, o pagamento é aprovado.",
                example = "APPROVED",
                nullable = true
        )
        PaymentResult simulatedResult
) {
}
