package com.vitryne.api.order.dto;

import com.vitryne.api.order.enums.ReviewItemStatus;
import com.vitryne.api.order.service.CheckoutEvaluator;
import com.vitryne.api.product.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutReviewItemDTO(
        Long cartItemId,
        Long stockId,
        Long productId,
        String productName,
        String photoUrl,
        String size,
        Integer quantity,

        @Schema(description = "Quantidade disponível em estoque no momento da revisão")
        Integer availableQuantity,

        @Schema(description = "Preço registrado quando o item foi adicionado ao carrinho")
        BigDecimal cartUnitPrice,

        @Schema(description = "Preço atual, usado no cálculo do total")
        BigDecimal currentUnitPrice,

        BigDecimal subtotal,
        ReviewItemStatus status
) {
    public static CheckoutReviewItemDTO from(CheckoutEvaluator.Line line) {
        Product product = line.stock().getProduct();
        List<String> photos = product.getPhotoUrls();

        return new CheckoutReviewItemDTO(
                line.cartItem().getId(),
                line.stock().getId(),
                product.getId(),
                product.getName(),
                photos == null || photos.isEmpty() ? null : photos.getFirst(),
                line.stock().getSize(),
                line.quantity(),
                line.stock().getQuantity(),
                line.cartUnitPrice(),
                line.currentUnitPrice(),
                line.subtotal(),
                line.status()
        );
    }
}
