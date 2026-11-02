package com.vitryne.api.auth.dto;


import com.vitryne.api.auth.dto.client.RegisterClientRequestDTO;
import com.vitryne.api.auth.dto.store.OpeningPeriodRequestDTO;
import com.vitryne.api.auth.dto.store.RegisterStoreRequestDTO;
import com.vitryne.api.auth.dto.store.StoreBankAccountRequestDTO;
import com.vitryne.api.auth.enums.BankAccountType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.vitryne.api.support.RegistrationFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class RegistrationRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void shouldAcceptValidClientRequest() {
        assertThat(validator.validate(client())).isEmpty();
    }

    @Test
    void shouldAcceptValidStoreRequest() {
        assertThat(validator.validate(store())).isEmpty();
    }

    @Test
    void shouldRejectInvalidClientFields() {
        var request = new RegisterClientRequestDTO(
                "",
                "",
                "email-invalido",
                "123",
                null
        );

        assertThat(invalidFields(request))
                .contains("name", "cpf", "email", "password", "address");
    }

    @Test
    void shouldValidateNestedAddress() {
        var invalidAddress = new AddressRequestDTO(
                "",
                "",
                "",
                "",
                null,
                "",
                "PARANA"
        );

        var request = new RegisterClientRequestDTO(
                "Cliente Teste",
                "111.111.111-11",
                "cliente@example.com",
                "SenhaTeste123!",
                invalidAddress
        );

        assertThat(invalidFields(request)).contains(
                "address.zipCode",
                "address.street",
                "address.number",
                "address.neighborhood",
                "address.city",
                "address.state"
        );
    }

    @Test
    void shouldRequireBankAccountForStoreRegistration() {
        var request = store(
                "loja@example.com",
                "11.111.111/1111-11",
                null,
                periods(),
                List.of()
        );

        assertThat(invalidFields(request)).contains("bankAccount");
    }

    @Test
    void shouldValidateNestedBankAccount() {
        var invalidAccount = new StoreBankAccountRequestDTO(
                "",
                "",
                null,
                "",
                "",
                "",
                null
        );

        var request = store(
                "loja@example.com",
                "11.111.111/1111-11",
                invalidAccount,
                periods(),
                List.of()
        );

        assertThat(invalidFields(request)).contains(
                "bankAccount.holderName",
                "bankAccount.holderDocument",
                "bankAccount.accountType",
                "bankAccount.bankCode",
                "bankAccount.agency",
                "bankAccount.accountNumber"
        );
    }

    @Test
    void shouldAllowMissingPixKey() {
        var account = new StoreBankAccountRequestDTO(
                "Titular Teste",
                "111.111.111-11",
                BankAccountType.CHECKING,
                "000",
                "0001",
                "00123-4",
                null
        );

        var request = store(
                "loja@example.com",
                "11.111.111/1111-11",
                account,
                periods(),
                List.of()
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void shouldRejectEmptyOpeningPeriods() {
        var request = store(
                "loja@example.com",
                "11.111.111/1111-11",
                bankAccount(),
                List.of(),
                List.of()
        );

        assertThat(invalidFields(request)).contains("openingPeriods");
    }

    @Test
    void shouldValidateEachOpeningPeriod() {
        var invalidPeriod = new OpeningPeriodRequestDTO(
                null,
                null,
                null
        );

        var request = store(
                "loja@example.com",
                "11.111.111/1111-11",
                bankAccount(),
                List.of(invalidPeriod),
                List.of()
        );

        assertThat(invalidFields(request)).contains(
                "openingPeriods[0].dayOfWeek",
                "openingPeriods[0].opensAt",
                "openingPeriods[0].closesAt"
        );
    }

    @Test
    void shouldRejectInvalidDeliverySettings() {
        var original = store();

        var request = new RegisterStoreRequestDTO(
                original.email(),
                original.password(),
                original.name(),
                original.cnpj(),
                original.phone(),
                original.legalName(),
                original.description(),
                original.address(),
                original.bankAccount(),
                original.mainCategory(),
                original.tags(),
                java.math.BigDecimal.ZERO,
                0,
                original.openingPeriods(),
                original.photoUrl(),
                original.bannerUrl()
        );

        assertThat(invalidFields(request))
                .contains("deliveryRadiusKm", "preparationTimeMinutes");
    }

    private Set<String> invalidFields(Object request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}