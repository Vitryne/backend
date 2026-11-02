package com.vitryne.api.auth.service;

import com.vitryne.api.auth.dto.store.OpeningPeriodRequestDTO;
import com.vitryne.api.auth.dto.store.StoreBankAccountRequestDTO;
import com.vitryne.api.auth.enums.BankAccountType;
import com.vitryne.api.client.entity.Client;
import com.vitryne.api.client.repository.ClientRepository;
import com.vitryne.api.shared.dto.RegistrationResponseDTO;
import com.vitryne.api.shared.entity.User;
import com.vitryne.api.shared.enums.UserRole;
import com.vitryne.api.shared.repository.UserRepository;
import com.vitryne.api.store.entity.Store;
import com.vitryne.api.store.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static com.vitryne.api.support.RegistrationFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    private static final Instant CREATED_AT =
            Instant.parse("2026-10-24T15:00:00Z");

    private static final String ENCODED_PASSWORD = "hash-gerado-pelo-encoder";

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationService service;

    @Test
    void shouldRegisterClientWithNormalizedDataAndEncodedPassword() {
        var request = client(
                " CLIENTE@EXAMPLE.COM ",
                "111.111.111-11",
                "SenhaTeste123!"
        );

        prepareUserPersistence();

        when(clientRepository.saveAndFlush(any(
                Client.class)))
                .thenAnswer(invocation -> {
                    Client client = invocation.getArgument(0);
                    ReflectionTestUtils.setField(client, "id", 20L);
                    return client;
                });

        RegistrationResponseDTO response = service.registerClient(request);

        var userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("cliente@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
        assertThat(savedUser.getRole()).isEqualTo(UserRole.CONSUMER);
        assertThat(savedUser.isActive()).isTrue();

        var clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).saveAndFlush(clientCaptor.capture());

        Client savedClient = clientCaptor.getValue();

        assertThat(savedClient.getUser()).isSameAs(savedUser);
        assertThat(savedClient.getName()).isEqualTo("Cliente Teste");
        assertThat(savedClient.getCpf()).isEqualTo("11111111111");

        assertThat(savedClient.getAddress().getZipCode())
                .isEqualTo("87000000");

        assertThat(savedClient.getAddress().getStreet())
                .isEqualTo("Rua de Teste");

        assertThat(savedClient.getAddress().getState()).isEqualTo("PR");

        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.clientId()).isEqualTo(20L);
        assertThat(response.storeId()).isNull();
        assertThat(response.role()).isEqualTo(UserRole.CONSUMER);
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);

        verify(passwordEncoder).encode(request.password());
        verify(userRepository).existsByEmailIgnoreCase("cliente@example.com");
        verifyNoInteractions(storeRepository);
    }

    @Test
    void shouldRegisterStoreWithBankAccountAddressAndOpeningPeriods() {
        prepareUserPersistence();
        prepareStorePersistence();

        RegistrationResponseDTO response = service.registerStore(store());

        var storeCaptor = ArgumentCaptor.forClass(Store.class);
        verify(storeRepository).saveAndFlush(storeCaptor.capture());

        Store savedStore = storeCaptor.getValue();

        assertThat(savedStore.getOwner().getRole()).isEqualTo(UserRole.STORE);
        assertThat(savedStore.getOwner().getPasswordHash())
                .isEqualTo(ENCODED_PASSWORD);

        assertThat(savedStore.getName()).isEqualTo("Loja Teste");
        assertThat(savedStore.getCnpj()).isEqualTo("11111111111111");
        assertThat(savedStore.getLegalName()).isEqualTo("Loja Teste Ltda");
        assertThat(savedStore.isActive()).isTrue();

        assertThat(savedStore.getAddress().getZipCode()).isEqualTo("87000000");
        assertThat(savedStore.getAddress().getState()).isEqualTo("PR");

        assertThat(savedStore.getTags())
                .containsExactlyInAnyOrder("casual", "vestidos");

        assertThat(savedStore.getDeliveryRadiusKm())
                .isEqualByComparingTo("10.00");

        assertThat(savedStore.getPreparationTimeMinutes()).isEqualTo(30);
        assertThat(savedStore.getOpeningPeriods()).hasSize(2);
        assertThat(savedStore.getOpeningPeriods().get(0).getDayOfWeek())
                .isEqualTo(DayOfWeek.MONDAY);

        var account = savedStore.getBankAccount();

        assertThat(account).isNotNull();
        assertThat(account.getHolderName()).isEqualTo("Loja Teste Ltda");
        assertThat(account.getHolderDocument()).isEqualTo("11111111111111");
        assertThat(account.getAccountType()).isEqualTo(BankAccountType.CHECKING);
        assertThat(account.getBankCode()).isEqualTo("000");
        assertThat(account.getAgency()).isEqualTo("0001");
        assertThat(account.getAccountNumber()).isEqualTo("00123-4");
        assertThat(account.getPixKey()).isEqualTo("financeiro@example.com");

        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.storeId()).isEqualTo(30L);
        assertThat(response.clientId()).isNull();
        assertThat(response.role()).isEqualTo(UserRole.STORE);

        verifyNoInteractions(clientRepository);
    }

    @Test
    void shouldRejectClientWhenEmailIsAlreadyRegistered() {
        when(userRepository.existsByEmailIgnoreCase("cliente@example.com"))
                .thenReturn(true);

        var exception = catchThrowableOfType(
                () -> service.registerClient(client()),
                ResponseStatusException.class
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder, clientRepository, storeRepository);
    }

    @Test
    void shouldRejectStoreWhenEmailIsAlreadyRegistered() {
        when(userRepository.existsByEmailIgnoreCase("loja@example.com"))
                .thenReturn(true);

        var exception = catchThrowableOfType(
                () -> service.registerStore(store()),
                ResponseStatusException.class
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder, clientRepository, storeRepository);
    }

    @Test
    void shouldRejectDuplicatedCpf() {
        when(clientRepository.existsByCpf("11111111111"))
                .thenReturn(true);

        var exception = catchThrowableOfType(
                () -> service.registerClient(client()),
                ResponseStatusException.class
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        verify(userRepository, never()).save(any(User.class));
        verify(clientRepository, never()).saveAndFlush(any(Client.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldRejectDuplicatedCnpj() {
        when(storeRepository.existsByCnpj("11111111111111"))
                .thenReturn(true);

        var exception = catchThrowableOfType(
                () -> service.registerStore(store()),
                ResponseStatusException.class
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        verify(userRepository, never()).save(any(User.class));
        verify(storeRepository, never()).saveAndFlush(any(Store.class));
        verifyNoInteractions(passwordEncoder);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123",
            "111111111111",
            "1111111111A",
            "11111111111!"
    })
    void shouldRejectInvalidCpfFormat(String cpf) {
        var request = client("cliente@example.com", cpf, "SenhaTeste123!");

        assertBadRequest(() -> service.registerClient(request));

        verifyNoInteractions(
                userRepository,
                clientRepository,
                storeRepository,
                passwordEncoder
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "111111111111111", "1111111111111!"})
    void shouldRejectInvalidCnpjFormat(String cnpj) {
        var request = store(
                "loja@example.com",
                cnpj,
                bankAccount(),
                periods(),
                List.of()
        );

        assertBadRequest(() -> service.registerStore(request));

        verifyNoInteractions(
                userRepository,
                clientRepository,
                storeRepository,
                passwordEncoder
        );
    }

    @Test
    void shouldRejectInvalidBankHolderDocumentBeforeSavingUser() {
        var invalidAccount = new StoreBankAccountRequestDTO(
                "Titular",
                "123",
                BankAccountType.CHECKING,
                "000",
                "0001",
                "00123-4",
                null
        );

        var request = store(
                "loja@example.com",
                "11.111.111/1111-11",
                invalidAccount,
                periods(),
                List.of()
        );

        assertBadRequest(() -> service.registerStore(request));

        verify(userRepository, never()).save(any(User.class));
        verify(storeRepository, never()).saveAndFlush(any(Store.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldAllowCpfHolderDocumentAndMissingPixKey() {
        prepareUserPersistence();
        prepareStorePersistence();

        var account = new StoreBankAccountRequestDTO(
                "Titular Teste",
                "111.111.111-11",
                BankAccountType.PAYMENT,
                "000",
                "0001",
                "00123-4",
                " "
        );

        var request = store(
                "loja@example.com",
                "11.111.111/1111-11",
                account,
                periods(),
                null
        );

        service.registerStore(request);

        var captor = ArgumentCaptor.forClass(Store.class);
        verify(storeRepository).saveAndFlush(captor.capture());

        assertThat(captor.getValue().getBankAccount().getHolderDocument())
                .isEqualTo("11111111111");

        assertThat(captor.getValue().getBankAccount().getPixKey()).isNull();
        assertThat(captor.getValue().getTags()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidOpeningPeriods")
    void shouldRejectInvalidOpeningPeriods(
            List<OpeningPeriodRequestDTO> openingPeriods
    ) {
        var request = store(
                "loja@example.com",
                "11.111.111/1111-11",
                bankAccount(),
                openingPeriods,
                List.of()
        );

        assertBadRequest(() -> service.registerStore(request));

        verify(userRepository, never()).save(any(User.class));
        verify(storeRepository, never()).saveAndFlush(any(Store.class));
        verifyNoInteractions(passwordEncoder);
    }

    static Stream<List<OpeningPeriodRequestDTO>> invalidOpeningPeriods() {
        return Stream.of(
                // Abertura igual ao fechamento.
                List.of(period(DayOfWeek.MONDAY, "08:00", "08:00")),

                // Atravessa meia-noite, fora do escopo do MVP.
                List.of(period(DayOfWeek.MONDAY, "22:00", "02:00")),

                // Sobreposição: o segundo intervalo começa antes do primeiro acabar.
                List.of(
                        period(DayOfWeek.MONDAY, "08:00", "12:00"),
                        period(DayOfWeek.MONDAY, "11:00", "18:00")
                ),

                // Intervalo inteiramente contido no outro.
                List.of(
                        period(DayOfWeek.MONDAY, "08:00", "18:00"),
                        period(DayOfWeek.MONDAY, "10:00", "12:00")
                ),

                // Segundos não são aceitos.
                List.of(period(DayOfWeek.MONDAY, "08:00:01", "12:00")),

                // Frações de segundo não são aceitas.
                List.of(period(DayOfWeek.MONDAY, "08:00:00.001", "12:00"))
        );
    }

    @Test
    void shouldAllowAdjacentIntervalsAndSameTimesOnDifferentDays() {
        prepareUserPersistence();
        prepareStorePersistence();

        var request = store(
                "loja@example.com",
                "11.111.111/1111-11",
                bankAccount(),
                List.of(
                        // Ordem de entrada não precisa ser cronológica.
                        period(DayOfWeek.MONDAY, "12:00", "18:00"),
                        period(DayOfWeek.TUESDAY, "08:00", "12:00"),
                        period(DayOfWeek.MONDAY, "08:00", "12:00")
                ),
                List.of()
        );

        assertThatCode(() -> service.registerStore(request))
                .doesNotThrowAnyException();

        verify(storeRepository).saveAndFlush(any(Store.class));
    }

    @Test
    void shouldRejectPasswordExceeding72Utf8Bytes() {
        // 37 caracteres, mas 74 bytes em UTF-8.
        String password = "á".repeat(37);

        var request = client(
                "cliente@example.com",
                "111.111.111-11",
                password
        );

        assertBadRequest(() -> service.registerClient(request));

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any(User.class));
        verify(clientRepository, never()).saveAndFlush(any(Client.class));
    }

    @Test
    void shouldAcceptPasswordAt72Utf8BytesWithoutChangingIt() {
        prepareUserPersistence();

        String password = "á".repeat(36);

        service.registerClient(client(
                "cliente@example.com",
                "111.111.111-11",
                password
        ));

        verify(passwordEncoder).encode(password);
        verify(clientRepository).saveAndFlush(any(Client.class));
    }

    @Test
    void shouldNotTrimOrLowercasePassword() {
        prepareUserPersistence();

        String password = "  MinhaSenhaABC123!  ";

        service.registerClient(client(
                "cliente@example.com",
                "111.111.111-11",
                password
        ));

        verify(passwordEncoder).encode(password);
    }

    @Test
    void shouldPropagateDatabaseFailureInsteadOfReturningSuccess() {
        prepareUserPersistence();

        var databaseException = new DataIntegrityViolationException(
                "Simulated unique constraint violation"
        );

        when(clientRepository.saveAndFlush(any(Client.class)))
                .thenThrow(databaseException);

        assertThatThrownBy(() -> service.registerClient(client()))
                .isSameAs(databaseException);
    }

    private void prepareUserPersistence() {
        when(passwordEncoder.encode(anyString()))
                .thenReturn(ENCODED_PASSWORD);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);

                    // Simula os valores normalmente preenchidos pelo JPA/banco.
                    ReflectionTestUtils.setField(user, "id", 10L);
                    ReflectionTestUtils.setField(user, "createdAt", CREATED_AT);

                    return user;
                });
    }

    private void prepareStorePersistence() {
        when(storeRepository.saveAndFlush(any(Store.class)))
                .thenAnswer(invocation -> {
                    Store store = invocation.getArgument(0);
                    ReflectionTestUtils.setField(store, "id", 30L);
                    return store;
                });
    }

    private void assertBadRequest(Runnable action) {
        var exception = catchThrowableOfType(
                action::run,
                ResponseStatusException.class
        );

        assertThat(exception.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}