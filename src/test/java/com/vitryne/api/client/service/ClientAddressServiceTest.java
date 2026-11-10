package com.vitryne.api.client.service;

import com.vitryne.api.address.entity.Address;
import com.vitryne.api.auth.dto.AddressRequestDTO;
import com.vitryne.api.client.dto.ClientAddressResponseDTO;
import com.vitryne.api.client.entity.Client;
import com.vitryne.api.client.entity.ClientAddress;
import com.vitryne.api.client.repository.ClientAddressRepository;
import com.vitryne.api.client.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientAddressServiceTest {

    private static final Long USER_ID = 10L;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientAddressRepository clientAddressRepository;

    @InjectMocks
    private ClientAddressService service;

    private Client client;
    private ClientAddress home;

    @BeforeEach
    void setUp() {
        client = new Client(null, "Cliente", "11111111111");
        ReflectionTestUtils.setField(client, "id", 20L);
        home = address(1L, "Rua de Casa");
        client.selectAddress(home);
        lenient().when(clientRepository.findByUserId(USER_ID)).thenReturn(Optional.of(client));
    }

    @Test
    void deveListarEnderecosIndicandoOSelecionado() {
        ClientAddress work = address(2L, "Rua do Trabalho");
        when(clientAddressRepository.findAllByClientUserIdOrderByIdAsc(USER_ID))
                .thenReturn(List.of(home, work));

        List<ClientAddressResponseDTO> addresses = service.list(USER_ID);

        assertThat(addresses).extracting(ClientAddressResponseDTO::id).containsExactly(1L, 2L);
        assertThat(addresses).extracting(ClientAddressResponseDTO::selected).containsExactly(true, false);
    }

    @Test
    void deveCadastrarEnderecoNormalizadoSemTrocarOSelecionado() {
        when(clientAddressRepository.saveAndFlush(any(ClientAddress.class))).thenAnswer(i -> {
            ClientAddress saved = i.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 3L);
            return saved;
        });

        ClientAddressResponseDTO created = service.create(USER_ID, request(" 87000-000 ", "pr"));

        assertThat(created.id()).isEqualTo(3L);
        assertThat(created.zipCode()).isEqualTo("87000000");
        assertThat(created.state()).isEqualTo("PR");
        assertThat(created.selected()).isFalse();
        assertThat(client.getSelectedAddress()).isSameAs(home);
    }

    @Test
    void primeiroEnderecoDoClienteDeveFicarSelecionado() {
        ReflectionTestUtils.setField(client, "selectedAddress", null);
        when(clientAddressRepository.saveAndFlush(any(ClientAddress.class))).thenAnswer(i -> {
            ClientAddress saved = i.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 3L);
            return saved;
        });

        ClientAddressResponseDTO created = service.create(USER_ID, request("87000000", "PR"));

        assertThat(created.selected()).isTrue();
        assertThat(client.getSelectedAddress().getId()).isEqualTo(3L);
    }

    @Test
    void deveRejeitarCepOuUfInvalidos() {
        assertThatThrownBy(() -> service.create(USER_ID, request("8700", "PR")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CEP");

        assertThatThrownBy(() -> service.create(USER_ID, request("87000000", "XX")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("UF");

        verify(clientAddressRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveEditarEnderecoDoProprioCliente() {
        when(clientAddressRepository.findByIdAndClientUserId(1L, USER_ID)).thenReturn(Optional.of(home));

        ClientAddressResponseDTO updated = service.update(USER_ID, 1L, request("86000000", "SP"));

        assertThat(updated.zipCode()).isEqualTo("86000000");
        assertThat(updated.state()).isEqualTo("SP");
        assertThat(updated.selected()).isTrue();
        verify(clientAddressRepository).saveAndFlush(home);
    }

    @Test
    void enderecoDeOutroClienteDeveRetornarNaoEncontrado() {
        when(clientAddressRepository.findByIdAndClientUserId(99L, USER_ID)).thenReturn(Optional.empty());

        for (Runnable call : List.<Runnable>of(
                () -> service.find(USER_ID, 99L),
                () -> service.update(USER_ID, 99L, request("87000000", "PR")),
                () -> service.select(USER_ID, 99L)
        )) {
            assertThatThrownBy(call::run)
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        verify(clientAddressRepository, never()).saveAndFlush(any());
        verify(clientRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveSelecionarOutroEndereco() {
        ClientAddress work = address(2L, "Rua do Trabalho");
        when(clientAddressRepository.findByIdAndClientUserId(2L, USER_ID)).thenReturn(Optional.of(work));

        ClientAddressResponseDTO selected = service.select(USER_ID, 2L);

        assertThat(selected.selected()).isTrue();
        assertThat(client.getSelectedAddress()).isSameAs(work);
    }

    @Test
    void enderecoDeEntregaDeveSerOSelecionadoOuOPrimeiro() {
        assertThat(service.findDeliveryAddress(USER_ID)).containsSame(home);

        ReflectionTestUtils.setField(client, "selectedAddress", null);
        ClientAddress first = address(5L, "Primeira");
        when(clientAddressRepository.findFirstByClientUserIdOrderByIdAsc(USER_ID)).thenReturn(Optional.of(first));

        assertThat(service.findDeliveryAddress(USER_ID)).containsSame(first);
    }

    private ClientAddress address(Long id, String street) {
        ClientAddress address = new ClientAddress(client, Address.builder()
                .zipCode("87000000").street(street).number("1").neighborhood("Centro")
                .city("Maringá").state("PR").build());
        ReflectionTestUtils.setField(address, "id", id);
        return address;
    }

    private AddressRequestDTO request(String zipCode, String state) {
        return new AddressRequestDTO(zipCode, "Rua Nova", "10", "Centro", null, "Maringá", state);
    }
}
