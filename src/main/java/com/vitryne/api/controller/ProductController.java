package com.vitryne.api.controller;

import com.vitryne.api.dto.ConfigureSaleRequestDTO;
import com.vitryne.api.dto.ProductClientResponseDTO;
import com.vitryne.api.dto.ProductManagementResponseDTO;
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
    public ResponseEntity<ProductManagementResponseDTO> findByIdManager(@PathVariable Long id){
        ProductManagementResponseDTO product = productService.findByIdManager(id);
        return ResponseEntity.ok(product);
    }

    /*@PutMapping("/manager/{id}"/sale/)
    public ResponseEntity<ProductManagementResponseDTO> configureSale(@PathVariable Long id, @RequestBody ConfigureSaleRequestDTO request){
        return ResponseEntity.ok(productService.configureSale(id, request)); falta linkar a um lojista
    }*/
}
