package com.vitryne.api.product.controller;

import com.vitryne.api.product.dto.ProductClientResponseDTO;
import com.vitryne.api.product.service.ProductService;
import com.vitryne.api.shared.dto.PageResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/product")
@Tag(
        name = "Catálogo de produtos",
        description = "Vitrine e detalhe de produtos para o app"
)
public class ProductController {

    private final ProductService productService;

    @Operation(
            operationId = "listProducts",
            summary = "Listar produtos ativos",
            description = """
                    Lista paginada dos produtos ativos, dos mais recentes para os mais
                    antigos, incluindo produtos esgotados.

                    Produtos esgotados possuem availableProduct igual a false.
                    Quando não houver produtos ativos, content vem vazio.
                    Use totalPages (ou totalElements) para saber se há próxima página.

                    O app deve utilizar finalPrice como preço vigente.
                    Cada produto traz a loja (store.id e store.name); compare com
                    o store do carrinho para avisar antes de adicionar produto de outra loja.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página do catálogo"),
            @ApiResponse(
                    responseCode = "400",
                    description = "page negativo ou size fora de 1 a 50",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<PageResponseDTO<ProductClientResponseDTO>> listProducts(
            @Parameter(description = "Número da página, começando em 0", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Itens por página, de 1 a 50", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(productService.listProducts(page, size));
    }

    @Operation(
            operationId = "findProductForClient",
            summary = "Consultar detalhe de produto ativo",
            description = """
                    Retorna dados do produto e suas variações de estoque.

                    availableSizes[].id é o identificador da variação
                    utilizado na seleção para o carrinho.

                    Tamanhos sem estoque continuam na resposta,
                    com available igual a false.

                    store traz o id e o nome da loja que vende o produto.

                    Um produto ativo esgotado retorna 200.
                    Produto inexistente ou inativo retorna 404.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Produto encontrado, com ou sem estoque",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ProductClientResponseDTO.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Produto inexistente ou inativo"
            )
    })
    @GetMapping("/client/{id}")
    public ResponseEntity<ProductClientResponseDTO> findByIdClient(@PathVariable Long id){
        ProductClientResponseDTO product = productService.findByIdClient(id);
        return ResponseEntity.ok(product);
    }
}
