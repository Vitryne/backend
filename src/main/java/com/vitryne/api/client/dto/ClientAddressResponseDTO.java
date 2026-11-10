package com.vitryne.api.client.dto;

import com.vitryne.api.address.entity.Address;
import com.vitryne.api.client.entity.ClientAddress;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ClientAddressResponseDTO(
        Long id,
        String zipCode,
        String street,
        String number,
        String neighborhood,
        String complement,
        String city,
        String state,

        @Schema(description = "Endereço selecionado para entrega (navegação e checkout)")
        boolean selected,

        Instant createdAt,
        Instant updatedAt
) {
    public static ClientAddressResponseDTO from(ClientAddress clientAddress, Long selectedAddressId) {
        Address address = clientAddress.getAddress();

        return new ClientAddressResponseDTO(
                clientAddress.getId(),
                address.getZipCode(),
                address.getStreet(),
                address.getNumber(),
                address.getNeighborhood(),
                address.getComplement(),
                address.getCity(),
                address.getState(),
                clientAddress.getId().equals(selectedAddressId),
                clientAddress.getCreatedAt(),
                clientAddress.getUpdatedAt()
        );
    }
}
