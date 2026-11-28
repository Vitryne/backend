package com.vitryne.api.store.repository;

import com.vitryne.api.store.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByOwnerId(Long ownerId);

    boolean existsByOwnerId(Long ownerId);

    boolean existsByCnpj(String cnpj);

    /** Lojas ativas com ao menos um produto ativo: loja sem vitrine não aparece na Home. */
    @Query(
            value = """
                    SELECT s FROM Store s
                     WHERE s.active = true
                       AND EXISTS (
                           SELECT 1 FROM Product p
                            WHERE p.store = s AND p.status = 'ACTIVE'
                       )
                    """,
            countQuery = """
                    SELECT count(s) FROM Store s
                     WHERE s.active = true
                       AND EXISTS (
                           SELECT 1 FROM Product p
                            WHERE p.store = s AND p.status = 'ACTIVE'
                       )
                    """
    )
    Page<Store> findActiveWithActiveProducts(Pageable pageable);
}
