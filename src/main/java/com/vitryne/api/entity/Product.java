package com.vitryne.api.entity;

import com.vitryne.api.exception.InvalidEndDateException;
import com.vitryne.api.exception.InvalidPromotionalPriceException;
import com.vitryne.api.exception.InvalidStartDateException;
import com.vitryne.api.exception.NullPromotionArgumentException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "product")
public class  Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "promotional_price")
    private BigDecimal promotionalPrice;

    @Column(name = "type")
    private String type;

    @Column(name = "color")
    private String color;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "status")
    private String status;

    @Column(name = "promotional_start_date")
    private LocalDateTime promotionalStartDate;

    @Column(name = "promotional_end_date")
    private LocalDateTime promotionalEndDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "stock_warning_threshold")
    private Integer stockWarningThreshold;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "photo_urls", columnDefinition = "text[]")
    private List<String> photoUrls;

    @Builder.Default
    @Setter(AccessLevel.NONE)
    @OneToMany(
            mappedBy = "product",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    private List<Stock> stocks = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /*@OneToMany(mappedBy = "produto")
    private List<Review> reviews;
    */

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    public BigDecimal calculateFinalPrice(){

        if(isItInSale()){
            return promotionalPrice;
        }

        return price;
    }

    public boolean isNew() {
        return createdAt != null
                && createdAt.isAfter(LocalDateTime.now().minusDays(7));
    }
    public void configureSale(BigDecimal promoPrice, LocalDateTime startDate, LocalDateTime endDate){
        if(startDate != null && endDate != null && promoPrice != null){

            if(startDate.isAfter(endDate)){
                throw new InvalidStartDateException();
            }

            if(endDate.isBefore(LocalDateTime.now())){
                throw new InvalidEndDateException();
            }

            if (promoPrice.signum() <= 0 || promoPrice.compareTo(price) >= 0) {
                throw new InvalidPromotionalPriceException();
            }
                this.promotionalStartDate = startDate;
                this.promotionalEndDate = endDate;
                this.promotionalPrice = promoPrice;
        }else{
            throw new NullPromotionArgumentException();
        }
    }

    public boolean isItInSale(){
        if(promotionalStartDate != null && promotionalEndDate != null && promotionalPrice != null) {
            return !promotionalStartDate.isAfter(promotionalEndDate) && !LocalDateTime.now().isBefore(promotionalStartDate) && !LocalDateTime.now().isAfter(promotionalEndDate);
        } else{
            return false;
        }
    }

    public Boolean checkAvailability(String size){
        return findStockBySize(size).map(Stock::isAvailable).orElse(false);
    }


    public Boolean checkAvailability(){
        return stocks.stream().anyMatch(Stock::isAvailable);
    }

    private java.util.Optional<Stock> findStockBySize(String size){
        return stocks.stream().filter(s -> s.getSize().equals(size)).findFirst();
    }

    public void addStock(String size, Integer quantity) {
        Stock stock = new Stock(this, size, quantity);

        boolean alreadyExists = stocks.stream()
                .anyMatch(existing ->
                        existing.getSize().equals(stock.getSize())
                );

        if (alreadyExists) {
            throw new IllegalArgumentException(
                    "Stock already exists for size: " + stock.getSize()
            );
        }

        stocks.add(stock);
    }

}
