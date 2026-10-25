![Logo Vitryne](https://raw.githubusercontent.com/Vitryne/.github/main/assets/logotipoGradiente.png)

# Backend da plataforma Vitryne

API responsável pelas regras de negócio e persistência de dados da plataforma Vitryne, consumida pelas aplicações web e mobile.

![Tecnologias](https://skillicons.dev/icons?i=java,spring,postgres,docker)

## 📖 Visão geral

A Vitryne é uma plataforma de marketplace desenvolvida como Trabalho de Conclusão de Curso.

O backend centraliza o cadastro de clientes e lojistas, o catálogo de produtos e os demais fluxos de compra e venda.

O projeto está em desenvolvimento. A disponibilidade de cada funcionalidade depende da branch utilizada.

## 🛠️ Stack

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Maven
- PostgreSQL
- Flyway
- Springdoc OpenAPI / Swagger UI
- Docker e Docker Compose

## 🚀 Começando

### Pré-requisitos

Para executar com Docker:

- [Git](https://git-scm.com/)
- [Docker com Docker Compose](https://www.docker.com/)

Mantenha o Docker em execução antes de iniciar os containers.

Para desenvolver ou executar a aplicação fora dos containers, também é necessário ter o JDK 21 instalado e configurar o acesso ao PostgreSQL.

### 1. Clonar o repositório

```bash
git clone https://github.com/Vitryne/backend.git
cd backend
```

### 2. Configurar as variáveis de ambiente

Na raiz do projeto, copie o arquivo `.env.example` para `.env`.

**Windows — PowerShell:**

```powershell
Copy-Item .env.example .env
```

**Linux, macOS ou Git Bash:**

```bash
cp .env.example .env
```

Edite o `.env` e preencha os valores conforme seu ambiente antes de iniciar a aplicação.

O `.env.example` documenta as variáveis utilizadas pelo projeto. Não versione o `.env` nem credenciais reais.

### 3. Iniciar a aplicação e o banco

```bash
docker compose up -d --build
```

O comando constrói a imagem da aplicação e inicia os serviços em segundo plano.

Para acompanhar a inicialização:

```bash
docker compose logs -f
```

Para verificar o estado dos containers:

```bash
docker compose ps
```

As migrations do Flyway são executadas automaticamente durante a inicialização da aplicação.

### 4. Acessar a API

Considerando a API publicada na porta `8080`:

| Recurso | Endereço |
| --- | --- |
| URL base | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Especificação OpenAPI | http://localhost:8080/v3/api-docs |

Caso altere a porta publicada no Docker Compose, ajuste os endereços acima.

A URL base é apenas o prefixo dos endpoints; ela não representa necessariamente uma página disponível.

## ⚙️ Comandos úteis

Execute os comandos na raiz do repositório.

### Reconstruir e iniciar após alterações

```bash
docker compose up -d --build
```

### Acompanhar os logs

```bash
docker compose logs -f
```

Use `Ctrl+C` para encerrar o acompanhamento dos logs. Os containers continuam em execução.

### Parar e remover os containers

```bash
docker compose down
```

Os dados armazenados nos volumes persistentes são preservados.

### Recriar o ambiente apagando os dados

> Atenção: este procedimento remove os volumes do projeto e apaga os dados do banco. Use apenas quando puder descartar os dados locais.

```bash
docker compose down -v
docker compose up -d --build
```

O reset não é necessário para atualizar a aplicação ou aplicar novas migrations.

## 🗃️ Banco de dados e migrations

O projeto utiliza Flyway para versionar as alterações no banco de dados.

As migrations ficam em:

```text
src/main/resources/db/migration
```

Os arquivos seguem o padrão de versão por timestamp:

```text
V<yyyyMMddHHmmss>__<descricao>.sql
```

Exemplo de nome:

```text
V20261025190000__add_stock_version.sql
```

Orientações:

- Crie uma nova migration para cada alteração de estrutura.
- Use uma versão posterior às migrations existentes e alinhe a ordem com a equipe.
- Não altere ou remova migrations já aplicadas em ambientes compartilhados.
- Não apague o histórico do Flyway para tentar corrigir uma falha.
- Não dependa de IDs fixos ou registros de uma carga antiga de testes.

Em um banco novo, as tabelas são criadas pelas migrations. Para testar os fluxos, cadastre os dados necessários pelos endpoints disponíveis.

## 📚 Documentação da API

O Swagger UI permite consultar os endpoints, visualizar os DTOs e executar requisições:

http://localhost:8080/swagger-ui.html

A especificação OpenAPI em JSON está disponível em:

http://localhost:8080/v3/api-docs

Ela pode ser importada em ferramentas como Postman e Insomnia.

### Cadastro de usuários

| Método | Endpoint | Descrição |
| --- | --- | --- |
| POST | `/auth/register/client` | Cadastro de cliente |
| POST | `/auth/register/merchant` | Cadastro de lojista e loja |

Consulte os exemplos e os campos obrigatórios diretamente no Swagger.

Os cadastros de cliente e lojista possuem contratos distintos. O cadastro de um usuário não equivale à realização de login.

### Integração com web e mobile

- Use os contratos do Swagger como referência para os nomes, tipos e obrigatoriedade dos campos.
- Confira os códigos de resposta documentados para tratar sucesso e erros.
- Endpoints protegidos dependem da autenticação e das permissões exigidas.
- Em outro computador ou dispositivo físico, `localhost` aponta para o próprio dispositivo. Utilize o endereço da máquina que hospeda a API e a porta publicada, com acesso de rede permitido.

## 📖 Documentação do projeto

A documentação de arquitetura, regras de negócio e decisões técnicas está disponível no [repositório de documentação](https://github.com/Vitryne/docs).

## 🌐 Ecossistema

| Repositório | Descrição |
| --- | --- |
| [Backend](https://github.com/Vitryne/backend) | API da plataforma |
| [Frontend](https://github.com/Vitryne/frontend) | Aplicação web |
| [Mobile](https://github.com/Vitryne/mobile) | Aplicativo mobile |
| [Docs](https://github.com/Vitryne/docs) | Documentação técnica |

## 📄 Licença

Este projeto está licenciado sob a licença MIT. Consulte o arquivo [LICENSE](LICENSE) para mais informações.