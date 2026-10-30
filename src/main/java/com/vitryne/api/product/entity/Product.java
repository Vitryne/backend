package com.vitryne.api.product.entity;

import com.vitryne.api.product.enums.ProductStatus;
import com.vitryne.api.shared.exception.*;
import com.vitryne.api.stock.entity.Stock;
import com.vitryne.api.store.entity.Store;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
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

    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 2)
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Positive
    @Digits(integer = 10, fraction = 2)
    @Column(name = "promotional_price", precision = 12, scale = 2)
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
    private Instant promotionalStartDate;

    @Column(name = "promotional_end_date")
    private Instant promotionalEndDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    @Column(name = "stock_warning_threshold")
    private Integer stockWarningThreshold;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "photo_urls", columnDefinition = "text[]")
    private List<String> photoUrls = new ArrayList<>();

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

    @Builder
    public Product(
            String name,
            String description,
            BigDecimal price,
            String type,
            String color,
            String status,
            Integer stockWarningThreshold,
            List<String> photoUrls,
            Store store
    ) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException(
                    "O nome do produto é obrigatório"
            );
        }

        if (description == null || description.isBlank()) {
            throw new InvalidInputException(
                    "A descrição do produto é obrigatória"
            );
        }

        if (price == null || price.signum() <= 0) {
            throw new InvalidInputException(
                    "O preço do produto deve ser maior que zero"
            );
        }

        BigDecimal normalizedPrice;

        try {
            normalizedPrice = price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new InvalidInputException(
                    "O preço deve ter no máximo duas casas decimais"
            );
        }

        if (normalizedPrice.precision() > 12) {
            throw new InvalidInputException(
                    "O preço deve ter no máximo dez dígitos inteiros"
            );
        }

        if (type == null || type.isBlank()) {
            throw new InvalidInputException(
                    "O tipo do produto é obrigatório"
            );
        }

        if (status == null || status.isBlank()) {
            throw new InvalidInputException(
                    "O status do produto é obrigatório"
            );
        }

        if (photoUrls == null || photoUrls.isEmpty()) {
            throw new InvalidInputException(
                    "A lista de imagens do produto é obrigatória"
            );
        }

        if (photoUrls.stream().anyMatch(
                url -> url == null || url.isBlank()
        )) {
            throw new InvalidInputException(
                    "As imagens não podem ter URLs vazias"
            );
        }

        if (store == null) {
            throw new InvalidInputException(
                    "A loja do produto é obrigatória"
            );
        }

        if (stockWarningThreshold != null && stockWarningThreshold < 0) {
            throw new InvalidInputException(
                    "O limite de alerta de estoque não pode ser negativo"
            );
        }

        this.name = name.trim();
        this.description = description.trim();
        this.price = normalizedPrice;
        this.type = type.trim();
        this.color = color;
        this.status = status;
        this.stockWarningThreshold = stockWarningThreshold;
        this.photoUrls = new ArrayList<>(photoUrls);
        this.store = store;
    }
    @PrePersist
    protected void onCreate(){
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = Instant.now();
    }


    public BigDecimal calculateFinalPrice(){

        if(isItInSale()){
            return promotionalPrice;
        }

        return price;
    }

    public boolean isNew() {
        Instant now = Instant.now();
        return createdAt != null
                && !createdAt.isAfter(now)
                &&createdAt.isAfter(now.minus(Duration.ofDays(7)));
    }
    public void configureSale(BigDecimal promoPrice, Instant startDate, Instant endDate){
        if(startDate != null && endDate != null && promoPrice != null){

            if(startDate.isAfter(endDate)){
                throw new InvalidStartDateException();
            }

            if(endDate.isBefore(Instant.now())){
                throw new InvalidEndDateException();
            }

            if (promoPrice.signum() <= 0 || promoPrice.compareTo(price) >= 0) {
                throw new InvalidPromotionalPriceException();
            }
            BigDecimal normalizedPrice;

            try {
                normalizedPrice = promoPrice.setScale(
                        2, RoundingMode.UNNECESSARY
                );
            } catch (ArithmeticException exception) {
                throw new InvalidPromotionalPriceException();
            }

            this.promotionalStartDate = startDate;
            this.promotionalEndDate = endDate;
            this.promotionalPrice = normalizedPrice;
        }else{
            throw new NullPromotionArgumentException();
        }
    }

    public boolean isItInSale(){
        Instant instant = Instant.now();
        if(promotionalStartDate != null && promotionalEndDate != null && promotionalPrice != null) {
            return !promotionalStartDate.isAfter(promotionalEndDate) && !instant.isBefore(promotionalStartDate) && !instant.isAfter(promotionalEndDate);
        } else{
            return false;
        }
    }

    public Boolean checkAvailability(String size){
        return findStockBySize(size).map(Stock::isAvailable).orElse(false);
    }

    public void updateDetails(
            String name,
            String description,
            BigDecimal price,
            String type,
            String color,
            Integer stockWarningThreshold,
            List<String> photoUrls){
        if (name == null || name.isBlank()) {
            throw new InvalidInputException(
                    "O nome do produto é obrigatório"
            );
        }

        if (description == null || description.isBlank()) {
            throw new InvalidInputException(
                    "A descrição do produto é obrigatória"
            );
        }

        if (price == null || price.signum() <= 0) {
            throw new InvalidInputException(
                    "O preço do produto deve ser maior que zero"
            );
        }

        BigDecimal normalizedPrice;

        try {
            normalizedPrice = price.setScale(
                    2, RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException exception) {
            throw new InvalidInputException(
                    "O preço deve ter no máximo duas casas decimais"
            );
        }

        if (normalizedPrice.precision() > 12) {
            throw new InvalidInputException(
                    "O preço deve ter no máximo dez dígitos inteiros"
            );
        }

        if (type == null || type.isBlank()) {
            throw new InvalidInputException(
                    "O tipo do produto é obrigatório"
            );
        }

        if (stockWarningThreshold != null && stockWarningThreshold < 0) {
            throw new InvalidInputException(
                    "O limite de alerta de estoque não pode ser negativo"
            );
        }

        if (photoUrls == null || photoUrls.isEmpty()) {
            throw new InvalidInputException(
                    "A lista de imagens do produto é obrigatória"
            );
        }

        if (photoUrls.stream().anyMatch(
                url -> url == null || url.isBlank()
        )) {
            throw new InvalidInputException(
                    "As imagens não podem ter URLs vazias"
            );
        }

        if(color == null || color.isBlank()){
            throw new InvalidInputException(
                    "A cor do produto não pode ser nula"
            );
        }

        Instant now = Instant.now();

        boolean hasUnexpiredSale = promotionalPrice != null
                        && promotionalStartDate != null
                        && promotionalEndDate != null
                        && !promotionalEndDate.isBefore(now);

        if(hasUnexpiredSale && normalizedPrice.compareTo(promotionalPrice) <= 0){
            throw new InvalidInputException(
                    "O preço normal deve ser maior que o preço promocional. "
                            + "Ajuste ou remova a promoção antes."
            );
        }

        this.name = name.trim();
        this.description = description.trim();
        this.price = normalizedPrice;
        this.type = type.trim();
        this.color = color;
        this.stockWarningThreshold = stockWarningThreshold;
        this.photoUrls = new ArrayList<>(photoUrls);
    }

    public void changeStatus(ProductStatus status){
        if(status == null){
            throw new InvalidInputException(
                    "O status do produto é obrigatório"
            );
        }

        this.status = status.name();
    }

    public Boolean checkAvailability(){
        return stocks.stream().anyMatch(Stock::isAvailable);
    }

    private java.util.Optional<Stock> findStockBySize(String size){
        return stocks.stream().filter(s -> s.getSize().equals(size)).findFirst();
    }

    public Stock addStock(String size, Integer quantity) {
        Stock stock = new Stock(this, size, quantity);

        boolean alreadyExists = stocks.stream()
                .anyMatch(existing ->
                        existing.getSize().equals(stock.getSize())
                );

        if (alreadyExists) {
            throw new IllegalArgumentException(
                    "Já existe estoque para o tamanho: " + stock.getSize()
            );
        }

        stocks.add(stock);

        return stock;
    }

}
