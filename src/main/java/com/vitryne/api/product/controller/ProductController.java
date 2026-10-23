package com.vitryne.api.product.controller;

import com.vitryne.api.product.dto.ProductClientResponseDTO;
import com.vitryne.api.product.dto.ProductManagementResponseDTO;
import com.vitryne.api.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductClientResponseDTO>> listProducts(){
        List<ProductClientResponseDTO> products = productService.listProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/client/{id}")
    public ResponseEntity<ProductClientResponseDTO> findByIdClient(@PathVariable Long id){
        ProductClientResponseDTO product = productService.findByIdClient(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/manager/{id}")
    @PreAuthorize("hasRole('STORE')")
    public ResponseEntity<ProductManagementResponseDTO> findByIdManager(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long ownerId
    ) {
        return ResponseEntity.ok(
                productService.findByIdManager(id, ownerId)
        );
    }
    /*@PutMapping("/manager/{id}"/sale/)
    public ResponseEntity<ProductManagementResponseDTO> configureSale(@PathVariable Long id, @RequestBody ConfigureSaleRequestDTO request){
        return ResponseEntity.ok(productService.configureSale(id, request)); falta linkar a um lojista
    }*/
}
