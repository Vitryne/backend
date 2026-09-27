package com.vitryne.api.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        LocalDateTime promotionalStartDate,
        LocalDateTime promotionalEndDate,
        Integer stockWarningThreshold,
        LocalDateTime createdAt
){
}
