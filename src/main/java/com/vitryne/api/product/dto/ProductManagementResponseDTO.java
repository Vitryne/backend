package com.vitryne.api.product.dto;

import com.vitryne.api.shared.dto.AvailableSizeDTO;
import com.vitryne.api.stock.dto.StockManagementResponseDTO;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record ProductManagementResponseDTO (
        Long id,
        String name,
        String description,
        BigDecimal price,
        BigDecimal promotionalPrice,
        BigDecimal finalPrice,
        String type,
        String color,
        Double rating,
        String status,
        Boolean newProduct,
        Boolean onSale,
        List<String> photoUrls,
        Boolean availableProduct,
        List<AvailableSizeDTO> availableSizes,
        Instant promotionalStartDate,
        Instant promotionalEndDate,
        Integer stockWarningThreshold,
        Instant createdAt,
        Instant updatedAt,
        List<StockManagementResponseDTO> stocks
){
}
