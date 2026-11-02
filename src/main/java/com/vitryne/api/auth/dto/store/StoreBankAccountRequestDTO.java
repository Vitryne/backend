package com.vitryne.api.auth.dto.store;

import com.vitryne.api.auth.enums.BankAccountType;
import jakarta.validation.constraints.*;

public record StoreBankAccountRequestDTO(
        @NotBlank @Size(max = 150)
        String holderName,

        @NotBlank @Size(max = 20)
        String holderDocument,

        @NotNull
        BankAccountType accountType,

        @NotBlank @Size(max = 20)
        String bankCode,

        @NotBlank @Size(max = 20)
        String agency,

        @NotBlank @Size(max = 30)
        String accountNumber,

        @Size(max = 255)
        String pixKey
) {
    @Override
    public String toString() {
        return "StoreBankAccountRequestDTO[REDACTED]";
    }
}