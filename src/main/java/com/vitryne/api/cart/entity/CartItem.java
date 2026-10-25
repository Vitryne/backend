package com.vitryne.api.cart.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "cart_item")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Positive
    @Digits(integer = 10, fraction = 2)
    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;
}
