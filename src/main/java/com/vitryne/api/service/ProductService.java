package com.vitryne.api.service;

import com.vitryne.api.dto.ConfigureSaleRequestDTO;
import com.vitryne.api.dto.ProductClientResponseDTO;
import com.vitryne.api.dto.ProductManagementResponseDTO;
import com.vitryne.api.dto.AvailableSizeDTO;
import com.vitryne.api.entity.Product;
import com.vitryne.api.exception.ProductNotFoundException;
import com.vitryne.api.repository.ProductRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductClientResponseDTO> listProducts(){
        return productRepository.findAll().stream().map(this::toClientResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public ProductClientResponseDTO findByIdClient(Long id){
        Product product = findProduct(id);
        return toClientResponseDTO(product);
    }

    @Transactional(readOnly = true)
    public ProductManagementResponseDTO findByIdManager(Long id){
        Product product = findProduct(id);
        return toManagementResponseDTO(product);
    }

    private ProductClientResponseDTO toClientResponseDTO(Product product){

        return  ProductClientResponseDTO.builder().id(product.getId()).rating(product.getRating()).color(product.getColor()).price(product.getPrice()).name(product.getName())
                .type(product.getType()).description(product.getDescription()).promotionalPrice(product.getPromotionalPrice())
                .finalPrice(product.calculateFinalPrice()).status(product.getStatus()).photoUrls(product.getPhotoUrls()).availableSizes(findSizes(product))
                .availableProduct(product.checkAvailability()).newProduct(product.isNew()).onSale(product.isItInSale())
                .build();
    }

    private ProductManagementResponseDTO toManagementResponseDTO(Product product){

        return  ProductManagementResponseDTO.builder().id(product.getId()).rating(product.getRating()).color(product.getColor()).price(product.getPrice()).name(product.getName())
                .type(product.getType()).description(product.getDescription()).promotionalPrice(product.getPromotionalPrice())
                .finalPrice(product.calculateFinalPrice()).status(product.getStatus()).photoUrls(product.getPhotoUrls()).availableSizes(findSizes(product))
                .availableProduct(product.checkAvailability()).newProduct(product.isNew()).onSale(product.isItInSale())
                .promotionalStartDate(product.getPromotionalStartDate()).promotionalEndDate(product.getPromotionalEndDate())
                .stockWarningThreshold(product.getStockWarningThreshold()).createdAt(product.getCreatedAt())
                .build();
    }

    @Transactional
    public ProductManagementResponseDTO configureSale(Long id, ConfigureSaleRequestDTO requestDTO){
        Product product = findProduct(id);
        product.configureSale(requestDTO.promoPrice(), requestDTO.startDate(), requestDTO.endDate());

        return toManagementResponseDTO(product);
    }

    private List<AvailableSizeDTO> findSizes(Product product){
        return product.getStocks().stream()
                .map(stock -> new AvailableSizeDTO(
                        stock.getId(),
                        stock.getSize(),
                        stock.getQuantity(),
                        product.checkAvailability(stock.getSize())
                ))
                .toList();
    }

    private Product findProduct(Long id){
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    // Manipulation methods to be implemented once the Store entity is modeled
}
