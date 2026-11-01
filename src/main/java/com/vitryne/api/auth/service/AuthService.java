package com.vitryne.api.auth.service;

import com.vitryne.api.auth.dto.LoginRequestDTO;
import com.vitryne.api.auth.dto.LoginResponseDTO;
import com.vitryne.api.shared.entity.User;
import com.vitryne.api.shared.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Credenciais inválidas";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final String dummyHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("dummy-password");
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        String hash = user == null ? dummyHash : user.getPasswordHash();
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        if (user == null || !passwordMatches || !user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    INVALID_CREDENTIALS
            );
        }

        return new LoginResponseDTO(
                tokenService.generate(user),
                "Bearer",
                tokenService.getExpirationSeconds()
        );
    }
}
