package com.vitryne.api.store.dto;

import com.vitryne.api.shared.dto.AddressResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Loja do lojista autenticado")
public record StoreProfileResponseDTO(
        @Schema(description = "ID da loja", example = "3")
        Long id,

        @Schema(description = "Nome da loja", example = "Loja Centro")
        String name,

        @Schema(description = "E-mail de login do lojista", example = "lojista@exemplo.com")
        String email,

        @Schema(description = "URL da foto da loja; null se não cadastrada")
        String photoUrl,

        @Schema(description = "Endereço da loja; null se não cadastrado")
        AddressResponseDTO address,

        @Schema(description = "Categoria principal", example = "Moda feminina")
        String mainCategory,

        @Schema(description = "Raio de entrega em km", example = "5.00")
        BigDecimal deliveryRadiusKm,

        @Schema(description = "Tempo de preparo em minutos", example = "30")
        Integer preparationTimeMinutes
) {
}
