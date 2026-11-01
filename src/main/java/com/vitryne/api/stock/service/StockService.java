package com.vitryne.api.stock.service;

import com.vitryne.api.product.dto.CreateStockRequestDTO;
import com.vitryne.api.product.entity.Product;
import com.vitryne.api.product.repository.ProductRepository;
import com.vitryne.api.stock.dto.AdjustStockRequestDTO;
import com.vitryne.api.stock.dto.ReplenishStockRequestDTO;
import com.vitryne.api.stock.dto.StockManagementResponseDTO;
import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;

    @Transactional
    public StockManagementResponseDTO addStock(
            Long productId,
            Long ownerId,
            CreateStockRequestDTO request
    ) {
        Product product = findManagedProduct(productId, ownerId);

        String normalizedSize = request.size()
                .trim()
                .toUpperCase(Locale.ROOT);

        if (stockRepository.existsByProductIdAndSize(
                productId, normalizedSize
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe estoque para esse tamanho"
            );
        }

        Stock stock = product.addStock(
                normalizedSize,
                request.quantity()
        );

        Stock savedStock = stockRepository.saveAndFlush(stock);

        return toResponse(savedStock);
    }

    @Transactional
    public StockManagementResponseDTO replenishStock(
            Long productId,
            Long stockId,
            Long ownerId,
            ReplenishStockRequestDTO request
    ) {
        Stock stock = findManagedStock(
                productId,
                stockId,
                ownerId
        );

        stock.increaseStock(request.quantity());

        stockRepository.flush();

        return toResponse(stock);
    }

    @Transactional
    public StockManagementResponseDTO adjustStock(
            Long productId,
            Long stockId,
            Long ownerId,
            AdjustStockRequestDTO request
    ) {
        Stock stock = findManagedStock(
                productId,
                stockId,
                ownerId
        );

        if (!Objects.equals(stock.getVersion(), request.version())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O estoque mudou. Atualize os dados antes de ajustar."
            );
        }

        stock.adjustQuantity(request.quantity());

        stockRepository.flush();

        return toResponse(stock);
    }

    private Product findManagedProduct(
            Long productId,
            Long ownerId
    ) {
        return productRepository
                .findByIdAndStoreOwnerId(productId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produto não encontrado"
                ));
    }

    private Stock findManagedStock(
            Long productId,
            Long stockId,
            Long ownerId
    ) {
        return stockRepository
                .findByIdAndProductIdAndProductStoreOwnerId(
                        stockId,
                        productId,
                        ownerId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Estoque não encontrado"
                ));
    }

    private StockManagementResponseDTO toResponse(Stock stock) {
        return new StockManagementResponseDTO(
                stock.getId(),
                stock.getProduct().getId(),
                stock.getSize(),
                stock.getQuantity(),
                stock.isAvailable(),
                stock.getVersion(),
                stock.getCreatedAt(),
                stock.getUpdatedAt()
        );
    }
}