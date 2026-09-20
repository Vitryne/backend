package com.vitryne.api.entity;

import com.vitryne.api.exception.NullPromotionArgument;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "photo_urls", columnDefinition = "text[]")
    private List<String> photoUrls;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Stock> stocks;

    /*@OneToMany(mappedBy = "produto")
    private List<Review> reviews;

    @OneToOne(mappedBy = "store_id")
    private Store storeId;*/


    public BigDecimal calculateFinalPrice(){

        if(isItInSale()){
            return promotionalPrice;
        }

        return price;
    }

    public boolean isNew(){
        return createdAt.isAfter(LocalDateTime.now().minusDays(7));
    }

    public void configureSale(BigDecimal promoPrice, LocalDateTime startDate, LocalDateTime endDate){
        if(startDate != null && endDate != null && promoPrice != null){
            if(&& promoPrice.compareTo(price) < 0){
                this.promotionalStartDate = startDate;
                this.promotionalEndDate = endDate;
                this.promotionalPrice = promoPrice;
            }
        }else{
            throw new NullPromotionArgument();
        }
    }

    public boolean isItInSale(){
        if(promotionalStartDate != null && promotionalEndDate != null && promotionalPrice != null) {
            return !promotionalStartDate.isAfter(promotionalEndDate) && !LocalDateTime.now().isAfter(promotionalStartDate) && !LocalDateTime.now().isAfter(promotionalEndDate);
        } else{
            return false;
        }
    }

    public Boolean checkAvailability(String size){
        return findStockBySize(size).map(Stock::isAvailable).orElse(false);
    }


    private java.util.Optional<Stock> findStockBySize(String size){
        return stocks.stream().filter(s -> s.getSize().equals(size)).findFirst();
    }

}
