<div align="center">

<img src="https://raw.githubusercontent.com/Vitryne/.github/main/assets/logotipoGradiente.png" width="280" alt="Logo da Vitryne" />

# Backend da plataforma Vitryne

API responsável pelas regras de negócio e persistência de dados da plataforma Vitryne, consumida pelas aplicações web e mobile.

![Stack](https://skillicons.dev/icons?i=java,spring,postgres,docker)

</div>


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
- Cloudinary (armazenamento das imagens)

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

| Variável | Obrigatória | Descrição |
| --- | --- | --- |
| `KEY_DB_USER`, `KEY_DB_PASSWORD`, `KEY_DB`, `KEY_DB_PORT` | Sim | Acesso ao PostgreSQL |
| `CLOUDINARY_URL` | Para o upload de imagens | Credencial do Cloudinary. Sem ela a aplicação sobe, mas o upload responde `503` |
| `CLOUDINARY_FOLDER` | Não | Pasta das imagens no Cloudinary. Padrão: `vitryne` |
| `IMAGE_ORPHAN_RETENTION` | Não | Por quanto tempo uma imagem enviada e não usada em produto é mantida. Padrão: `24h` |
| `IMAGE_ORPHAN_CLEANUP_INTERVAL` | Não | Intervalo da limpeza das imagens não usadas. Padrão: `PT1H` |
| `ORDER_ACCEPTANCE_TIMEOUT` | Não | Prazo para a loja aceitar o pedido; vencido, o pedido é cancelado. Padrão: `30m` |
| `STORE_TIMEZONE` | Não | Fuso dos horários de funcionamento das lojas, usado no `isOpen`. Padrão: `America/Sao_Paulo` |
| `ORDER_ACCEPTANCE_EXPIRATION_INTERVAL` | Não | Intervalo da verificação dos pedidos vencidos. Padrão: `PT1M` |

### 3. Configurar o Cloudinary (upload de imagens)

As fotos dos produtos ficam no [Cloudinary](https://cloudinary.com), e não no disco do servidor. Para usar o upload em desenvolvimento:

1. Crie uma conta gratuita no Cloudinary.
2. No [console](https://console.cloudinary.com), abra **Settings → API Keys** (ou o bloco de credenciais no Dashboard) e copie a linha **API environment variable**.
3. Cole no seu `.env`, de preferência com uma pasta separada para desenvolvimento:

```dotenv
CLOUDINARY_URL=cloudinary://<api_key>:<api_secret>@<cloud_name>
CLOUDINARY_FOLDER=vitryne-dev
```

> ⚠️ Coloque a credencial só no `.env`, que não é versionado. Este repositório é público: credencial em qualquer arquivo versionado, incluindo o `.env.example`, fica exposta no histórico do Git. Se isso acontecer, gere uma nova chave no Cloudinary.

Sem o `CLOUDINARY_URL`, todo o restante da API funciona normalmente; só o upload de imagens fica indisponível.

### 4. Chaves JWT (desenvolvimento)

O projeto usa um par de chaves RSA para assinar e validar os tokens JWT.
O repositório inclui um par **exclusivo para desenvolvimento** em
`src/main/resources/keys/dev/`. Antes de subir o projeto pela primeira vez,
copie-o para o local que a aplicação lê:

**Linux / macOS / Git Bash**
```bash
cp src/main/resources/keys/dev/*.pem src/main/resources/keys/
```

**Windows (PowerShell)**
```powershell
Copy-Item src\main\resources\keys\dev\*.pem src\main\resources\keys\
```

Depois, suba normalmente (`docker compose up --build`). Se esquecer esse
passo, a aplicação falha na inicialização por não encontrar os arquivos.

> ⚠️ As chaves de `keys/dev/` são públicas e **não devem ser usadas em
> produção**. Em qualquer ambiente real, gere um par novo e injete como
> secret, fora do repositório.

Para usar um par próprio, gere com:

```bash
openssl genrsa -out keypair.pem 2048
openssl rsa -in keypair.pem -pubout -out src/main/resources/keys/public.pem
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in keypair.pem -out src/main/resources/keys/private.pem
rm keypair.pem
```

### 5. Iniciar a aplicação e o banco

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

### 6. Acessar a API

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

### Lojas

| Método | Endpoint | Descrição |
| --- | --- | --- |
| GET | `/stores` | Lojas ativas para a Home do app (público, sem token) |

- Lista paginada (`page` a partir de 0 e `size` de 1 a 50, padrão 20), em ordem alfabética.
- Aparecem só lojas ativas com pelo menos um produto ativo. Não há cálculo de distância no MVP.
- Cada loja traz nome, descrição, categoria principal, raio de entrega, tempo de preparo, foto, banner, tags, horários de funcionamento e `isOpen` (aberta agora, no horário de Brasília).
- Não traz CNPJ, razão social, telefone, endereço nem dados bancários.

### Catálogo de produtos

| Método | Endpoint | Descrição |
| --- | --- | --- |
| GET | `/product` | Produtos ativos para a vitrine do app (público, sem token) |

- Lista paginada (`page` a partir de 0 e `size` de 1 a 50, padrão 20), dos mais recentes para os mais antigos.
- Resposta no formato `{ content, page, size, totalElements, totalPages }`, igual a `GET /stores`.
- Produtos esgotados aparecem com `availableProduct` igual a `false`.

### Fotos de produto

| Método | Endpoint | Descrição |
| --- | --- | --- |
| POST | `/uploads/images` | Envio das fotos (perfil STORE) |

O cadastro de fotos acontece em duas etapas:

1. Envie as imagens em `multipart/form-data`, no campo `files`: de 1 a 8 arquivos JPG, PNG ou WEBP de até 5 MB cada. A resposta traz as URLs públicas na ordem enviada:

```json
{ "urls": ["https://res.cloudinary.com/.../foto1.jpg", "https://res.cloudinary.com/.../foto2.jpg"] }
```

2. Use essas URLs no `photoUrls` do cadastro (`POST /product/manager`) ou da edição (`PUT /product/manager/{id}`).

Regras:

- O produto aceita só URLs enviadas pela própria loja por essa rota. URLs externas são recusadas.
- Cada produto tem no máximo 8 fotos, sem repetição. A primeira do `photoUrls` é a capa.
- Uma imagem enviada e não usada em nenhum produto é apagada depois de 24 horas. O mesmo vale para as fotos removidas na edição e para as de produtos excluídos.
- Arquivo inválido ou acima do limite retorna `400`, com a mensagem para exibir ao usuário no campo `detail`.

Exemplo com `curl`:

```bash
curl -X POST http://localhost:8080/uploads/images   -H "Authorization: Bearer <token do lojista>"   -F "files=@capa.jpg"   -F "files=@verso.png"
```

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