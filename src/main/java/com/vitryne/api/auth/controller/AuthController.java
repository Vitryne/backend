package com.vitryne.api.auth.controller;

import com.vitryne.api.auth.dto.LoginRequestDTO;
import com.vitryne.api.auth.dto.LoginResponseDTO;
import com.vitryne.api.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Login e emissão de token")
public class AuthController {

    private final AuthService authService;

    @Operation(
            operationId = "login",
            summary = "Autenticar usuário",
            description = """
            Valida e-mail e senha e retorna um JWT.
            Envie o token nas demais requisições no header
            Authorization: Bearer <token>.
            """
    )
    @ApiResponse(responseCode = "200", description = "Autenticado")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    @PostMapping(
            value = "/login",
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }
}
