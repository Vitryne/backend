package com.vitryne.api.product.dto;

import com.vitryne.api.product.enums.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequestDTO(
        @NotNull ProductStatus status
){
}
