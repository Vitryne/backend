package com.vitryne.api.dto;

public record AvailableSizeDTO(
        Long stockId,
        String size,
        Integer quantity,
        Boolean available
) {
}
