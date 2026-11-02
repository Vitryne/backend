package com.vitryne.api.product.dto;

import com.vitryne.api.shared.dto.AvailableSizeDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@Schema(description = "Produto exibido na vitrine e no detalhe do app")
public class ProductClientResponseDTO {

    @Schema(description = "ID do produto", example = "1")
    private Long id;

    private String name;
    private String description;
    private String type;
    private String color;

    @Schema(
            description = "Avaliação média; null quando não há avaliações",
            example = "4.5"
    )
    private Double rating;

    @Schema(description = "Preço normal", example = "100.00")
    private BigDecimal price;

    @Schema(
            description = """
                    Preço promocional configurado.
                    Só deve ser apresentado como promoção quando onSale for true.
                    """
    )
    private BigDecimal promotionalPrice;

    @Schema(
            description = "Preço vigente no momento da consulta",
            example = "80.00"
    )
    private BigDecimal finalPrice;

    @Schema(description = "Status do produto", example = "ACTIVE")
    private String status;

    @Schema(description = "URLs das fotos; a primeira pode ser usada como capa")
    private List<String> photoUrls;

    @Schema(
            description = """
                    Variações do produto, incluindo tamanhos sem estoque.
                    O id de cada item identifica o estoque.
                    """
    )
    private List<AvailableSizeDTO> availableSizes;

    @Schema(description = "Existe pelo menos uma variação com estoque positivo")
    private boolean availableProduct;

    @Schema(description = "Produto cadastrado nos últimos sete dias")
    private boolean newProduct;

    @Schema(description = "A promoção está vigente no momento da consulta")
    private boolean onSale;
}