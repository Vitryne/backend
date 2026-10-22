package com.vitryne.api.service;

import com.vitryne.api.dto.AvailableSizeDTO;
import com.vitryne.api.entity.Stock;
import com.vitryne.api.repository.StockRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    @Transactional
    public AvailableSizeDTO adjustQuantity(
            Long stockId,
            Long ownerId,
            Integer quantity
    ) {
        Stock stock = findManagedStock(stockId, ownerId);

        stock.adjustQuantity(quantity);

        return toResponse(stock);
    }

    @Transactional
    public AvailableSizeDTO increaseStock(
            Long stockId,
            Long ownerId,
            int quantity
    ) {
        Stock stock = findManagedStock(stockId, ownerId);

        stock.increaseStock(quantity);

        return toResponse(stock);
    }

    private Stock findManagedStock(Long stockId, Long ownerId) {
        return stockRepository
                .findByIdAndProductStoreOwnerId(stockId, ownerId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Stock not found")
                );
    }

    private AvailableSizeDTO toResponse(Stock stock) {
        return new AvailableSizeDTO(
                stock.getId(),
                stock.getSize(),
                stock.getQuantity(),
                stock.isAvailable()
        );
    }
}