package com.vitryne.api.product.controller;

import com.vitryne.api.product.dto.ProductClientResponseDTO;
import com.vitryne.api.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
                    Retorna produtos ativos, incluindo produtos esgotados.

                    Produtos esgotados possuem availableProduct igual a false.
                    Quando não houver produtos ativos, retorna uma lista vazia.

                    O app deve utilizar finalPrice como preço vigente.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Catálogo retornado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(
                            schema = @Schema(
                                    implementation = ProductClientResponseDTO.class
                            )
                    )
            )
    )
    @GetMapping
    public ResponseEntity<List<ProductClientResponseDTO>> listProducts() {
        return ResponseEntity.ok(productService.listProducts());
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
