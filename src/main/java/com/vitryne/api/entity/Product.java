package com.vitryne.api.entity;

import com.vitryne.api.exception.InsufficientStockException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price")
    private Double price;

    @Column(name = "promotional_price")
    private Double promotionalPrice;

    @Column(name = "type")
    private String type;

    @Column(name = "color")
    private String color;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "status")
    private String status;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "photo_urls", columnDefinition = "text[]")
    private List<String> photoUrls;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Stock> stocks;

    //relate product to a Store in the future


    public Double calculateFinalPrice(){
        return (promotionalPrice != null) ? promotionalPrice : price;
    }

    public void applyDiscount(Double percentage){
        if(percentage == null || percentage <= 0 || percentage >= 100){
            throw new IllegalArgumentException("Invalid discount percentage");
        }

        this.promotionalPrice = this.price * (1 - percentage / 100);
    }

    public void removeDiscount(){
        this.promotionalPrice = null;
    }

    public Boolean checkAvailability(String size){
        return findStockBySize(size).map(Stock::isAvailable).orElse(false);
    }

    public void restockBySize(String size, Integer quantity) {
        Stock stock = findStockBySize(size)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Size not registered for this product: " + size));

        stock.increaseStock(quantity);
    }

    public void decreaseStockBySize(String size, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Invalid quantity for stock decrease: " + quantity);
        }

        Stock stock = findStockBySize(size)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Size not registered for this product: " + size));

        if (stock.getQuantity() < quantity) {
            throw new InsufficientStockException(size, stock.getQuantity(), quantity);
        }

        stock.decreaseStock(quantity);
    }

    public void registerSize(String size, Integer initialQuantity) {
        if (findStockBySize(size).isPresent()) {
            throw new IllegalArgumentException("Size already registered: " + size);
        }
        if (initialQuantity == null || initialQuantity < 0) {
            throw new IllegalArgumentException("Invalid quantity: " + initialQuantity);
        }

        Stock newStock = Stock.builder()
                .product(this)
                .size(size)
                .quantity(initialQuantity)
                .build();

        this.stocks.add(newStock);
    }

    private java.util.Optional<Stock> findStockBySize(String size){
        return stocks.stream().filter(s -> s.getSize().equals(size)).findFirst();
    }

}
