<div align="center">
    <img src="https://raw.githubusercontent.com/Vitryne/.github/main/assets/logotipoGradiente.png" width="280" />
    <h1> API REST que orquestra</h1>
    <p>catálogo, pedidos, pagamento e entrega da plataforma Vitryne. </p>
    <br>

[![Java](https://skillicons.dev/icons?i=java,spring,postgres,redis,docker)](https://skillicons.dev)
</div>

---

## Sobre

O `vitryne-backend` é o núcleo da plataforma Vitryne. Expõe uma API REST stateless que serve a aplicação web e o aplicativo mobile, gerenciando os domínios de identidade, catálogo, pedidos, pagamento, entrega e notificações.

A arquitetura segue o padrão de **Monólito Modular** com bounded contexts inspirados em DDD, onde cada módulo se comunica via interfaces, mantendo o isolamento do domínio sem a complexidade operacional de microsserviços.

---

## Stack

| Camada | Tecnologias |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.x |
| Banco de dados | PostgreSQL |
| Cache / Sessão | Redis |
| Migrations | Flyway |
| Autenticação | Spring Security + JWT |
| Documentação da API | SpringDoc OpenAPI (Swagger UI) |
| Containerização | Docker + Docker Compose |
| Testes | JUnit 5 + Mockito + Testcontainers |

---

## Pré-requisitos

- [Java 17+](https://adoptium.net/)
- [Maven 3.9+](https://maven.apache.org/)
- [Docker e Docker Compose](https://www.docker.com/)
- [PostgreSQL 16+](https://www.postgresql.org/) *(se rodar sem Docker)*
- [Redis 7+](https://redis.io/) *(se rodar sem Docker)*

---

## Instalação

### Com Docker *(recomendado)*

```bash
# Clone o repositório
git clone https://github.com/Vitryne/vitryne-backend.git
cd vitryne-backend

# Suba os containers (aplicação + banco + cache)
docker-compose up --build
```

### Sem Docker

```bash
# Clone o repositório
git clone https://github.com/Vitryne/vitryne-backend.git
cd vitryne-backend

# Configure as variáveis de ambiente (veja a seção abaixo)
cp .env.example .env

# Execute a aplicação
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

A API estará disponível em `http://localhost:8080`.  
A documentação interativa (Swagger UI) estará em `http://localhost:8080/swagger-ui.html`.

---

## Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto com base no `.env.example`:

```env
# Banco de dados
DB_URL=jdbc:postgresql://localhost:5432/vitryne
DB_USERNAME=vitryne
DB_PASSWORD=senha_segura

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=sua_chave_secreta_de_no_minimo_256_bits
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=604800000

# Gateway de pagamento
PAYMENT_GATEWAY_KEY=sua_chave_de_api
PAYMENT_GATEWAY_SECRET=seu_segredo

# Notificações push (FCM)
FCM_SERVER_KEY=sua_chave_fcm
```

> **Nunca versione o arquivo `.env` com credenciais reais.** O `.gitignore` já exclui este arquivo por padrão.

---

## Estrutura do Projeto

```
src/main/java/com/vitryne/
├── config/          # Configurações: Security, CORS, Swagger, WebSocket, Redis
├── controller/      # Controllers REST — apenas delegação para services
├── dto/
│   ├── request/     # DTOs de entrada (dados recebidos pela API)
│   └── response/    # DTOs de saída (dados retornados pela API)
├── entity/          # Entidades JPA mapeadas para o banco de dados
├── enums/           # Enumerações de domínio (UserRole, OrderStatus, etc.)
├── exception/       # Exceções customizadas e handler global (@ControllerAdvice)
├── repository/      # Repositórios Spring Data JPA
├── service/         # Lógica de negócio isolada por domínio
│   └── impl/        # Implementações concretas dos services
├── security/        # Filtros JWT, configuração de autenticação e autorização
├── util/            # Utilitários e helpers transversais
└── integration/     # Integrações externas: pagamento, geolocalização, FCM
```

---

## Módulos de Domínio

| Módulo | Responsabilidade |
|---|---|
| `identity` | Autenticação, autorização e ciclo de vida de tokens JWT |
| `user` | Cadastro e gestão de perfis (Consumidor, Lojista, Entregador) |
| `store` | Cadastro, configuração e status de lojas |
| `catalog` | Produtos, categorias, fotos, estoque e preços promocionais |
| `search` | Busca por proximidade geográfica com filtros combinados |
| `cart` | Carrinho persistido e sincronizado por sessão |
| `order` | Ciclo de vida do pedido e order splitting por loja |
| `payment` | Integração com gateway, pré-autorização e estornos |
| `delivery` | Acionamento de entregadores, rastreamento e geofencing |
| `notification` | Notificações push (FCM) e in-app via WebSocket |
| `admin` | Painel administrativo, aprovação de cadastros e disputas |

---

## Profiles

| Profile | Uso |
|---|---|
| `dev` | Desenvolvimento local com logs detalhados |
| `staging` | Homologação > gateway de pagamento em sandbox |
| `prod` | Produção > variáveis via ambiente, logs estruturados |

---

## Testes

```bash
# Executar todos os testes
./mvnw test

# Executar com relatório de cobertura (JaCoCo)
./mvnw verify
```

O relatório de cobertura é gerado em `target/site/jacoco/index.html`.  
A meta mínima do projeto é **70% de cobertura** nas camadas `service` e `controller`.
