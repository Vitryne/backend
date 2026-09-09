package com.vitryne.api.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ProductResponseDTO(
        Long id,
        String name,
        String description,
        Double price,
        Double promotionalPrice,
        Double finalPrice,
        String type,
        String color,
        Double rating,
        String status,
        List<String> photoUrls,
        List<AvailableSizeDTO> availableSizes
) {
}
