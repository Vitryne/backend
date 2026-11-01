package com.vitryne.api.stock.repository;

import com.vitryne.api.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByIdAndProductIdAndProductStoreOwnerId(
            Long stockId,
            Long productId,
            Long ownerId
    );

    boolean existsByProductIdAndSize(
            Long productId,
            String size
    );
}