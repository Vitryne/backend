package com.vitryne.api.store.entity;

import com.vitryne.api.address.entity.Address;
import jakarta.persistence.*;
import lombok.*;
import com.vitryne.api.shared.entity.User;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "store")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "cnpj", length = 14)
    private String cnpj;

    @Column(name = "phone", length = 25)
    private String phone;

    @Column(name = "legal_name", length = 255)
    private String legalName;

    @Column(name = "description", length = 2000)
    private String description;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Embedded
    private Address address;

    @OneToOne(
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @JoinColumn(name = "bank_account_id", unique = true)
    private StoreBankAccount bankAccount;

    @Column(name = "main_category", length = 100)
    private String mainCategory;

    @Column(name = "delivery_radius_km", precision = 6, scale = 2)
    private BigDecimal deliveryRadiusKm;

    @Column(name = "preparation_time_minutes")
    private Integer preparationTimeMinutes;

    @Column(name = "photo_url", length = 2048)
    private String photoUrl;

    @Column(name = "banner_url", length = 2048)
    private String bannerUrl;

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "store_tag",
            joinColumns = @JoinColumn(name = "store_id")
    )
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "store_opening_period",
            joinColumns = @JoinColumn(name = "store_id")
    )
    private List<StoreOpeningPeriod> openingPeriods = new ArrayList<>();
}