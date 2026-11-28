package com.vitryne.api.order.repository;

import com.vitryne.api.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "payment", "store"})
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"items", "payment", "store"})
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"items", "payment", "store"})
    List<Order> findAllByStoreOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @EntityGraph(attributePaths = {"items", "payment", "store"})
    Optional<Order> findByIdAndStoreOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = {"items", "payment", "store"})
    Optional<Order> findByPaymentId(Long paymentId);

    /**
     * Trava o pedido da loja antes de mudar o status. Serializa aceite,
     * recusa, avanço e cancelamento do mesmo pedido.
     */
    @Query(value = """
            SELECT o.id
            FROM orders o
            JOIN store s ON s.id = o.store_id
            WHERE o.id = :orderId AND s.owner_id = :ownerId
            FOR UPDATE OF o
            """, nativeQuery = true)
    Optional<Long> lockByIdAndStoreOwnerId(@Param("orderId") Long orderId, @Param("ownerId") Long ownerId);

    /** Trava o pedido do cliente antes de cancelar. */
    @Query(value = """
            SELECT id
            FROM orders
            WHERE id = :orderId AND user_id = :userId
            FOR UPDATE
            """, nativeQuery = true)
    Optional<Long> lockByIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Query("SELECT i.stock.id FROM OrderItem i WHERE i.order.id = :orderId")
    List<Long> findStockIdsByOrderId(@Param("orderId") Long orderId);
}
