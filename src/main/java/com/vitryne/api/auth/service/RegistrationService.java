package com.vitryne.api.auth.service;

import com.vitryne.api.address.entity.Address;
import com.vitryne.api.shared.entity.User;
import com.vitryne.api.auth.dto.AddressRequestDTO;
import com.vitryne.api.auth.dto.client.RegisterClientRequestDTO;
import com.vitryne.api.auth.dto.store.OpeningPeriodRequestDTO;
import com.vitryne.api.auth.dto.store.RegisterStoreRequestDTO;
import com.vitryne.api.auth.dto.store.StoreBankAccountRequestDTO;
import com.vitryne.api.client.entity.Client;
import com.vitryne.api.client.repository.ClientRepository;
import com.vitryne.api.shared.dto.RegistrationResponseDTO;
import com.vitryne.api.shared.enums.UserRole;
import com.vitryne.api.shared.repository.UserRepository;
import com.vitryne.api.store.entity.Store;
import com.vitryne.api.store.entity.StoreBankAccount;
import com.vitryne.api.store.entity.StoreOpeningPeriod;
import com.vitryne.api.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegistrationResponseDTO registerClient(
            RegisterClientRequestDTO request
    ) {
        String email = normalizeEmail(request.email());
        String cpf = normalizeDocument(request.cpf());

        requireFormat(cpf, "[0-9]{11}", "CPF deve conter 11 dígitos");

        ensureEmailAvailable(email);

        if (clientRepository.existsByCpf(cpf)) {
            throw conflict("CPF já cadastrado");
        }

        Address address = toAddress(request.address());

        User user = createUser(
                email,
                request.password(),
                UserRole.CONSUMER
        );

        Client client = new Client(
                user,
                request.name().trim(),
                cpf,
                address
        );

        clientRepository.saveAndFlush(client);

        return new RegistrationResponseDTO(
                user.getId(),
                client.getId(),
                null,
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public RegistrationResponseDTO registerStore(
            RegisterStoreRequestDTO request
    ) {
        String email = normalizeEmail(request.email());
        String cnpj = normalizeDocument(request.cnpj());

        requireFormat(
                cnpj,
                "[A-Z0-9]{14}",
                "CNPJ deve conter 14 caracteres alfanuméricos"
        );

        ensureEmailAvailable(email);

        if (storeRepository.existsByCnpj(cnpj)) {
            throw conflict("CNPJ já cadastrado");
        }

        Address address = toAddress(request.address());
        StoreBankAccount bankAccount = toBankAccount(request.bankAccount());

        validateOpeningPeriods(request.openingPeriods());

        List<StoreOpeningPeriod> openingPeriods =
                request.openingPeriods().stream()
                        .map(period -> StoreOpeningPeriod.builder()
                                .dayOfWeek(period.dayOfWeek())
                                .opensAt(period.opensAt())
                                .closesAt(period.closesAt())
                                .build())
                        .collect(Collectors.toCollection(ArrayList::new));

        Set<String> tags = request.tags() == null
                ? new LinkedHashSet<>()
                : request.tags().stream()
                  .map(String::trim)
                  .map(tag -> tag.toLowerCase(Locale.ROOT))
                  .collect(Collectors.toCollection(LinkedHashSet::new));

        User user = createUser(
                email,
                request.password(),
                UserRole.STORE
        );

        Store store = Store.builder()
                .owner(user)
                .name(request.name().trim())
                .cnpj(cnpj)
                .phone(request.phone().trim())
                .legalName(request.legalName().trim())
                .description(trimOrNull(request.description()))
                .address(address)
                .bankAccount(bankAccount)
                .mainCategory(request.mainCategory().trim())
                .tags(tags)
                .deliveryRadiusKm(request.deliveryRadiusKm())
                .preparationTimeMinutes(request.preparationTimeMinutes())
                .openingPeriods(openingPeriods)
                .photoUrl(trimOrNull(request.photoUrl()))
                .bannerUrl(trimOrNull(request.bannerUrl()))
                .build();

        storeRepository.saveAndFlush(store);

        return new RegistrationResponseDTO(
                user.getId(),
                null,
                store.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    private User createUser(String email, String password, UserRole role) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw badRequest("Senha deve ter no máximo 72 bytes em UTF-8");
        }

        User user = new User(
                email,
                passwordEncoder.encode(password),
                role
        );

        return userRepository.save(user);
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw conflict("E-mail já cadastrado");
        }
    }

    private Address toAddress(AddressRequestDTO request) {
        String zipCode = request.zipCode().trim().replace("-", "");

        requireFormat(
                zipCode,
                "[0-9]{8}",
                "CEP deve conter 8 dígitos"
        );

        return Address.builder()
                .zipCode(zipCode)
                .street(request.street().trim())
                .number(request.number().trim())
                .neighborhood(request.neighborhood().trim())
                .complement(trimOrNull(request.complement()))
                .city(request.city().trim())
                .state(request.state().trim().toUpperCase(Locale.ROOT))
                .build();
    }

    private StoreBankAccount toBankAccount(
            StoreBankAccountRequestDTO request
    ) {
        String document = normalizeDocument(request.holderDocument());

        requireFormat(
                document,
                "(?:[0-9]{11}|[A-Z0-9]{14})",
                "Documento do titular deve ter formato de CPF ou CNPJ"
        );

        return StoreBankAccount.builder()
                .holderName(request.holderName().trim())
                .holderDocument(document)
                .accountType(request.accountType())
                .bankCode(request.bankCode().trim())
                .agency(request.agency().trim())
                .accountNumber(request.accountNumber().trim())
                .pixKey(trimOrNull(request.pixKey()))
                .build();
    }

    private void validateOpeningPeriods(
            List<OpeningPeriodRequestDTO> periods
    ) {
        for (OpeningPeriodRequestDTO period : periods) {
            if (!period.opensAt().isBefore(period.closesAt())) {
                throw badRequest(
                        "Horário de abertura deve ser anterior ao fechamento"
                );
            }

            if (period.opensAt().getSecond() != 0
                    || period.closesAt().getSecond() != 0
                    || period.opensAt().getNano() != 0
                    || period.closesAt().getNano() != 0) {

                throw badRequest("Informe horários com precisão de minutos");
            }
        }

        var periodsByDay = periods.stream()
                .collect(Collectors.groupingBy(
                        OpeningPeriodRequestDTO::dayOfWeek
                ));

        for (var dayPeriods : periodsByDay.values()) {
            var sorted = dayPeriods.stream()
                    .sorted(Comparator.comparing(
                            OpeningPeriodRequestDTO::opensAt
                    ))
                    .toList();

            for (int i = 1; i < sorted.size(); i++) {
                var previous = sorted.get(i - 1);
                var current = sorted.get(i);

                if (current.opensAt().isBefore(previous.closesAt())) {
                    throw badRequest(
                            "Horários de funcionamento do mesmo dia se sobrepõem"
                    );
                }
            }
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDocument(String document) {
        return document.trim()
                .replaceAll("[.\\-/\\s]", "")
                .toUpperCase(Locale.ROOT);
    }

    private void requireFormat(
            String value,
            String regex,
            String message
    ) {
        if (!value.matches(regex)) {
            throw badRequest(message);
        }
    }

    private String trimOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}