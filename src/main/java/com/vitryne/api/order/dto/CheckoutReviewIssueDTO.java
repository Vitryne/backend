package com.vitryne.api.order.dto;

import com.vitryne.api.order.enums.ReviewIssueType;
import com.vitryne.api.order.service.CheckoutEvaluator;
import io.swagger.v3.oas.annotations.media.Schema;

public record CheckoutReviewIssueDTO(
        ReviewIssueType type,

        @Schema(description = "Indica se o problema impede a confirmação", example = "false")
        boolean blocking,

        @Schema(description = "Item do carrinho afetado; nulo para problemas gerais", nullable = true)
        Long cartItemId,

        String message
) {
    public static CheckoutReviewIssueDTO from(CheckoutEvaluator.Issue issue) {
        return new CheckoutReviewIssueDTO(
                issue.type(),
                issue.type().isBlocking(),
                issue.cartItemId(),
                issue.message()
        );
    }
}
