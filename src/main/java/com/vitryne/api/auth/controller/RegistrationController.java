package com.vitryne.api.auth.controller;


import com.vitryne.api.auth.dto.client.RegisterClientRequestDTO;
import com.vitryne.api.auth.dto.store.RegisterStoreRequestDTO;
import com.vitryne.api.auth.service.RegistrationService;
import com.vitryne.api.documentation.RegistrationExamples;
import com.vitryne.api.shared.dto.ApiErrorResponseDTO;
import com.vitryne.api.shared.dto.RegistrationResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/register")
@Tag(
        name = "Cadastros",
        description = "Cadastro público de clientes e lojistas"
)
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = """
            Dados inválidos. Falhas de validação podem incluir o mapa fields.
            Erros de regras de negócio retornam message.
            """,
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiErrorResponseDTO.class),
                        examples = {
                                @ExampleObject(
                                        name = "Validação de campos",
                                        value = RegistrationExamples.VALIDATION_ERROR
                                ),
                                @ExampleObject(
                                        name = "Regra de negócio",
                                        value = RegistrationExamples.BUSINESS_ERROR
                                )
                        }
                )
        ),
        @ApiResponse(
                responseCode = "409",
                description = """
            E-mail, CPF ou CNPJ já cadastrado,
            ou conflito com restrições do banco.
            """,
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ApiErrorResponseDTO.class),
                        examples = @ExampleObject(
                                value = RegistrationExamples.CONFLICT_ERROR
                        )
                )
        )
})
public class RegistrationController {

    private final RegistrationService registrationService;

    @Operation(
            operationId = "registerClient",
            summary = "Cadastrar cliente",
            description = """
            Cria usuário com perfil CONSUMER, cliente e endereço
            na mesma transação.

            Não exige autenticação e não retorna token.
            O CPF é normalizado e recebe validação básica de formato.
            O frontend deve validar a confirmação da senha.
            """,
            requestBody =
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = RegisterClientRequestDTO.class
                            ),
                            examples = @ExampleObject(
                                    name = "Cadastro de cliente",
                                    value = RegistrationExamples.CLIENT_REQUEST
                            )
                    )
            )
    )
    @ApiResponse(
            responseCode = "201",
            description = "Cliente cadastrado",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            implementation = RegistrationResponseDTO.class
                    ),
                    examples = @ExampleObject(
                            value = RegistrationExamples.CLIENT_RESPONSE
                    )
            )
    )
    @PostMapping(
            value = "/client",
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<RegistrationResponseDTO> registerClient(
            @Valid @RequestBody RegisterClientRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.registerClient(request));
    }

    @Operation(
            operationId = "registerStore",
            summary = "Cadastrar lojista e loja",
            description = """
            Cria usuário com perfil STORE, loja, endereço,
            conta bancária, tags e horários na mesma transação.

            Não exige autenticação e não retorna token.
            Dados bancários não são verificados externamente.

            accountType:
            CHECKING = corrente;
            SAVINGS = poupança;
            PAYMENT = pagamento.

            openingPeriods aceita múltiplos intervalos por dia.
            Dia sem intervalos significa fechado.
            Não são permitidos intervalos sobrepostos,
            horários atravessando meia-noite ou precisão abaixo de minutos.

            tags, pixKey, description, complement, photoUrl e bannerUrl
            são opcionais. A conta bancária é obrigatória neste contrato.
            """,
            requestBody =
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = RegisterStoreRequestDTO.class
                            ),
                            examples = @ExampleObject(
                                    name = "Cadastro completo da loja",
                                    value = RegistrationExamples.STORE_REQUEST
                            )
                    )
            )
    )
    @ApiResponse(
            responseCode = "201",
            description = "Lojista e loja cadastrados",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            implementation = RegistrationResponseDTO.class
                    ),
                    examples = @ExampleObject(
                            value = RegistrationExamples.STORE_RESPONSE
                    )
            )
    )
    @PostMapping(
            value = "/merchant",
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<RegistrationResponseDTO> registerStore(
            @Valid @RequestBody RegisterStoreRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.registerStore(request));
    }
}