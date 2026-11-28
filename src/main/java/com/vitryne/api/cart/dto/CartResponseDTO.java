package com.vitryne.api.cart.dto;

import com.vitryne.api.store.dto.StoreSummaryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record CartResponseDTO(
        Long id,
        Long userId,

        @Schema(description = """
                Loja dos itens do carrinho; o carrinho só aceita produtos de uma loja.
                É null quando o carrinho está vazio. Se o produto a adicionar for de
                outra loja, o app deve avisar o cliente e oferecer esvaziar o carrinho
                (DELETE /cart/items) antes de adicionar.
                """)
        StoreSummaryDTO store,

        @Schema(description = "Soma dos itens, sem frete", example = "119.80")
        BigDecimal totalValueForecast,

        @Schema(description = """
                Frete fixo por pedido (R$ 10,90 no MVP, configurável no servidor).
                Zero quando o carrinho está vazio. A revisão do checkout confirma o valor.
                """, example = "10.90")
        BigDecimal shippingAmount,

        @Schema(description = "Total com frete: totalValueForecast + shippingAmount", example = "130.70")
        BigDecimal totalAmount,
        LocalDateTime updatedAt,
        List<CartItemResponseDTO> items
) {
}
