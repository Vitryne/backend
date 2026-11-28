package com.vitryne.api.store.controller;

import com.vitryne.api.shared.security.AuthenticatedUser;
import com.vitryne.api.store.dto.StoreProfileResponseDTO;
import com.vitryne.api.store.service.StoreProfileService;
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
@PreAuthorize("hasRole('STORE')")
@Tag(name = "Perfil da loja", description = "Dados da loja do lojista autenticado")
@SecurityRequirement(name = "bearerAuth")
public class StoreProfileController {

    private final StoreProfileService storeProfileService;

    @GetMapping("/store/me")
    @Operation(
            operationId = "getMyStore",
            summary = "Consultar minha loja",
            description = """
                    Retorna a loja do lojista do token: id, nome, foto, endereço, categoria
                    principal, raio de entrega e tempo de preparo. Não recebe id, então só
                    alcança a loja do próprio lojista. Não retorna CNPJ, razão social nem
                    dados bancários.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loja do lojista", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "O usuário autenticado não possui o papel STORE",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Lojista do token sem loja cadastrada",
                    content = @Content)
    })
    public ResponseEntity<StoreProfileResponseDTO> me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(storeProfileService.me(AuthenticatedUser.id(jwt)));
    }
}
