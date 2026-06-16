<div align="center">
    <img src="https://raw.githubusercontent.com/Vitryne/.github/main/assets/logotipoGradiente.png" width="280" />
    <h1> API REST que orquestra</h1>
    <p>catálogo, pedidos, pagamento e entrega da plataforma Vitryne. </p>
    <br>

[![Java](https://skillicons.dev/icons?i=java,spring,postgres,docker)](https://skillicons.dev)
</div>

---

## Sobre

O `vitryne-backend` é o núcleo da plataforma Vitryne. Expõe uma API REST stateless que serve a aplicação web e o aplicativo mobile.

A visão do produto cobre os domínios de identidade, catálogo, pedidos, pagamento, entrega e notificações. **Atualmente** a API implementa o núcleo de **catálogo de produtos, estoque e carrinho** — os demais domínios estão no roadmap (veja [Roadmap](#roadmap)).

A arquitetura segue o padrão de **Monólito Modular** com bounded contexts inspirados em DDD, onde cada módulo se comunica via interfaces, mantendo o isolamento do domínio sem a complexidade operacional de microsserviços.

---

## Stack

### Em uso hoje

| Camada | Tecnologias |
|---|-------|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.0.x |
| Banco de dados | PostgreSQL |
| Migrations | Flyway |
| Persistência | Spring Data JPA |
| Produtividade | Lombok |
| Containerização | Docker + Docker Compose |
| Testes | JUnit 5 |

### Planejado para próximas features

> Estas tecnologias **ainda não estão integradas** ao projeto e serão adicionadas conforme as features de cada domínio evoluírem.

| Camada | Tecnologia | Quando |
|---|---|---|
| Cache / Sessão | Redis | Carrinho sincronizado e cache de catálogo |
| Autenticação | Spring Security + JWT | Módulo `identity` |
| Documentação da API | SpringDoc OpenAPI (Swagger UI) | Próxima iteração de DX |
| Testes | Mockito + Testcontainers | Cobertura de integração |
| Cobertura | JaCoCo | Métricas de qualidade |

---

## Pré-requisitos

- [Java 21+](https://adoptium.net/)
- [Maven 3.9+](https://maven.apache.org/) *(ou use o wrapper `./mvnw` incluído)*
- [Docker e Docker Compose](https://www.docker.com/)
- [PostgreSQL 15+](https://www.postgresql.org/) *(se rodar sem Docker)*

---

## Instalação

### Com Docker *(recomendado)*

```bash
# Clone o repositório
git clone https://github.com/Vitryne/vitryne-backend.git
cd vitryne-backend

# Crie o arquivo .env na raiz (veja a seção Variáveis de Ambiente)

# Suba os containers (aplicação + banco)
docker-compose up --build
```

### Sem Docker

```bash
# Clone o repositório
git clone https://github.com/Vitryne/vitryne-backend.git
cd vitryne-backend

# Crie o arquivo .env na raiz (veja a seção Variáveis de Ambiente)
# e tenha um PostgreSQL acessível nas credenciais informadas

# Execute a aplicação
./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080`.

---

## Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto. As variáveis abaixo são as utilizadas hoje:

```env
# Banco de dados
KEY_DB_USER=vitryne_user
KEY_DB_PASSWORD=sua_senha_segura
KEY_DB=vitryne_db
KEY_DB_PORT=5435
```

O `application.properties` carrega esse arquivo via `spring.config.import=optional:file:.env[.properties]`.

> **Nunca versione o arquivo `.env` com credenciais reais.** Adicione `.env` ao `.gitignore` e mantenha apenas valores de exemplo no repositório.

---

## Estrutura do Projeto

```
src/main/java/com/vitryne/api/
├── controller/      # Controllers REST — apenas delegação para services
├── dto/             # DTOs de entrada e saída da API
├── entity/          # Entidades JPA mapeadas para o banco de dados
├── exception/       # Exceções customizadas e handler global (@ControllerAdvice)
├── repository/      # Repositórios Spring Data JPA
└── service/         # Lógica de negócio isolada por domínio

src/main/resources/
├── application.properties
└── db/migration/    # Scripts de migração Flyway
```

> Conforme novos domínios forem implementados, a estrutura deve crescer com pacotes como `config/`, `security/`, `enums/`, `util/` e `integration/`.

---

## Módulos de Domínio

### Implementados

| Módulo | Responsabilidade |
|---|---|
| `produto` (catalog) | Cadastro de produtos e tamanhos disponíveis |
| `estoque` (supply) | Controle de disponibilidade e quantidade por produto |
| `carrinho` (cart) | Carrinho de compras com itens e quantidades |

### Roadmap

> Domínios planejados que ainda **não foram implementados**.

| Módulo | Responsabilidade |
|---|---|
| `identity` | Autenticação, autorização e ciclo de vida de tokens JWT |
| `user` | Cadastro e gestão de perfis (Consumidor, Lojista, Entregador) |
| `store` | Cadastro, configuração e status de lojas |
| `search` | Busca por proximidade geográfica com filtros combinados |
| `order` | Ciclo de vida do pedido e order splitting por loja |
| `payment` | Integração com gateway, pré-autorização e estornos |
| `delivery` | Acionamento de entregadores, rastreamento e geofencing |
| `notification` | Notificações push (FCM) e in-app via WebSocket |
| `admin` | Painel administrativo, aprovação de cadastros e disputas |

---

## Testes

```bash
# Executar todos os testes
./mvnw test
```

> A configuração de cobertura (JaCoCo) e testes de integração (Mockito + Testcontainers) estão no roadmap. A meta de qualidade do projeto será **70% de cobertura** nas camadas `service` e `controller`.
