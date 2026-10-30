package com.vitryne.api.product.controller;

import com.vitryne.api.product.dto.ProductClientResponseDTO;
import com.vitryne.api.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/product")
@Tag(
        name = "Catálogo de produtos",
        description = "Consulta pública de produtos"
)
public class ProductController {

    private final ProductService productService;

    @Operation(
            operationId = "listProducts",
            summary = "Listar produtos do catálogo"
    )
    @GetMapping
    public ResponseEntity<List<ProductClientResponseDTO>> listProducts() {
        return ResponseEntity.ok(productService.listProducts());
    }

    @Operation(
            operationId = "findProductForClient",
            summary = "Consultar detalhe de produto"
    )
    @GetMapping("/client/{id}")
    public ResponseEntity<ProductClientResponseDTO> findByIdClient(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(productService.findByIdClient(id));
    }
}