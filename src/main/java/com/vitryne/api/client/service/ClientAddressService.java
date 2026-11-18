package com.vitryne.api.client.service;

import com.vitryne.api.address.AddressNormalizer;
import com.vitryne.api.auth.dto.AddressRequestDTO;
import com.vitryne.api.client.dto.ClientAddressResponseDTO;
import com.vitryne.api.client.entity.Client;
import com.vitryne.api.client.entity.ClientAddress;
import com.vitryne.api.client.repository.ClientAddressRepository;
import com.vitryne.api.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ClientAddressService {

    private final ClientRepository clientRepository;
    private final ClientAddressRepository clientAddressRepository;

    @Transactional(readOnly = true)
    public List<ClientAddressResponseDTO> list(Long userId) {
        Long selectedId = selectedAddressId(findClient(userId));

        return clientAddressRepository.findAllByClientUserIdOrderByIdAsc(userId).stream()
                .map(address -> ClientAddressResponseDTO.from(address, selectedId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientAddressResponseDTO find(Long userId, Long addressId) {
        Client client = findClient(userId);
        return ClientAddressResponseDTO.from(findOwned(userId, addressId), selectedAddressId(client));
    }

    @Transactional
    public ClientAddressResponseDTO create(Long userId, AddressRequestDTO request) {
        Client client = findClient(userId);

        ClientAddress address = clientAddressRepository.saveAndFlush(
                new ClientAddress(client, AddressNormalizer.toAddress(request))
        );

        // Primeiro endereço do cliente passa a ser o selecionado.
        if (client.getSelectedAddress() == null) {
            client.selectAddress(address);
        }

        return ClientAddressResponseDTO.from(address, selectedAddressId(client));
    }

    @Transactional
    public ClientAddressResponseDTO update(Long userId, Long addressId, AddressRequestDTO request) {
        Client client = findClient(userId);
        ClientAddress address = findOwned(userId, addressId);

        address.changeAddress(AddressNormalizer.toAddress(request));
        clientAddressRepository.saveAndFlush(address);

        return ClientAddressResponseDTO.from(address, selectedAddressId(client));
    }

    @Transactional
    public ClientAddressResponseDTO select(Long userId, Long addressId) {
        Client client = findClient(userId);
        ClientAddress address = findOwned(userId, addressId);

        client.selectAddress(address);
        clientRepository.saveAndFlush(client);

        return ClientAddressResponseDTO.from(address, address.getId());
    }

    /**
     * Endereço de entrega do cliente: o selecionado ou, na falta dele, o primeiro da lista.
     */
    @Transactional(readOnly = true)
    public Optional<ClientAddress> findDeliveryAddress(Long userId) {
        return clientRepository.findByUserId(userId)
                .map(Client::getSelectedAddress)
                .or(() -> clientAddressRepository.findFirstByClientUserIdOrderByIdAsc(userId));
    }

    private Client findClient(Long userId) {
        return clientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente não encontrado para o usuário autenticado"
                ));
    }

    /** Endereço de outro cliente é tratado como inexistente (404). */
    private ClientAddress findOwned(Long userId, Long addressId) {
        return clientAddressRepository.findByIdAndClientUserId(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Endereço não encontrado"
                ));
    }

    private static Long selectedAddressId(Client client) {
        return client.getSelectedAddress() == null ? null : client.getSelectedAddress().getId();
    }
}
