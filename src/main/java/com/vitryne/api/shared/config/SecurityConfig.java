package com.vitryne.api.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/auth/register/client",
                                "/auth/register/merchant"
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        // Documentação Swagger/OpenAPI.
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()

                        // Cadastro público de clientes e lojistas.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/register/client",
                                "/auth/register/merchant"
                        ).permitAll()

                        // Catálogo público.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/product",
                                "/product/client/*"
                        ).permitAll()

                        // Demais operações exigem autenticação.
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}