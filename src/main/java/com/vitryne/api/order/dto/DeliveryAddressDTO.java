package com.vitryne.api.order.dto;

import com.vitryne.api.address.entity.Address;

public record DeliveryAddressDTO(
        String zipCode,
        String street,
        String number,
        String neighborhood,
        String complement,
        String city,
        String state
) {
    public static DeliveryAddressDTO from(Address address) {
        if (address == null) {
            return null;
        }

        return new DeliveryAddressDTO(
                address.getZipCode(),
                address.getStreet(),
                address.getNumber(),
                address.getNeighborhood(),
                address.getComplement(),
                address.getCity(),
                address.getState()
        );
    }
}
