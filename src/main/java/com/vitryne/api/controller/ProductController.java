package com.vitryne.api.controller;

import com.vitryne.api.dto.ProductResponseDTO;
import com.vitryne.api.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> listProducts(){
        log.info("Request received to list all products");
        List<ProductResponseDTO> products = productService.listProducts();
        log.info("Returning {} products found", products.size());
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> findById(@PathVariable Long id){
        log.info("Request received to find product with ID: {}", id);
        ProductResponseDTO product = productService.findById(id);
        log.info("Product with ID: {} returned successfully", id);
        return ResponseEntity.ok(product);
    }
}
