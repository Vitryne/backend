package com.vitryne.api.stock.repository;

import com.vitryne.api.stock.entity.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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

    /**
     * Trava as variações para baixa de estoque. A ordenação por id evita
     * deadlock entre checkouts concorrentes com itens em comum.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM Stock s
            JOIN FETCH s.product p
            JOIN FETCH p.store
            WHERE s.id IN :ids
            ORDER BY s.id
            """)
    List<Stock> lockAllByIdForUpdate(@Param("ids") Collection<Long> ids);
}
