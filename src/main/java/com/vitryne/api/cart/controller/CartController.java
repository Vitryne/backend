package com.vitryne.api.cart.controller;

import com.vitryne.api.cart.dto.AddItemRequestDTO;
import com.vitryne.api.cart.dto.CartResponseDTO;
import com.vitryne.api.cart.dto.UpdateItemRequestDTO;
import com.vitryne.api.cart.service.CartService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/cart")
@PreAuthorize("hasRole('CONSUMER')")
@Tag(
        name = "Carrinho",
        description = "Carrinho do cliente autenticado. O ID do cliente é obtido pelo token."
)
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "Token ausente, inválido ou expirado",
                content = @Content
        ),
        @ApiResponse(
                responseCode = "403",
                description = "O usuário autenticado não possui o papel CONSUMER",
                content = @Content
        )
})
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(
            operationId = "getMyCart",
            summary = "Consultar meu carrinho",
            description = """
                    Retorna os itens, os preços registrados e o total calculado pelo backend.
                    No primeiro acesso, retorna 200 com items vazio e total 0.00.
                    Nesse caso, id e updatedAt são nulos porque o carrinho ainda não foi salvo.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Carrinho consultado",
            useReturnTypeSchema = true
    )
    public ResponseEntity<CartResponseDTO> searchByUser(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(cartService.searchByUser(AuthenticatedUser.id(jwt)));
    }

    @PostMapping("/items")
    @Operation(
            operationId = "addItemToMyCart",
            summary = "Adicionar uma variação ao carrinho",
            description = """
                    Envie o stockId da variação de tamanho selecionada no catálogo.
                    Se ela já estiver no carrinho, a quantidade enviada será somada à atual.
                    A quantidade acumulada não pode exceder o estoque disponível.
                    Um carrinho preenchido só aceita produtos da mesma loja.
                    Adicionar o item não reserva nem baixa estoque.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AddItemRequestDTO.class),
                            examples = @ExampleObject(value = """
                                    {"stockId": 101, "quantity": 2}
                                    """)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrinho atualizado",
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Variação de estoque não encontrada",
                    content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "Produto inativo ou carrinho com itens de outra loja",
                    content = @Content)
    })
    public ResponseEntity<CartResponseDTO> addItem(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddItemRequestDTO request
    ) {
        return ResponseEntity.ok(cartService.addItem(AuthenticatedUser.id(jwt), request));
    }

    @PutMapping("/items/{itemId}")
    @Operation(
            operationId = "updateMyCartItemQuantity",
            summary = "Definir a quantidade de um item",
            description = """
                    Envie a quantidade final desejada. Por exemplo, se o item tem 2 unidades
                    e a requisição informa 3, o resultado será 3 unidades.
                    O backend verifica a disponibilidade e atualiza o preço da linha.
                    Para remover o item, utilize DELETE.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UpdateItemRequestDTO.class),
                            examples = @ExampleObject(value = """
                                    {"quantity": 3}
                                    """)
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrinho atualizado",
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Carrinho ou item não encontrado para este cliente",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Produto inativo",
                    content = @Content)
    })
    public ResponseEntity<CartResponseDTO> updateItemQuantity(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do item retornado em items[].id", example = "30")
            @PathVariable("itemId") Long itemId,
            @Valid @RequestBody UpdateItemRequestDTO request
    ) {
        return ResponseEntity.ok(
                cartService.updateItemQuantity(AuthenticatedUser.id(jwt), itemId, request)
        );
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(
            operationId = "removeItemFromMyCart",
            summary = "Remover um item do carrinho",
            description = "Remove a linha inteira, independentemente da quantidade, e recalcula o total."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrinho após a remoção",
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Carrinho ou item não encontrado para este cliente",
                    content = @Content)
    })
    public ResponseEntity<CartResponseDTO> removeItem(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID do item retornado em items[].id", example = "30")
            @PathVariable("itemId") Long itemId
    ) {
        return ResponseEntity.ok(cartService.removeItem(AuthenticatedUser.id(jwt), itemId));
    }

    @DeleteMapping("/items")
    @Operation(
            operationId = "clearMyCart",
            summary = "Esvaziar meu carrinho",
            description = """
                    Remove todos os itens e retorna o carrinho com total 0.00.
                    Se ainda não houver carrinho salvo, retorna um carrinho vazio sem criar registro.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Carrinho vazio",
            useReturnTypeSchema = true
    )
    public ResponseEntity<CartResponseDTO> clear(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(cartService.clear(AuthenticatedUser.id(jwt)));
    }
}