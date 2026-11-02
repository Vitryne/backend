package com.vitryne.api.cart.repository;

import com.vitryne.api.cart.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {"items", "items.stock", "items.stock.product"})
    Optional<Cart> findByUserId(Long userId);

    @Query(value = """
        SELECT id
        FROM users
        WHERE id = :userId
        FOR UPDATE
        """, nativeQuery = true)
    Optional<Long> lockUserForCartMutation(@Param("userId") Long userId);
}