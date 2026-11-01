package com.vitryne.api.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vitryneOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                                        Faça login em POST /auth/login e cole apenas o JWT
                                        no campo abaixo, sem o prefixo Bearer.
                                        """)))
                .info(new Info()
                        .title("Vitryne API")
                        .version("v1-mvp")
                        .description("""
                                API do marketplace Vitryne.

                                Fluxos disponíveis nesta documentação:
                                - Cadastro de cliente.
                                - Cadastro de lojista com criação da loja.
                                - Login com e-mail e senha para obtenção do JWT.
                                - Consulta ao catálogo e gestão de produtos da loja.
                                - Consulta e alteração do carrinho do cliente autenticado.

                                Observações do MVP:
                                - O login utiliza e-mail; username não é solicitado no cadastro.
                                - O cadastro não retorna token nem autentica automaticamente.
                                - Entrada Google e verificação de e-mail ainda não estão implementadas.
                                - CPF/CNPJ recebem validação básica de formato, sem verificação externa.
                                - Dados bancários são cadastrados, mas não verificados.
                                - Horários, raio de entrega e tempo de preparo são armazenados;
                                  isso não implica cálculo automático de frete ou abertura da loja.
                                - Datas Instant são representadas em ISO 8601.
                                """));
    }
}