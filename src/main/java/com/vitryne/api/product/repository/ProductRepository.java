package com.vitryne.api.product.repository;

import com.vitryne.api.product.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndStoreOwnerId(
            Long productId,
            Long ownerId
    );

    List<Product> findAllByStoreOwnerId(Long ownerId);

    List<Product> findAllByIdInAndStoreOwnerId(
            Collection<Long> ids,
            Long ownerId
    );

    @Query("""
            SELECT DISTINCT oi.stock.product.id
            FROM OrderItem oi
            WHERE oi.stock.product.id IN :productIds
            """)
    List<Long> findProductIdsWithOrders(
            @Param("productIds") Collection<Long> productIds
    );

    /** Catálogo do app: estoques e loja na mesma consulta, sem busca por produto. */
    @EntityGraph(attributePaths = {"stocks", "store"})
    List<Product> findAllByStatus(String status);

    @EntityGraph(attributePaths = {"stocks", "store"})
    Optional<Product> findByIdAndStatus(Long id, String status);
}