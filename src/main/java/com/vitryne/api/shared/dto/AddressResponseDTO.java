package com.vitryne.api.shared.dto;

import com.vitryne.api.address.entity.Address;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Endereço")
public record AddressResponseDTO(
        @Schema(example = "87000000") String zipCode,
        @Schema(example = "Avenida Brasil") String street,
        @Schema(example = "1000") String number,
        @Schema(example = "Centro") String neighborhood,
        @Schema(example = "Sala 2") String complement,
        @Schema(example = "Maringá") String city,
        @Schema(example = "PR") String state
) {

    public static AddressResponseDTO from(Address address) {
        if (address == null) {
            return null;
        }

        return new AddressResponseDTO(
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
