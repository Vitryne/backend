package com.vitryne.api.image.entity;

import com.vitryne.api.product.entity.Product;
import com.vitryne.api.store.entity.Store;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Imagem enviada ao armazenamento externo por uma loja.
 * Sem produto, a imagem está livre e entra na limpeza depois do prazo.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "uploaded_image")
public class UploadedImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private String publicId;

    @Column(name = "url", nullable = false, unique = true, updatable = false, length = 2048)
    private String url;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "orphaned_since")
    private Instant orphanedSince;

    public UploadedImage(Store store, String publicId, String url, Instant now) {
        this.store = store;
        this.publicId = publicId;
        this.url = url;
        this.createdAt = now;
        this.orphanedSince = now;
    }

    public boolean belongsTo(Store store) {
        return this.store.getId().equals(store.getId());
    }

    public boolean isUsedByOtherThan(Product product) {
        return this.product != null && !this.product.getId().equals(product.getId());
    }

    public void attachTo(Product product) {
        this.product = product;
        this.orphanedSince = null;
    }

    public void release(Instant now) {
        this.product = null;
        this.orphanedSince = now;
    }
}
