package com.vitryne.api.product.service;

import com.vitryne.api.product.dto.ConfigureSaleRequestDTO;
import com.vitryne.api.product.dto.ProductClientResponseDTO;
import com.vitryne.api.product.dto.ProductManagementResponseDTO;
import com.vitryne.api.shared.dto.AvailableSizeDTO;
import com.vitryne.api.product.entity.Product;
import com.vitryne.api.shared.exception.ProductNotFoundException;
import com.vitryne.api.product.repository.ProductRepository;
import com.vitryne.api.store.repository.StoreRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

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
    public ProductManagementResponseDTO findByIdManager(
            Long id,
            Long ownerId
    ) {
        Product product = productRepository
                .findByIdAndStoreOwnerId(id, ownerId)
                .orElseThrow(() -> new ProductNotFoundException(id));

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
    public ProductManagementResponseDTO configureSale(
            Long id,
            Long ownerId,
            ConfigureSaleRequestDTO requestDTO
    ) {
        Product product = findManagedProduct(id, ownerId);

        product.configureSale(
                requestDTO.promoPrice(),
                requestDTO.startDate(),
                requestDTO.endDate()
        );

        return toManagementResponseDTO(product);
    }

    private List<AvailableSizeDTO> findSizes(Product product) {
        return product.getStocks().stream()
                .map(stock -> new AvailableSizeDTO(
                        stock.getId(),
                        stock.getSize(),
                        stock.getQuantity(),
                        stock.isAvailable()
                ))
                .toList();
    }

    private Product findProduct(Long id){
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Product findManagedProduct(Long productId, Long ownerId) {
        return productRepository
                .findByIdAndStoreOwnerId(productId, ownerId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Transactional(readOnly = true)
    public List<ProductManagementResponseDTO> listManagedProducts(
            Long ownerId
    ) {
        return productRepository.findAllByStoreOwnerId(ownerId)
                .stream()
                .map(this::toManagementResponseDTO)
                .toList();
    }



    // Manipulation methods to be implemented once the Store entity is modeled
}
