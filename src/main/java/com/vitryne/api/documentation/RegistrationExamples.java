package com.vitryne.api.documentation;

public final class RegistrationExamples {

    private RegistrationExamples() {
    }

    public static final String CLIENT_REQUEST = """
        {
          "name": "Cliente Demonstração",
          "cpf": "111.111.111-11",
          "email": "cliente.novo@example.com",
          "password": "SenhaTeste123!",
          "address": {
            "zipCode": "87000-000",
            "street": "Rua de Teste",
            "number": "123A",
            "neighborhood": "Centro",
            "complement": "Apartamento 10",
            "city": "Maringá",
            "state": "PR"
          }
        }
        """;

    public static final String STORE_REQUEST = """
        {
          "email": "loja.nova@example.com",
          "password": "SenhaTeste123!",
          "name": "Moda Exemplo",
          "cnpj": "11.111.111/1111-11",
          "phone": "(44) 99999-9999",
          "legalName": "Moda Exemplo Ltda",
          "description": "Loja fictícia para demonstração.",
          "address": {
            "zipCode": "87000-000",
            "street": "Avenida de Teste",
            "number": "500",
            "neighborhood": "Centro",
            "complement": null,
            "city": "Maringá",
            "state": "PR"
          },
          "bankAccount": {
            "holderName": "Moda Exemplo Ltda",
            "holderDocument": "11.111.111/1111-11",
            "accountType": "CHECKING",
            "bankCode": "000",
            "agency": "0001",
            "accountNumber": "00123-4",
            "pixKey": "financeiro@example.com"
          },
          "mainCategory": "Moda feminina",
          "tags": ["casual", "vestidos"],
          "deliveryRadiusKm": 10.00,
          "preparationTimeMinutes": 30,
          "openingPeriods": [
            {
              "dayOfWeek": "MONDAY",
              "opensAt": "08:00",
              "closesAt": "12:00"
            },
            {
              "dayOfWeek": "MONDAY",
              "opensAt": "13:30",
              "closesAt": "18:00"
            }
          ],
          "photoUrl": "https://example.com/loja.jpg",
          "bannerUrl": "https://example.com/banner.jpg"
        }
        """;

    public static final String CLIENT_RESPONSE = """
        {
          "userId": 10,
          "clientId": 20,
          "storeId": null,
          "email": "cliente.novo@example.com",
          "role": "CONSUMER",
          "createdAt": "2026-10-25T12:00:00Z"
        }
        """;

    public static final String STORE_RESPONSE = """
        {
          "userId": 11,
          "clientId": null,
          "storeId": 30,
          "email": "loja.nova@example.com",
          "role": "STORE",
          "createdAt": "2026-10-25T12:00:00Z"
        }
        """;

    public static final String VALIDATION_ERROR = """
        {
          "message": "Dados de cadastro inválidos",
          "fields": {
            "email": "deve ser um endereço de e-mail bem formado"
          }
        }
        """;

    public static final String BUSINESS_ERROR = """
        {
          "message": "Horário de abertura deve ser anterior ao fechamento"
        }
        """;

    public static final String CONFLICT_ERROR = """
        {
          "message": "E-mail já cadastrado"
        }
        """;
}