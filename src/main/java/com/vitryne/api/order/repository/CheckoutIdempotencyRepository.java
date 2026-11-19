package com.vitryne.api.order.repository;

import com.vitryne.api.order.entity.CheckoutIdempotency;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CheckoutIdempotencyRepository extends JpaRepository<CheckoutIdempotency, Long> {

    @EntityGraph(attributePaths = {"payment"})
    Optional<CheckoutIdempotency> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
}
