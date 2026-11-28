package com.vitryne.api.client.controller;

import com.vitryne.api.client.dto.ClientProfileResponseDTO;
import com.vitryne.api.client.service.ClientProfileService;
import com.vitryne.api.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@PreAuthorize("hasRole('CONSUMER')")
@Tag(name = "Perfil do cliente", description = "Dados do cliente autenticado")
@SecurityRequirement(name = "bearerAuth")
public class ClientProfileController {

    private final ClientProfileService clientProfileService;

    @GetMapping("/client/me")
    @Operation(
            operationId = "getMyClientProfile",
            summary = "Consultar meu perfil",
            description = """
                    Retorna nome, e-mail e endereço de entrega selecionado do cliente do token.
                    Use após o login para carregar o app. selectedAddress é null quando
                    não há endereço selecionado. Não retorna senha nem CPF.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil do cliente", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "O usuário autenticado não possui o papel CONSUMER",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário do token sem cadastro de cliente",
                    content = @Content)
    })
    public ResponseEntity<ClientProfileResponseDTO> me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(clientProfileService.me(AuthenticatedUser.id(jwt)));
    }
}
