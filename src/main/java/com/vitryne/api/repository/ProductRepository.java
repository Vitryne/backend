package com.vitryne.api.repository;

import com.vitryne.api.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndStoreOwnerId(
            Long productId,
            Long ownerId
    );

    List<Product> findAllByStoreOwnerId(Long ownerId);
}