package com.vitryne.api.product.service;

import com.vitryne.api.product.dto.*;
import com.vitryne.api.product.exception.ProductDontBelongToStoreException;
import com.vitryne.api.shared.dto.AvailableSizeDTO;
import com.vitryne.api.product.entity.Product;
import com.vitryne.api.shared.exception.NoProductsInThatStoreException;
import com.vitryne.api.shared.exception.ProductNotFoundException;
import com.vitryne.api.product.repository.ProductRepository;
import com.vitryne.api.stock.dto.StockManagementResponseDTO;
import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.store.entity.Store;
import com.vitryne.api.store.exception.StoreNotFoundException;
import com.vitryne.api.store.repository.StoreRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;


    @Transactional(readOnly = true)
    public ProductManagementResponseDTO findByIdManager(
            Long id,
            Long ownerId
    ) {
        return toManagementResponseDTO(
                findManagedProduct(id, ownerId)
        );
    }

    @Transactional
    public ProductManagementResponseDTO createProduct(CreateProductRequestDTO request, Long ownerId){
        Store store = storeRepository.findByOwnerId(ownerId).orElseThrow(() -> new StoreNotFoundException(ownerId));

        Product product = toProductBuild(request, store);

        for(CreateStockRequestDTO stockRequest : request.stocks()){
            product.addStock(
                    stockRequest.size(),
                    stockRequest.quantity()
            );
        }

        Product savedProduct = productRepository.save(product);

        return toManagementResponseDTO(savedProduct);
    }

    @Transactional
    public ProductManagementResponseDTO editProduct(Long id, Long ownerId, EditProductRequestDTO request){
        Product product = findManagedProduct(id, ownerId);

        product.updateDetails(
                request.name(),
                request.description(),
                request.price(),
                request.type(),
                request.color(),
                request.stockWarningThreshold(),
                request.photoUrls()
        );

        productRepository.flush();

        return toManagementResponseDTO(product);
    }

    @Transactional
    public ProductManagementResponseDTO alterStatus(Long id, Long ownerId, StatusRequestDTO request){
        Product product = findManagedProduct(id, ownerId);

        product.changeStatus(request.status());

        productRepository.flush();

        return toManagementResponseDTO(product);
    }

    private ProductManagementResponseDTO toManagementResponseDTO(Product product){

        return  ProductManagementResponseDTO.builder().id(product.getId()).rating(product.getRating()).color(product.getColor()).price(product.getPrice()).name(product.getName())
                .type(product.getType()).description(product.getDescription()).promotionalPrice(product.getPromotionalPrice())
                .finalPrice(product.calculateFinalPrice()).status(product.getStatus()).photoUrls(product.getPhotoUrls()).availableSizes(findSizes(product))
                .availableProduct(product.checkAvailability()).newProduct(product.isNew()).onSale(product.isItInSale())
                .promotionalStartDate(product.getPromotionalStartDate()).promotionalEndDate(product.getPromotionalEndDate())
                .stockWarningThreshold(product.getStockWarningThreshold()).createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt()).stocks(findManagedStocks(product))
                .build();
    }

    private Product toProductBuild(CreateProductRequestDTO request, Store store){
        return  Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .type(request.type())
                .color(request.color())
                .stockWarningThreshold(request.stockWarningThreshold())
                .photoUrls(request.photoUrls())
                .store(store)
                .status("ACTIVE")
                .build();
    }

    private List<StockManagementResponseDTO> findManagedStocks(
            Product product
    ) {
        return product.getStocks().stream()
                .map(stock -> new StockManagementResponseDTO(
                        stock.getId(),
                        product.getId(),
                        stock.getSize(),
                        stock.getQuantity(),
                        stock.isAvailable(),
                        stock.getVersion(),
                        stock.getCreatedAt(),
                        stock.getUpdatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductClientResponseDTO> listProducts() {
        return productRepository.findAllByStatus("ACTIVE")
                .stream()
                .sorted(Comparator.comparing(Product::getId).reversed())
                .map(this::toClientResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductClientResponseDTO findByIdClient(Long id) {
        Product product = productRepository
                .findByIdAndStatus(id, "ACTIVE")
                .orElseThrow(() -> new ProductNotFoundException(id));

        return toClientResponseDTO(product);
    }

    private ProductClientResponseDTO toClientResponseDTO(Product product) {
        List<AvailableSizeDTO> sizes = findSizes(product);

        boolean onSale = product.isItInSale();

        BigDecimal finalPrice = onSale
                ? product.getPromotionalPrice()
                : product.getPrice();

        boolean available = sizes.stream()
                .anyMatch(AvailableSizeDTO::available);

        return ProductClientResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .type(product.getType())
                .color(product.getColor())
                .rating(product.getRating())
                .price(product.getPrice())
                .promotionalPrice(product.getPromotionalPrice())
                .finalPrice(finalPrice)
                .status(product.getStatus())
                .photoUrls(
                        product.getPhotoUrls() == null
                                ? List.of()
                                : List.copyOf(product.getPhotoUrls())
                )
                .availableSizes(sizes)
                .availableProduct(available)
                .newProduct(product.isNew())
                .onSale(onSale)
                .build();
    }

    private List<AvailableSizeDTO> findSizes(Product product) {
        return product.getStocks().stream()
                .sorted(Comparator.comparing(Stock::getId))
                .map(stock -> new AvailableSizeDTO(
                        stock.getId(),
                        stock.getSize(),
                        stock.getQuantity(),
                        stock.isAvailable()
                ))
                .toList();
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
