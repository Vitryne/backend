package com.vitryne.api.stock.repository;

import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByIdAndProductStoreOwnerId(
            Long stockId,
            Long ownerId
    );

    Optional<Store> findByOwnerId(Long ownerId);
}