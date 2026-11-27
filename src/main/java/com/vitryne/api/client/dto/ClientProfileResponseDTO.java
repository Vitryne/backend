package com.vitryne.api.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Perfil do cliente autenticado")
public record ClientProfileResponseDTO(
        @Schema(description = "Nome do cliente", example = "Ana Souza")
        String name,

        @Schema(description = "E-mail de login", example = "ana@exemplo.com")
        String email,

        @Schema(description = """
                Endereço de entrega selecionado; null quando o cliente não tem endereço selecionado.
                Para listar os demais, use GET /client/addresses.
                """)
        ClientAddressResponseDTO selectedAddress
) {
}
