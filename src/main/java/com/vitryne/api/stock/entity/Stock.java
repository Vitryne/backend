package com.vitryne.api.stock.entity;

import com.vitryne.api.shared.exception.InsufficientStockException;
import com.vitryne.api.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "stock")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "size", nullable = false, length = 10)
    private String size;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Version
    private Long version;

    public Stock(Product product, String size, Integer quantity) {
        if (product == null) {
            throw new IllegalArgumentException("Product is required");
        }

        if (size == null || size.isBlank()) {
            throw new IllegalArgumentException("Size is required");
        }

        validateQuantity(quantity);

        String normalizedSize = size.trim().toUpperCase(Locale.ROOT);

        if (normalizedSize.length() > 10) {
            throw new IllegalArgumentException(
                    "Size must have at most 10 characters"
            );
        }

        this.size = normalizedSize;
        this.product = product;
        this.quantity = quantity;
    }

    public void decreaseStock(int quantity) {
        validateMovement(quantity);

        if (this.quantity < quantity) {
            throw new InsufficientStockException(
                    this.size,
                    this.quantity,
                    quantity
            );
        }

        this.quantity -= quantity;
    }

    public void increaseStock(int quantity) {
        validateMovement(quantity);
        this.quantity = Math.addExact(this.quantity, quantity);
    }

    public void adjustQuantity(Integer quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
    }

    public boolean isAvailable() {
        return this.quantity > 0;
    }

    private static void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException(
                    "Quantity must be zero or greater"
            );
        }
    }

    private static void validateMovement(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }
    }
}