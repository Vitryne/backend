package com.vitryne.api.store.dto;

import com.vitryne.api.store.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Loja responsável pelo produto ou pelos itens do carrinho")
public record StoreSummaryDTO(
        @Schema(description = "ID da loja", example = "3")
        Long id,

        @Schema(description = "Nome da loja", example = "Loja Centro")
        String name
) {

    public static StoreSummaryDTO from(Store store) {
        return store == null ? null : new StoreSummaryDTO(store.getId(), store.getName());
    }
}
