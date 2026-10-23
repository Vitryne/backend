package com.vitryne.api.shared.dto;

public record AvailableSizeDTO(
        Long stockId,
        String size,
        Integer quantity,
        Boolean available
) {
}
