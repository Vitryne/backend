package com.vitryne.api.repository;

import com.vitryne.api.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query(nativeQuery = true, value = """
        select * from cart c where c.user_id = :userId
    """)
    Optional<Cart> findByUserId(@Param("userId") Long userId);
}
