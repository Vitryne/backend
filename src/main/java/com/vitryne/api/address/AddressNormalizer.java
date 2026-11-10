package com.vitryne.api.address;

import com.vitryne.api.address.entity.Address;
import com.vitryne.api.auth.dto.AddressRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

/**
 * Regras únicas de normalização e validação de endereço,
 * usadas no cadastro (cliente e loja) e na gestão de endereços do cliente.
 */
public final class AddressNormalizer {

    public static final Set<String> STATES = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA",
            "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    private AddressNormalizer() {
    }

    public static Address toAddress(AddressRequestDTO request) {
        String zipCode = request.zipCode().trim().replace("-", "");

        if (!zipCode.matches("[0-9]{8}")) {
            throw badRequest("CEP deve conter 8 dígitos");
        }

        String state = request.state().trim().toUpperCase(Locale.ROOT);

        if (!STATES.contains(state)) {
            throw badRequest("UF inválida");
        }

        return Address.builder()
                .zipCode(zipCode)
                .street(request.street().trim())
                .number(request.number().trim())
                .neighborhood(request.neighborhood().trim())
                .complement(trimOrNull(request.complement()))
                .city(request.city().trim())
                .state(state)
                .build();
    }

    private static String trimOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
