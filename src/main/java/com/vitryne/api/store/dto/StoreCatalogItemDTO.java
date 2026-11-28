package com.vitryne.api.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Loja na vitrine pública do app. Não traz CNPJ, razão social, telefone,
 * endereço, dados bancários nem o dono da loja.
 */
@Schema(description = "Loja ativa exibida na Home do app")
public record StoreCatalogItemDTO(
        @Schema(description = "ID da loja", example = "3")
        Long id,

        @Schema(description = "Nome da loja", example = "Loja Centro")
        String name,

        @Schema(description = "Descrição da loja; null se não cadastrada", example = "Moda feminina casual")
        String description,

        @Schema(description = "Categoria principal; null se não cadastrada", example = "Moda feminina")
        String mainCategory,

        @Schema(description = "Raio de entrega em km; null se não cadastrado", example = "10.00")
        BigDecimal deliveryRadiusKm,

        @Schema(description = "Tempo de preparo em minutos; null se não cadastrado", example = "30")
        Integer preparationTimeMinutes,

        @Schema(description = "URL da foto (logo) da loja; null se não cadastrada")
        String photoUrl,

        @Schema(description = "URL do banner da loja; null se não cadastrado")
        String bannerUrl,

        @Schema(description = "Tags da loja", example = "[\"casual\", \"vestidos\"]")
        List<String> tags,

        @Schema(description = "Períodos de funcionamento, ordenados por dia e horário")
        List<OpeningPeriodDTO> openingPeriods,

        @JsonProperty("isOpen")
        @Schema(
                name = "isOpen",
                description = """
                        Se a loja está aberta agora, pelos períodos de funcionamento
                        no horário de Brasília. Sem períodos cadastrados, false.
                        """,
                example = "true"
        )
        boolean isOpen
) {
}
