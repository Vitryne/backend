package com.vitryne.api.cart.repository;

import com.vitryne.api.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("""
            SELECT ci.stock.id
            FROM CartItem ci
            WHERE ci.cart.userId = :userId
            """)
    List<Long> findStockIdsByUserId(@Param("userId") Long userId);
}
