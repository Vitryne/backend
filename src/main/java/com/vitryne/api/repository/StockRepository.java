package com.vitryne.api.repository;

import com.vitryne.api.entity.Stock;
import com.vitryne.api.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByIdAndProductStoreOwnerId(
            Long stockId,
            Long ownerId
    );

    Optional<Store> findByOwnerId(Long ownerId);
}