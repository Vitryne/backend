package com.vitryne.api.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;

/**
 * Base dos testes de integração: aplicação completa contra Postgres real
 * (Flyway, locks, restrições), com MockMvc e JWT assinado pela chave de dev.
 * O contêiner é único por execução e compartilhado entre as classes; cada
 * teste cria os próprios dados com {@link PurchaseFixtures}.
 */
@SpringBootTest(properties = {
        "jwt.public.key=classpath:keys/dev/public.pem",
        "jwt.private.key=classpath:keys/dev/private.pem"
})
@AutoConfigureMockMvc
public abstract class PostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    private JwtEncoder jwtEncoder;

    /** Token Bearer com os mesmos claims do login (sub = id do usuário, role). */
    protected String bearer(long userId, String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("vitryne-api")
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(600))
                .claim("role", role)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims
        )).getTokenValue();
        return "Bearer " + token;
    }
}
