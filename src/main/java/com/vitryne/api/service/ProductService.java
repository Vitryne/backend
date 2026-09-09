package com.vitryne.api.service;

import com.vitryne.api.dto.ProductResponseDTO;
import com.vitryne.api.dto.AvailableSizeDTO;
import com.vitryne.api.entity.Product;
import com.vitryne.api.exception.ProductNotFoundException;
import com.vitryne.api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponseDTO> listProducts(){
        return productRepository.findAll().stream().map(this::toResponseDTO).toList();
    }

    public ProductResponseDTO findById(Long id){
        Product product = findProduct(id);
        return toResponseDTO(product);
    }

    public Boolean checkAvailability(Long productId, String size){
        Product product = findProduct(productId);
        return product.checkAvailability(size);
    }

    private ProductResponseDTO toResponseDTO(Product product){
        List<AvailableSizeDTO> sizes = product.getStocks().stream()
                .map(stock -> new AvailableSizeDTO(
                        stock.getId(),
                        stock.getSize(),
                        stock.getQuantity(),
                        product.checkAvailability(stock.getSize())
                ))
                .toList();

        return  ProductResponseDTO.builder().id(product.getId()).rating(product.getRating()).color(product.getColor()).price(product.getPrice()).name(product.getName())
                .type(product.getType()).description(product.getDescription()).promotionalPrice(product.getPromotionalPrice())
                .finalPrice(product.calculateFinalPrice()).status(product.getStatus()).photoUrls(product.getPhotoUrls()).availableSizes(sizes).build();
    }

    private Product findProduct(Long id){
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    // Manipulation methods to be implemented once the Store entity is modeled
}
