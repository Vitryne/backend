package com.vitryne.api.order.dto;

import com.vitryne.api.order.service.CheckoutEvaluator;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutReviewResponseDTO(
        @Schema(
                description = """
                        Identifica os valores revisados. Deve ser enviado no checkout; se algo mudar
                        depois da revisão, o checkout é recusado com 409. Nulo quando canConfirm é false.
                        """,
                nullable = true
        )
        String reviewToken,

        @Schema(description = "Falso quando algum problema bloqueia a confirmação")
        boolean canConfirm,

        Long storeId,
        String storeName,

        @Schema(nullable = true, description = "Endereço de entrega; nulo quando inválido")
        DeliveryAddressDTO deliveryAddress,

        List<CheckoutReviewItemDTO> items,
        BigDecimal itemsSubtotal,

        @Schema(description = "Frete. Zero no MVP.", example = "0.00")
        BigDecimal shippingAmount,

        BigDecimal totalAmount,
        List<CheckoutReviewIssueDTO> issues
) {
    public static CheckoutReviewResponseDTO from(CheckoutEvaluator.Evaluation evaluation) {
        return new CheckoutReviewResponseDTO(
                evaluation.reviewToken(),
                evaluation.canConfirm(),
                evaluation.store().getId(),
                evaluation.store().getName(),
                DeliveryAddressDTO.from(evaluation.deliveryAddress()),
                evaluation.lines().stream().map(CheckoutReviewItemDTO::from).toList(),
                evaluation.itemsSubtotal(),
                evaluation.shippingAmount(),
                evaluation.totalAmount(),
                evaluation.issues().stream().map(CheckoutReviewIssueDTO::from).toList()
        );
    }
}
