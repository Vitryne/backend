package com.vitryne.api.product.controller;

import com.vitryne.api.product.dto.CreateProductRequestDTO;
import com.vitryne.api.product.dto.EditProductRequestDTO;
import com.vitryne.api.product.dto.ProductManagementResponseDTO;
import com.vitryne.api.product.dto.StatusRequestDTO;
import com.vitryne.api.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/product/manager")
@PreAuthorize("hasRole('STORE')")
@Tag(
        name = "Gestão de produtos",
        description = """
                Cadastro e gerenciamento dos produtos da própria loja.
                Exige usuário autenticado com perfil STORE.
                O proprietário é identificado pela autenticação.
                """
)
public class ProductManagementController {

    private final ProductService productService;

    @Operation(
            operationId = "findProductForManager",
            summary = "Consultar produto da própria loja",
            description = """
                    Retorna os dados gerenciais de um produto da loja autenticada,
                    incluindo estoque, promoção e datas disponíveis no DTO.

                    Permite consultar produtos ativos e inativos.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produto encontrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ProductManagementResponseDTO.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto inexistente ou não pertencente à loja autenticada",
                    content = @Content
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductManagementResponseDTO> findByIdManager(
            @Parameter(
                    description = "ID do produto",
                    example = "1",
                    required = true
            )
            @PathVariable Long id,

            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "id") Long ownerId
    ) {
        return ResponseEntity.ok(
                productService.findByIdManager(id, ownerId)
        );
    }

    @Operation(
            operationId = "listManagedProducts",
            summary = "Listar produtos da própria loja",
            description = """
                    Retorna os produtos ativos e inativos da loja autenticada.
                    Quando não houver produtos cadastrados, retorna uma lista vazia.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de produtos retornada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(
                            schema = @Schema(
                                    implementation = ProductManagementResponseDTO.class
                            )
                    )
            )
    )
    @GetMapping
    public ResponseEntity<List<ProductManagementResponseDTO>> findByOwner(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "id") Long ownerId
    ) {
        return ResponseEntity.ok(
                productService.listManagedProducts(ownerId)
        );
    }

    @Operation(
            operationId = "createManagedProduct",
            summary = "Cadastrar produto na própria loja",
            description = """
                    Cadastra os dados básicos, URLs das fotos, tamanhos e
                    quantidades iniciais do produto.

                    A loja é identificada pelo usuário autenticado.
                    O produto é criado com status ACTIVE.

                    O preço deve ser positivo e as quantidades não podem
                    ser negativas. Não é permitido repetir tamanhos
                    após a normalização.

                    Este endpoint recebe URLs de imagens, não arquivos.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produto cadastrado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ProductManagementResponseDTO.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Loja do usuário autenticado não encontrada",
                    content = @Content
            )
    })
    @PostMapping
    public ResponseEntity<ProductManagementResponseDTO> createProduct(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "id") Long ownerId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = """
                            Dados do produto e estoques iniciais.
                            Não informe proprietário, loja, avaliação ou datas de auditoria.
                            """
            )
            @Valid @RequestBody CreateProductRequestDTO request
    ) {
        return ResponseEntity.ok(
                productService.createProduct(request, ownerId)
        );
    }

    @Operation(
            operationId = "editManagedProduct",
            summary = "Editar dados do produto",
            description = """
                    Atualiza nome, descrição, preço, tipo, cor, URLs das fotos
                    e limite de alerta de estoque.

                    Envie todos os campos do DTO de edição.
                    A lista de fotos enviada substitui a anterior.
                    Campos opcionais enviados como null ficam sem valor.

                    Preserva a loja, os estoques, o status, a avaliação
                    e a data de criação.

                    Havendo promoção vigente ou futura, o preço normal
                    deve permanecer maior que o preço promocional.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produto atualizado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ProductManagementResponseDTO.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos ou preço incompatível com a promoção",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto inexistente ou não pertencente à loja autenticada",
                    content = @Content
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductManagementResponseDTO> editProduct(
            @Parameter(
                    description = "ID do produto",
                    example = "1",
                    required = true
            )
            @PathVariable Long id,

            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "id") Long ownerId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Dados completos dos campos editáveis do produto"
            )
            @Valid @RequestBody EditProductRequestDTO request
    ) {
        return ResponseEntity.ok(
                productService.editProduct(id, ownerId, request)
        );
    }

    @Operation(
            operationId = "changeManagedProductStatus",
            summary = "Ativar ou desativar produto",
            description = """
                    Define o status do produto como ACTIVE ou INACTIVE.

                    Produtos inativos deixam de aparecer nas consultas públicas,
                    mas permanecem disponíveis na gestão da loja.

                    A operação preserva o produto e seus estoques.
                    Informar o status atual mantém o produto nesse estado.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produto retornado com o status solicitado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ProductManagementResponseDTO.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Status ausente ou inválido",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto inexistente ou não pertencente à loja autenticada",
                    content = @Content
            )
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductManagementResponseDTO> alterStatus(
            @Parameter(
                    description = "ID do produto",
                    example = "1",
                    required = true
            )
            @PathVariable Long id,

            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "id") Long ownerId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Status desejado para o produto: ACTIVE ou INACTIVE"
            )
            @Valid @RequestBody StatusRequestDTO request
    ) {
        return ResponseEntity.ok(
                productService.alterStatus(id, ownerId, request)
        );
    }
}