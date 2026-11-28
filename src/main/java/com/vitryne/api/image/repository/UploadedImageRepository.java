package com.vitryne.api.image.repository;

import com.vitryne.api.image.entity.UploadedImage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface UploadedImageRepository extends JpaRepository<UploadedImage, Long> {

    List<UploadedImage> findAllByUrlIn(Collection<String> urls);

    List<UploadedImage> findAllByProductId(Long productId);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE UploadedImage i
               SET i.product = null, i.orphanedSince = :now
             WHERE i.product.id IN :productIds
            """)
    int releaseFromProducts(
            @Param("productIds") Collection<Long> productIds,
            @Param("now") Instant now
    );

    /** Livres desde antes do corte; o coalesce cobre o product_id zerado pelo banco (ON DELETE SET NULL). */
    @Query("""
            SELECT i FROM UploadedImage i
             WHERE i.product IS NULL
               AND COALESCE(i.orphanedSince, i.createdAt) < :cutoff
             ORDER BY i.id
            """)
    List<UploadedImage> findOrphansBefore(@Param("cutoff") Instant cutoff, Pageable page);

    /** Apaga só se a imagem continua livre; 0 indica que ela foi usada nesse meio-tempo. */
    @Modifying
    @Query("DELETE FROM UploadedImage i WHERE i.id = :id AND i.product IS NULL")
    int deleteIfOrphan(@Param("id") Long id);
}
