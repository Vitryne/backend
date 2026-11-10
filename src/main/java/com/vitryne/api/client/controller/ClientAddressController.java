package com.vitryne.api.client.controller;

import com.vitryne.api.auth.dto.AddressRequestDTO;
import com.vitryne.api.client.dto.ClientAddressResponseDTO;
import com.vitryne.api.client.service.ClientAddressService;
import com.vitryne.api.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/client/addresses")
@PreAuthorize("hasRole('CONSUMER')")
@Tag(
        name = "Endereços do cliente",
        description = """
                Endereços de entrega do cliente autenticado. O endereço selecionado é usado
                na navegação e como endereço de entrega na revisão e no checkout.
                """
)
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "O usuário autenticado não possui o papel CONSUMER",
                content = @Content)
})
public class ClientAddressController {

    private static final String ADDRESS_EXAMPLE = """
            {
              "zipCode": "87000-000",
              "street": "Rua de Teste",
              "number": "123A",
              "neighborhood": "Centro",
              "complement": "Apartamento 10",
              "city": "Maringá",
              "state": "PR"
            }
            """;

    private final ClientAddressService clientAddressService;

    @GetMapping
    @Operation(
            operationId = "listMyAddresses",
            summary = "Listar meus endereços",
            description = """
                    Retorna os endereços do cliente, do mais antigo para o mais novo.
                    O primeiro é o informado no cadastro. selected indica o endereço de entrega atual.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Endereços do cliente", useReturnTypeSchema = true)
    public ResponseEntity<List<ClientAddressResponseDTO>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(clientAddressService.list(AuthenticatedUser.id(jwt)));
    }

    @GetMapping("/{addressId}")
    @Operation(operationId = "getMyAddress", summary = "Consultar um endereço meu")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereço encontrado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado para este cliente",
                    content = @Content)
    })
    public ResponseEntity<ClientAddressResponseDTO> find(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do endereço", example = "1") @PathVariable("addressId") Long addressId
    ) {
        return ResponseEntity.ok(clientAddressService.find(AuthenticatedUser.id(jwt), addressId));
    }

    @PostMapping
    @Operation(
            operationId = "createMyAddress",
            summary = "Cadastrar endereço",
            description = """
                    Usa as mesmas regras do endereço do cadastro (CEP com 8 dígitos, UF válida).
                    O novo endereço não muda o selecionado; use POST /client/addresses/{id}/select.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AddressRequestDTO.class),
                            examples = @ExampleObject(value = ADDRESS_EXAMPLE)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Endereço cadastrado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos", content = @Content)
    })
    public ResponseEntity<ClientAddressResponseDTO> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddressRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientAddressService.create(AuthenticatedUser.id(jwt), request));
    }

    @PutMapping("/{addressId}")
    @Operation(
            operationId = "updateMyAddress",
            summary = "Editar endereço",
            description = """
                    Substitui os dados do endereço. Pedidos já feitos não mudam, pois guardam
                    uma cópia do endereço de entrega.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AddressRequestDTO.class),
                            examples = @ExampleObject(value = ADDRESS_EXAMPLE)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereço atualizado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado para este cliente",
                    content = @Content)
    })
    public ResponseEntity<ClientAddressResponseDTO> update(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do endereço", example = "1") @PathVariable("addressId") Long addressId,
            @Valid @RequestBody AddressRequestDTO request
    ) {
        return ResponseEntity.ok(clientAddressService.update(AuthenticatedUser.id(jwt), addressId, request));
    }

    @PostMapping("/{addressId}/select")
    @Operation(
            operationId = "selectMyAddress",
            summary = "Selecionar endereço de entrega",
            description = """
                    Define o endereço usado na navegação e como entrega na revisão e no checkout.
                    Ao trocar no checkout, refaça GET /orders/checkout/review: um reviewToken
                    obtido com outro endereço é recusado no checkout.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereço selecionado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado para este cliente",
                    content = @Content)
    })
    public ResponseEntity<ClientAddressResponseDTO> select(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do endereço", example = "1") @PathVariable("addressId") Long addressId
    ) {
        return ResponseEntity.ok(clientAddressService.select(AuthenticatedUser.id(jwt), addressId));
    }
}
