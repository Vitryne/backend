package com.vitryne.api.entity;

import com.vitryne.api.exception.InsufficientStockException;
import jakarta.persistence.*;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "stock")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "size", nullable = false)
    private String size;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    public void decreaseStock(int quantity){
        if(quantity <= 0){
            throw new IllegalArgumentException("Invalid quantity");
        }

        if(this.quantity < quantity){
            throw new InsufficientStockException(this.size, this.quantity, quantity);
        }

        this.quantity -= quantity;
    }

    public void increaseStock(int quantity){
        if(quantity <= 0){
            throw new IllegalArgumentException("Invalid quantity");
        }

        this.quantity += quantity;
    }

    public Boolean isAvailable(){
        return this.quantity > 0;
    }
}
