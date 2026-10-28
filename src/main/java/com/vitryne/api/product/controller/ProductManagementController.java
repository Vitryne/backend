package com.vitryne.api.product.controller;

import com.vitryne.api.product.dto.CreateProductRequestDTO;
import com.vitryne.api.product.dto.EditProductRequestDTO;
import com.vitryne.api.product.dto.ProductManagementResponseDTO;
import com.vitryne.api.product.dto.StatusRequestDTO;
import com.vitryne.api.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        description = "Gerenciamento dos produtos da própria loja"
)
public class ProductManagementController {

    private final ProductService productService;

    @Operation(
            operationId = "findProductForManager",
            summary = "Consultar produto da própria loja",
            description = """
                    Exige lojista autenticado.
                    O ID do proprietário vem da autenticação
                    e não é enviado na requisição.
                    """
    )
    @GetMapping("/{id}")
    public ResponseEntity<ProductManagementResponseDTO> findByIdManager(
            @Parameter(description = "ID do produto", example = "1")
            @PathVariable Long id,

            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "id") Long ownerId
    ) {
        return ResponseEntity.ok(
                productService.findByIdManager(id, ownerId)
        );
    }

    @GetMapping
    public ResponseEntity<List<ProductManagementResponseDTO>> findByOwner(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "id") Long ownerId
    ) {
        return ResponseEntity.ok(
                productService.listManagedProducts(ownerId)
        );
    }

    @PostMapping
    public ResponseEntity<ProductManagementResponseDTO> createProduct(
            @AuthenticationPrincipal(expression = "id") Long ownerId,
            @Valid @RequestBody CreateProductRequestDTO request
            ){
        return ResponseEntity.ok(
                productService.createProduct(request, ownerId)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductManagementResponseDTO> editProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long ownerId,
            @Valid @RequestBody EditProductRequestDTO request

    ){
        return ResponseEntity.ok(
                productService.editProduct(id, ownerId, request)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductManagementResponseDTO> alterStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long ownerId,
            @Valid @RequestBody StatusRequestDTO request
    ){
        return ResponseEntity.ok(
                productService.alterStatus(id, ownerId, request)
        );
    }
}