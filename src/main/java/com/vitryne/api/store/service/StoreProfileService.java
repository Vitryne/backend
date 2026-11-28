package com.vitryne.api.store.service;

import com.vitryne.api.shared.dto.AddressResponseDTO;
import com.vitryne.api.store.dto.StoreProfileResponseDTO;
import com.vitryne.api.store.entity.Store;
import com.vitryne.api.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@Service
public class StoreProfileService {

    private final StoreRepository storeRepository;

    /** Loja do lojista do token; não há parâmetro de id, então não alcança loja de outro lojista. */
    @Transactional(readOnly = true)
    public StoreProfileResponseDTO me(Long ownerId) {
        Store store = storeRepository.findProfileByOwnerId(ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Loja não encontrada para o usuário autenticado"
                ));

        return new StoreProfileResponseDTO(
                store.getId(),
                store.getName(),
                store.getOwner().getEmail(),
                store.getPhotoUrl(),
                AddressResponseDTO.from(store.getAddress()),
                store.getMainCategory(),
                store.getDeliveryRadiusKm(),
                store.getPreparationTimeMinutes()
        );
    }
}
