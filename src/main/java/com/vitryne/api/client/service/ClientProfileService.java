package com.vitryne.api.client.service;

import com.vitryne.api.client.dto.ClientAddressResponseDTO;
import com.vitryne.api.client.dto.ClientProfileResponseDTO;
import com.vitryne.api.client.entity.Client;
import com.vitryne.api.client.entity.ClientAddress;
import com.vitryne.api.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@Service
public class ClientProfileService {

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public ClientProfileResponseDTO me(Long userId) {
        Client client = clientRepository.findProfileByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente não encontrado para o usuário autenticado"
                ));

        ClientAddress selected = client.getSelectedAddress();

        return new ClientProfileResponseDTO(
                client.getName(),
                client.getUser().getEmail(),
                selected == null ? null : ClientAddressResponseDTO.from(selected, selected.getId())
        );
    }
}
