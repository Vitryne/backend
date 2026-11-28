package com.vitryne.api.product.repository;

import com.vitryne.api.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Catálogo do app, paginado no banco. A loja vem no mesmo SELECT; os estoques
     * da página vêm em uma consulta só pelo @BatchSize de Product.stocks.
     * (Buscar a coleção no EntityGraph faria o Hibernate paginar em memória.)
     */
    @EntityGraph(attributePaths = "store")
    Page<Product> findByStatus(String status, Pageable pageable);

    @EntityGraph(attributePaths = {"stocks", "store"})
    Optional<Product> findByIdAndStatus(Long id, String status);
}