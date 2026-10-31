package com.vitryne.api.product.controller;

import com.vitryne.api.product.dto.ProductClientResponseDTO;
import com.vitryne.api.product.dto.ProductManagementResponseDTO;
import com.vitryne.api.product.service.ProductService;
import com.vitryne.api.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(
        name = "Produtos",
        description = "Consulta pública e gerenciamento de produtos"
)
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;


    @Operation(
            operationId = "listProducts",
            summary = "Listar produtos do catálogo"
    )
    @GetMapping
    public ResponseEntity<List<ProductClientResponseDTO>> listProducts(){
        List<ProductClientResponseDTO> products = productService.listProducts();
        return ResponseEntity.ok(products);
    }

    @Operation(
            operationId = "findProductForClient",
            summary = "Consultar detalhe de produto"
    )
    @GetMapping("/client/{id}")
    public ResponseEntity<ProductClientResponseDTO> findByIdClient(@PathVariable Long id){
        ProductClientResponseDTO product = productService.findByIdClient(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/manager/{id}")
    @PreAuthorize("hasRole('STORE')")
    @Operation(
            operationId = "findProductForManager",
            summary = "Consultar produto da própria loja",
            description = """
        Exige lojista autenticado.
        O ID do proprietário vem da autenticação e não é enviado na requisição.
        """
    )
    public ResponseEntity<ProductManagementResponseDTO> findByIdManager(
            @Parameter(description = "ID do produto", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                productService.findByIdManager(id, AuthenticatedUser.id(jwt))
        );
    }
    /*@PutMapping("/manager/{id}"/sale/)
    public ResponseEntity<ProductManagementResponseDTO> configureSale(@PathVariable Long id, @RequestBody ConfigureSaleRequestDTO request){
        return ResponseEntity.ok(productService.configureSale(id, request)); falta linkar a um lojista
    }*/
}
