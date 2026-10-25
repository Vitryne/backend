package com.vitryne.api.support;


import com.vitryne.api.auth.dto.AddressRequestDTO;
import com.vitryne.api.auth.dto.client.RegisterClientRequestDTO;
import com.vitryne.api.auth.dto.store.OpeningPeriodRequestDTO;
import com.vitryne.api.auth.dto.store.RegisterStoreRequestDTO;
import com.vitryne.api.auth.dto.store.StoreBankAccountRequestDTO;
import com.vitryne.api.auth.enums.BankAccountType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public final class RegistrationFixtures {

    private RegistrationFixtures() {
    }

    public static AddressRequestDTO address() {
        return new AddressRequestDTO(
                "87000-000",
                " Rua de Teste ",
                "123A",
                " Centro ",
                " Apartamento 10 ",
                " Maringá ",
                "pr"
        );
    }

    public static RegisterClientRequestDTO client() {
        return client(
                "cliente@example.com",
                "111.111.111-11",
                "SenhaTeste123!"
        );
    }

    public static RegisterClientRequestDTO client(
            String email,
            String cpf,
            String password
    ) {
        return new RegisterClientRequestDTO(
                " Cliente Teste ",
                cpf,
                email,
                password,
                address()
        );
    }

    public static StoreBankAccountRequestDTO bankAccount() {
        return new StoreBankAccountRequestDTO(
                " Loja Teste Ltda ",
                "11.111.111/1111-11",
                BankAccountType.CHECKING,
                "000",
                "0001",
                "00123-4",
                " financeiro@example.com "
        );
    }

    public static OpeningPeriodRequestDTO period(
            DayOfWeek day,
            String opensAt,
            String closesAt
    ) {
        return new OpeningPeriodRequestDTO(
                day,
                LocalTime.parse(opensAt),
                LocalTime.parse(closesAt)
        );
    }

    public static List<OpeningPeriodRequestDTO> periods() {
        return List.of(
                period(DayOfWeek.MONDAY, "08:00", "12:00"),
                period(DayOfWeek.MONDAY, "13:30", "18:00")
        );
    }

    public static RegisterStoreRequestDTO store() {
        return store(
                "loja@example.com",
                "11.111.111/1111-11",
                bankAccount(),
                periods(),
                List.of(" Casual ", "casual", "Vestidos")
        );
    }

    public static RegisterStoreRequestDTO store(
            String email,
            String cnpj,
            StoreBankAccountRequestDTO bankAccount,
            List<OpeningPeriodRequestDTO> openingPeriods,
            List<String> tags
    ) {
        return new RegisterStoreRequestDTO(
                email,
                "SenhaTeste123!",
                " Loja Teste ",
                cnpj,
                "(44) 99999-9999",
                " Loja Teste Ltda ",
                " Loja de demonstração ",
                address(),
                bankAccount,
                " Moda feminina ",
                tags,
                new BigDecimal("10.00"),
                30,
                openingPeriods,
                "https://example.com/loja.jpg",
                "https://example.com/banner.jpg"
        );
    }
}