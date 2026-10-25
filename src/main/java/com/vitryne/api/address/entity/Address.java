package com.vitryne.api.address.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class Address {

    @Column(name = "zip_code", length = 8)
    private String zipCode;

    @Column(name = "street", length = 200)
    private String street;

    @Column(name = "address_number", length = 20)
    private String number;

    @Column(name = "neighborhood", length = 100)
    private String neighborhood;

    @Column(name = "complement", length = 150)
    private String complement;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 2)
    private String state;
}