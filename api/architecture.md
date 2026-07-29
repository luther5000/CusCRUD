# Especificação Técnica: Backend da API REST do banco de dados do Projeto CusCrud

Esta é uma especificação técnica estruturada para que qualquer LLM (ou desenvolvedor) consiga interpretar a arquitetura do MVP do backend da API REST do banco de dados do Projeto CusCrud sem ambiguidades. Use este documento como o seu "Source of Truth" para o planejamento e execução

## Menu de Seções

- [1. Visão Geral](#sec-1)
- [2. Stack Tecnológica](#sec-2)
- [3. Regras de Negócio & Segurança (MVP)](#sec-3)
- [4. Arquitetura de Dados (Schema SQL)](#sec-4)
- [5. Especificação da API REST](#sec-5)
- [5.0. Saúde Operacional](#sec-5-0)
- [5.1. Autenticação](#sec-5-1)
- [5.2. Inventários (Gerenciamento)](#sec-5-2)
- [5.3. Inventários (Gerenciamento de Acesso)](#sec-5-3)
- [5.4. Tipos](#sec-5-4)
- [5.5. Produtos](#sec-5-5)
- [6. Infraestrutura e Redirecionamento (Nginx)](#sec-6)
- [7. Convenções de código](#sec-7)
- [7.1. Documentação](#sec-7-1)
- [7.2. Testes](#sec-7-2)
- [7.3. TDD](#sec-7-3)
- [7.4. Validação de Email](#sec-7-4)
- [8. Especificações para agentes](#sec-8)

<a id="sec-1"></a>
## 1. Visão Geral
Sistema de backend para suporte a uma aplicação mobile, permitindo a gestão de inventários multiusuário com controle de acesso básico e armazenamento de tipos de produtos (incluindo imagens).

---

<a id="sec-2"></a>
## 2. Stack Tecnológica
* **Banco de Dados:** PostgreSQL 17
* **Backend:** API REST Java com Spring Boot.
* **Acesso a Dados:** Spring JDBC para consultas e comandos SQL contra o PostgreSQL.
* **Segurança:** Spring Security para autenticação, autorização HTTP e processamento do header `Authorization: Bearer <token>`.
* **Hash de Senha:** `BCryptPasswordEncoder` para geração e verificação do hash de `passwd`.
* **JWT:** Token assinado com `HS256`, usando segredo lido da variável de ambiente `JWT_SECRET` e TTL fixo de `3600` segundos.
* **Proxy Reverso:** Nginx
* **Containerização:** Docker & Docker Compose com backend e PostgreSQL. A porta exposta vai ser redirecionada pelo Nginx. O banco roda em container com volume fixo para dados.

---

<a id="sec-3"></a>
## 3. Regras de Negócio & Segurança (MVP)

1.  **Isolamento de Dados:** Toda requisição aos endpoints de `/inventories/{inv_id}/*` deve primeiro validar se o `user_id` extraído do JWT possui uma entrada correspondente na tabela `inventory_access` para aquele `inv_id`.
2.  **Paginação e Ordenação:** Sempre offset. O campo `next_page` deve ser uma URL completa ou parâmetro de query (ex: `?offset=200`). Quando não houver próxima página, `next_page` deve ser omitido. A ordenação das listagens paginadas deve ser determinística e usar apenas a PK de cada tabela: inventários por `inv_id ASC`; usuários por `user_id ASC`; tipos por `type_id ASC`; produtos por `product_id ASC`.
3.  **Integridade de Tipos:** Não permitir a exclusão de um registro de `types` se houver produtos vinculados a ele (`ON DELETE RESTRICT`).
4.  **Convenção de Roles:** `role = 0` representa owner (leitura, escrita e administração), `role = 1` representa editor (leitura e escrita sem administração) e `role = 2` representa reader (somente leitura).
5.  **Criação de Inventário:** Ao criar um inventário via `POST /inventories`, o sistema deve automaticamente criar uma entrada em `inventory_access` com `role = 0` (owner) para o usuário que criou.
6.  **Gerenciamento de Inventário:** Apenas usuários com `role = 0` (owner) podem renomear, deletar o inventário, listar usuários com acesso, adicionar usuários e remover usuários.
7.  **Edição de Tipos e Produtos:** Usuários com `role = 0` (owner) ou `role = 1` (editor) podem criar, editar e remover tipos e produtos do inventário.
8.  **Leitura de Tipos e Produtos:** Usuários com qualquer role válida (`0`, `1` ou `2`) podem visualizar tipos e produtos do inventário.
9.  **Adição de Usuários:** Apenas o owner pode adicionar usuários a um inventário via `POST /inventories/{inv_id}/users`. O novo usuário deve receber `role = 1` (editor) ou `role = 2` (reader); esta rota não atribui `role = 0`.
10. **Auto-remoção do Owner:** O owner não pode remover o próprio acesso ao inventário via `DELETE /inventories/{inv_id}/users/{user_id}`; o sistema deve retornar `409 Conflict`.
11. **Alteração de Role:** Apenas o owner pode alterar a role de outro usuário entre `1` (editor) e `2` (reader) via `PATCH /inventories/{inv_id}/users/{user_id}`. O owner não pode alterar a própria role; o sistema deve retornar `409 Conflict`.
12. **Datas e Timezone:** Todos os campos de data/hora persistidos e expostos pela API devem usar timezone. No banco, usar `TIMESTAMP WITH TIME ZONE`. Na aplicação e nos payloads/respostas, a timezone padrão é `America/Recife` (`UTC-3`), com serialização em ISO 8601 contendo offset explícito (ex.: `2026-03-19T15:30:00-03:00`).

---

<a id="sec-4"></a>
## 4. Arquitetura de Dados (Schema SQL)

Antes do schema principal, deve existir um arquivo `00_configure.sql` executado na inicialização do PostgreSQL para preparar o banco e o usuário da aplicação.

**Arquivo `00_configure.sql`:**

```sql
\set ON_ERROR_STOP on
\getenv cuscrud_app_password CUSCRUD_APP_PASSWORD

SELECT format(
    'CREATE ROLE cuscrud_app LOGIN PASSWORD %L',
    :'cuscrud_app_password'
)
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'cuscrud_app') \gexec

SELECT format(
    'ALTER ROLE cuscrud_app WITH LOGIN PASSWORD %L',
    :'cuscrud_app_password'
)
WHERE EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'cuscrud_app') \gexec

SELECT 'CREATE DATABASE cuscrud OWNER cuscrud_app'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'cuscrud') \gexec

ALTER DATABASE cuscrud OWNER TO cuscrud_app;
ALTER DATABASE cuscrud SET timezone TO 'America/Recife';
```

Esse arquivo deve ser executado antes do arquivo que cria tabelas, índices e constraints, garantindo que o banco da aplicação exista, tenha um owner dedicado e fique com a timezone padrão alinhada com a regra global da aplicação. No fluxo com Docker Compose, o script lê `CUSCRUD_APP_PASSWORD` diretamente do ambiente do container PostgreSQL via `\getenv`, sem exigir edição manual do arquivo SQL para materializar a senha.

```sql
-- Conectar explicitamente ao banco da aplicação antes de criar extensões e objetos
\connect cuscrud

-- Habilitar a extensão necessária no banco da aplicação
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Garantir que tabelas, índices e constraints pertençam ao usuário da aplicação
SET ROLE cuscrud_app;

-- 1. Tabela de Usuários
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    login VARCHAR(255) UNIQUE NOT NULL, -- email
    passwd TEXT NOT NULL, -- Bcrypt
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_login ON users (login);

-- 2. Tabela de Inventários
CREATE TABLE inventories (
    inv_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    inv_name VARCHAR(255) NOT NULL
);

-- 3. Tabela de Acesso
CREATE TABLE inventory_access (
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    inv_id UUID REFERENCES inventories(inv_id) ON DELETE CASCADE,
    role INT4 NOT NULL,
    PRIMARY KEY (user_id, inv_id)
);
CREATE INDEX idx_inventory_access_user ON inventory_access (user_id);
CREATE INDEX idx_inventory_access_inv ON inventory_access (inv_id);

-- 4. Tabela de Tipos
CREATE TABLE types (
    type_id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    imagem BYTEA, -- Armazenamento binário de imagem
    inv_id UUID NOT NULL REFERENCES inventories(inv_id) ON DELETE CASCADE,
    UNIQUE (inv_id, nome),
    UNIQUE (inv_id, type_id)
);
CREATE INDEX idx_types_inv ON types (inv_id);

-- 5. Tabela de Produtos
CREATE TABLE products (
    product_id BIGSERIAL PRIMARY KEY,
    type_id INT8 NOT NULL,
    marca VARCHAR(255) NULL,
    dataValidade TIMESTAMP WITH TIME ZONE NULL,
    unidade INT8 NULL CHECK (unidade IS NULL OR unidade BETWEEN 0 AND 999999999999999999),
    unidadeMedida VARCHAR(255) NULL,
    quantidade INT8 NOT NULL DEFAULT 0 CHECK (quantidade BETWEEN 0 AND 999999999999999999),
    inv_id UUID NOT NULL REFERENCES inventories(inv_id) ON DELETE CASCADE,
    FOREIGN KEY (inv_id, type_id) REFERENCES types(inv_id, type_id) ON DELETE RESTRICT
);
CREATE INDEX idx_products_inv ON products (inv_id);
CREATE INDEX idx_products_type_id ON products (type_id);
CREATE INDEX idx_products_inv_type_id ON products (inv_id, type_id);

RESET ROLE;
```

<a id="sec-5"></a>
## 5. Especificação da API REST

A API utiliza o cabeçalho `Authorization: Bearer <token>` para identificar o usuário, eliminando a necessidade de enviar `user_id` no corpo das requisições de escrita/leitura. Paginação é sempre por `limit`/`offset`; limites fora do intervalo aceito resultam em 404. Quando não houver próxima página, o campo de paginação (`next_page` ou equivalente) deve ser omitido. Todos os endpoints devem ser expostos sob o prefixo `/api/v1`.

**Versionamento semântico**
- Formato: `MAJOR.MINOR.PATCH`, com sufixos opcionais de pré-lançamento e metadados de build.
- Incrementar:
  - MAJOR: mudanças incompatíveis na API.
  - MINOR: novas funcionalidades compatíveis.
  - PATCH: correções compatíveis.

**Padrão de respostas de erro**
- Toda resposta de erro usa JSON no formato:
```json
{
  "error": {
    "code": "STRING_INTERNO",
    "message": "Descrição curta e acionável",
    "details": { "campo": "opcional", "info": "opcional" }
  }
}
```
- Exemplos de `code` por status:
  - 400: `VALIDATION_ERROR`
  - 401: `UNAUTHENTICATED`
  - 403: `FORBIDDEN`
  - 404: `NOT_FOUND`
  - 409: `CONFLICT`
- Endpoints que retornam 204 **não** enviam corpo.
- As tabelas de erros listam apenas o HTTP status; o corpo sempre segue o formato acima.

<a id="sec-5-0"></a>
### 5.0. Saúde Operacional

|  ID   | Método  | Endpoint         | Descrição                                                                               |
|:-----:|:--------|:-----------------|:----------------------------------------------------------------------------------------|
| 5.0.1 | **GET** | `/api/v1/health` | Checagem operacional do backend. Não requer autenticação e não acessa o banco de dados. |

#### 5.0.1 GET /health

**Headers:**
Nenhum.

**Payload:**
Nenhum.

**Status Codes:**

| Código | Descrição                                                         |
|:------:|:------------------------------------------------------------------|
|  200   | Aplicação em execução e apta a responder requisições HTTP.        |

**Resposta (200):**
```json
{
  "status": "ok",
  "time": "2026-03-24T14:05:00-03:00"
}
```

**Exemplo de requisição:**
```bash
curl -X GET https://api.exemplo.com/api/v1/health
```

**Permissões:** Endpoint público. Não requer token JWT. Não consulta banco de dados nem outros serviços externos para responder.

<a id="sec-5-1"></a>
### 5.1. Autenticação

|  ID   | Método   | Endpoint                | Descrição                                                                                                                   |
|:-----:|:---------|:------------------------|:----------------------------------------------------------------------------------------------------------------------------|
| 5.1.1 | **POST** | `/api/v1/auth/login`    | Gera o token JWT para acesso geral do usuário. O token concede acesso a todos os inventários que o usuário pode visualizar. |
| 5.1.2 | **POST** | `/api/v1/auth/register` | Cria um novo usuário com nome, email (`login`) e senha fornecidos.                                                          |
| 5.1.3 | **GET**  | `/api/v1/auth/validate` | Valida o token JWT da requisição. Retorna informações do usuário se válido.                                                 |

**Notas técnicas de implementação:**
- A camada HTTP de autenticação e autorização deve usar Spring Security.
- O acesso à tabela `users` deve usar Spring JDBC.
- O campo `passwd` deve ser armazenado e validado com `BCryptPasswordEncoder`.
- A assinatura e validação do JWT devem usar `HS256` com segredo vindo de `JWT_SECRET`.
- O TTL do token é fixo em `3600` segundos para `login` e para o cálculo de `expires_in` retornado pela API.

#### 5.1.1 POST /auth/login

**Headers:**
```
Content-Type: application/json
```

**Payload:**

| Campo    |  Tipo  | Obrigatório | Descrição                          | Restrições                                |
|:---------|:------:|:-----------:|:-----------------------------------|:------------------------------------------|
| `login`  | string |     Sim     | Email do usuário para autenticação | Formato de email válido, 1-255 caracteres |
| `passwd` | string |     Sim     | Senha do usuário                   | De 8 a 50 caracteres                      |

**Status Codes:**

| Código | Descrição                                                |
|:------:|:---------------------------------------------------------|
|  200   | Login bem-sucedido. Retorna o token JWT.                 |
|  400   | Payload inválido ou campos faltando.                     |
|  401   | Credenciais inválidas (login/email ou senha incorretos). |

**Resposta (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 3600,
  "user": {
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "João Silva",
    "login": "joao@example.com",
    "created_at": "2026-03-19T15:30:00-03:00"
  }
}
```

**JWT Claims e algoritmo:**

|  Claim   | Descrição                            |
|:--------:|:-------------------------------------|
|  `sub`   | `user_id` do usuário                 |
|  `exp`   | Timestamp de expiração (TTL: 1 hora) |
|  `iat`   | Timestamp de emissão                 |

**Algoritmo de geração do JWT:**
1. Header: `{ "alg": "HS256", "typ": "JWT" }`.
2. Payload: incluir claims acima; `exp = iat + 3600s`.
3. Assinatura: `HMAC-SHA256(base64url(header) + "." + base64url(payload), secret_compartilhado)`, onde `secret_compartilhado` é lido de `JWT_SECRET`.
4. Retornar o token `header.payload.signature` e `expires_in = 3600`.

**Exemplo de Requisição:**
```bash
curl -X POST https://api.exemplo.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "login": "joao@example.com",
    "passwd": "minhasenha123"
  }'
```

**Erros:**

| Código | Situação                                             |
|:------:|:-----------------------------------------------------|
|  400   | `login` vazio ou fora do formato de email válido     |
|  400   | `passwd` fora do intervalo de 8 a 50 caracteres      |
|  401   | `login` não encontrado na base                       |
|  401   | `passwd` incorreta                                   |

---

**Permissões:** Este endpoint é público (não requer autenticação).

#### 5.1.2 POST /auth/register

**Headers:**
```
Content-Type: application/json
```

**Payload:**

| Campo    |  Tipo  | Obrigatório | Descrição                     | Restrições                                                  |
|:---------|:------:|:-----------:|:------------------------------|:------------------------------------------------------------|
| `name`   | string |     Sim     | Nome completo do usuário      | 1-255 caracteres em UTF-8                                   |
| `login`  | string |     Sim     | Email único para a nova conta | Formato de email válido, 1-255 caracteres, único no sistema |
| `passwd` | string |     Sim     | Senha para a nova conta       | De 8 a 50 caracteres                                        |

**Status Codes:**

| Código | Descrição                                        |
|:------:|:-------------------------------------------------|
|  201   | Usuário criado com sucesso. Retorna o `user_id`. |
|  400   | Payload inválido ou campos faltando.             |
|  409   | Já existe um usuário com o `login` informado.    |

**Resposta (201):**
```json
{
  "user_id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "João Novo",
  "login": "joao.novo@example.com",
  "created_at": "2026-03-19T15:30:00-03:00"
}
```

**Exemplo de Requisição:**
```bash
curl -X POST https://api.exemplo.com/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Novo",
    "login": "joao.novo@example.com",
    "passwd": "senhaforte456"
  }'
```

**Erros:**

| Código | Situação                                        |
|:------:|:------------------------------------------------|
|  400   | `login` vazio ou fora do formato de email       |
|  400   | `passwd` fora do intervalo de 8 a 50 caracteres |
|  400   | `name` vazio ou fora do limite de tamanho       |
|  409   | `login` já cadastrado no sistema                |

**Notas:**
- A senha é hashada com Bcrypt antes de ser armazenada.
- Este endpoint **não** gera um token JWT. O usuário deve usar o endpoint 5.1.1 para autenticar após criar a conta.

---

**Permissões:** Este endpoint é público (não requer autenticação).

#### 5.1.3 GET /auth/validate

**Headers:**
```
Authorization: Bearer <token>
```

**Payload:** -

**Status Codes:**

| Código | Descrição                               |
|:------:|:----------------------------------------|
|  200   | Token válido. Retorna dados do usuário. |
|  401   | Token ausente, inválido ou expirado.    |

**Resposta (200):**
```json
{
  "user": {
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "João Silva",
    "login": "joao@example.com",
    "created_at": "2026-03-19T15:30:00-03:00"
  },
  "token": {
    "expires_in": 3600,
    "issued_at": "2026-03-19T15:30:00-03:00"
  }
}
```

**Exemplo de Requisição:**
```bash
curl -X GET https://api.exemplo.com/api/v1/auth/validate \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Erros:**

| Código | Situação                                  |
|:------:|:------------------------------------------|
|  401   | Token não enviado ou com formato inválido |
|  401   | Token expirado ou assinatura inválida     |

---

**Permissões:** Este endpoint requer autenticação (JWT).

<a id="sec-5-2"></a>
### 5.2. Inventários (Gerenciamento)

|  ID   | Método     | Endpoint                       | Descrição                                                                                                            |
|:-----:|:-----------|:-------------------------------|:---------------------------------------------------------------------------------------------------------------------|
| 5.2.1 | **POST**   | `/api/v1/inventories`          | Cria um novo inventário. O usuário logado recebe automaticamente a função de owner (`role = 0`). Retorna o `inv_id`. |
| 5.2.2 | **PATCH**  | `/api/v1/inventories/{inv_id}` | Renomeia um inventário. Apenas o owner (`role = 0`) pode executar esta ação.                                         |
| 5.2.3 | **DELETE** | `/api/v1/inventories/{inv_id}` | Apaga um inventário. Apenas o owner (`role = 0`) pode executar esta ação.                                            |
| 5.2.4 | **GET**    | `/api/v1/inventories`          | Lista até 200 inventários que o usuário logado acessa.                                                               |

#### 5.2.1 POST /inventories

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Payload:**

| Campo      |  Tipo  | Obrigatório | Descrição                       | Restrições       |
|:-----------|:------:|:-----------:|:--------------------------------|:-----------------|
| `inv_name` | string |     Sim     | Nome do inventário a ser criado | 1-255 caracteres |

**Status Codes:**

| Código | Descrição                                                                 |
|:------:|:--------------------------------------------------------------------------|
|  201   | Inventário criado. Retorna o `inv_id`.                                    |
|  400   | Payload inválido ou `inv_name` ausente/vazio.                             |
|  401   | Token ausente, inválido ou expirado.                                      |
|  409   | Limite de inventários que o usuário pode ser owner atingido (máximo 100). |

**Resposta (201):**
```json
{
  "inventory": {
    "inv_id": "123e4567-e89b-12d3-a456-426614174000",
    "inv_name": "Estoque da Loja"
  },
  "role": 0
}
```

**Exemplo de Requisição:**
```bash
curl -X POST https://api.exemplo.com/api/v1/inventories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "inv_name": "Estoque da Loja"
  }'
```

**Erros:**

| Código | Situação                                                        |
|:------:|:----------------------------------------------------------------|
|  400   | `inv_name` vazio ou fora do limite de tamanho                   |
|  401   | Token não enviado ou inválido                                   |
|  409   | Limite de 100 inventários que o usuário pode ser owner atingido |

---

**Permissões:** Este endpoint requer autenticação (JWT). O usuário autenticado torna-se owner (`role = 0`) do inventário criado. Limite máximo de 100 inventários que o usuário pode ser owner.

#### 5.2.2 PATCH /inventories/{inv_id}

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Path Params:**

| Campo    | Tipo | Obrigatório | Descrição                              |
|:---------|:----:|:-----------:|:---------------------------------------|
| `inv_id` | UUID |     Sim     | Identificador do inventário a renomear |

**Payload:**

| Campo      |  Tipo  | Obrigatório | Descrição                   | Restrições       |
|:-----------|:------:|:-----------:|:----------------------------|:-----------------|
| `inv_name` | string |     Sim     | Novo nome para o inventário | 1-255 caracteres |

**Status Codes:**

| Código | Descrição                                                    |
|:------:|:-------------------------------------------------------------|
|  200   | Inventário renomeado. Retorna dados atualizados.             |
|  400   | Payload inválido ou `inv_name` ausente/vazio.                |
|  401   | Token ausente, inválido ou expirado.                         |
|  403   | Usuário autenticado não possui `role = 0` para o inventário. |
|  404   | Inventário não encontrado para o `inv_id` informado.         |

**Resposta (200):**
```json
{
  "inventory": {
    "inv_id": "123e4567-e89b-12d3-a456-426614174000",
    "inv_name": "Estoque Renomeado"
  },
  "role": 0
}
```

**Exemplo de Requisição:**
```bash
curl -X PATCH https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "inv_name": "Estoque Renomeado"
  }'
```

**Erros:**

| Código | Situação                                                    |
|:------:|:------------------------------------------------------------|
|  400   | `inv_name` vazio ou fora do limite de tamanho               |
|  401   | Token não enviado ou inválido                               |
|  403   | Usuário autenticado não possui `role = 0` para o inventário |
|  404   | `inv_id` não encontrado ou não pertence ao usuário          |

---

**Permissões:** Este endpoint requer autenticação (JWT) e exige que o usuário tenha `role = 0` (owner) para o inventário especificado.

#### 5.2.3 DELETE /inventories/{inv_id}

**Headers:**
```
Authorization: Bearer <token>
```

**Path Params:**

| Campo    | Tipo | Obrigatório | Descrição                                  |
|:---------|:----:|:-----------:|:-------------------------------------------|
| `inv_id` | UUID |     Sim     | Identificador do inventário a ser removido |

**Payload:** -

**Status Codes:**

| Código | Descrição                                                    |
|:------:|:-------------------------------------------------------------|
|  204   | Inventário deletado com sucesso (sem corpo).                 |
|  401   | Token ausente, inválido ou expirado.                         |
|  403   | Usuário autenticado não possui `role = 0` para o inventário. |
|  404   | Inventário não encontrado para o `inv_id` informado.         |

**Exemplo de Requisição:**
```bash
curl -X DELETE https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Erros:**

| Código | Situação                                                    |
|:------:|:------------------------------------------------------------|
|  401   | Token não enviado ou inválido                               |
|  403   | Usuário autenticado não possui `role = 0` para o inventário |
|  404   | `inv_id` não encontrado ou não pertence ao usuário          |

---

**Permissões:** Este endpoint requer autenticação (JWT) e exige `role = 0` (owner) para o inventário especificado.

#### 5.2.4 GET /inventories

**Headers:**
```
Authorization: Bearer <token>
```

**Query Params (opcional):**

| Campo    | Tipo | Descrição                                             | Restrições        |
|:---------|:----:|:------------------------------------------------------|:------------------|
| `limit`  | int  | Quantidade máxima de inventários a retornar           | 1-200, padrão 200 |
| `offset` | int  | Deslocamento para paginação (offset)                  | ≥ 0               |

**Ordenação:**
`ORDER BY inv_id ASC`

**Payload:** -

**Status Codes:**

| Código | Descrição                                                                                           |
|:------:|:----------------------------------------------------------------------------------------------------|
|  200   | Lista de inventários acessíveis pelo usuário. Inclui paginação (offset). Omitir `next_page` no fim. |
|  401   | Token ausente, inválido ou expirado.                                                                |
|  404   | `offset/limit` fora do intervalo aceito (ex.: offset além do fim).                                  |

**Resposta (200):**
```json
{
  "inventories": [
    {
      "inv_id": "123e4567-e89b-12d3-a456-426614174000",
      "inv_name": "Estoque da Loja",
      "role": 0
    },
    {
      "inv_id": "223e4567-e89b-12d3-a456-426614174000",
      "inv_name": "Depósito Central",
      "role": 2
    }
  ],
  "next_page": "https://api.exemplo.com/api/v1/inventories?offset=200&limit=200"
}
```

**Exemplo de Requisição:**
```bash
curl -X GET "https://api.exemplo.com/api/v1/inventories?limit=200&offset=0" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Erros:**

| Código | Situação                                                             |
|:------:|:---------------------------------------------------------------------|
|  401   | Token não enviado ou inválido                                        |
|  404   | `offset/limit` fora do intervalo aceito (offset após o fim da lista) |

---

**Permissões:** Este endpoint requer autenticação (JWT). Retorna apenas inventários onde o usuário possui entrada em `inventory_access`.

<a id="sec-5-3"></a>
### 5.3. Inventários (Gerenciamento de Acesso)

|  ID   | Método     | Endpoint                                       | Descrição                                                                                                                                                |
|:-----:|:-----------|:-----------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------|
| 5.3.1 | **GET**    | `/api/v1/inventories/{inv_id}/users`           | Lista até 200 usuários com acesso ao inventário, incluindo suas roles. Apenas o owner (`role = 0`) pode executar esta ação.                              |
| 5.3.2 | **POST**   | `/api/v1/inventories/{inv_id}/users`           | Adiciona um usuário existente ao inventário com role de editor (`role = 1`) ou reader (`role = 2`). Apenas o owner (`role = 0`) pode executar esta ação. |
| 5.3.3 | **PATCH**  | `/api/v1/inventories/{inv_id}/users/{user_id}` | Atualiza a role de um usuário no inventário entre editor (`1`) e reader (`2`). Apenas o owner (`role = 0`) pode executar esta ação.                      |
| 5.3.4 | **DELETE** | `/api/v1/inventories/{inv_id}/users/{user_id}` | Remove o acesso de um usuário ao inventário. Apenas o owner (`role = 0`) pode executar esta ação.                                                        |

#### 5.3.1 GET /inventories/{inv_id}/users

**Headers:**
```
Authorization: Bearer <token>
```

**Path Params:**

| Campo    | Tipo | Obrigatório | Descrição                                                 |
|:---------|:----:|:-----------:|:----------------------------------------------------------|
| `inv_id` | UUID |     Sim     | Identificador do inventário cujos usuários serão listados |

**Query Params (opcional):**

| Campo    | Tipo | Descrição                                | Restrições        |
|:---------|:----:|:-----------------------------------------|:------------------|
| `limit`  | int  | Quantidade máxima de usuários a retornar | 1-200, padrão 200 |
| `offset` | int  | Deslocamento para paginação (offset)     | ≥ 0, padrão 0     |

**Payload:** -

**Ordenação:**
`ORDER BY user_id ASC`

**Status Codes:**

| Código | Descrição                                                                                      |
|:------:|:-----------------------------------------------------------------------------------------------|
|  200   | Lista de usuários com acesso ao inventário, com paginação (offset). Omitir `next_page` no fim. |
|  401   | Token ausente, inválido ou expirado.                                                           |
|  403   | Usuário autenticado não possui `role = 0` para o inventário.                                   |
|  404   | Inventário não encontrado para o `inv_id` informado; `offset/limit` fora do intervalo aceito.  |

**Resposta (200):**
```json
{
  "inventory": {
    "inv_id": "123e4567-e89b-12d3-a456-426614174000",
    "inv_name": "Estoque da Loja"
  },
  "users": [
    {
      "user_id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "João Silva",
      "login": "joao@example.com",
      "role": 0
    },
    {
      "user_id": "660e8400-e29b-41d4-a716-446655440000",
      "name": "Maria Editor",
      "login": "maria.editor@example.com",
      "role": 1
    },
    {
      "user_id": "770e8400-e29b-41d4-a716-446655440000",
      "name": "Carlos Reader",
      "login": "carlos.reader@example.com",
      "role": 2
    }
  ],
  "next_page": "https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/users?limit=200&offset=200"
}
```

**Exemplo de Requisição:**
```bash
curl -X GET "https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/users?limit=200&offset=0" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Erros:**

| Código | Situação                                                                                                        |
|:------:|:----------------------------------------------------------------------------------------------------------------|
|  401   | Token não enviado ou inválido                                                                                   |
|  403   | Usuário autenticado não possui `role = 0` para o inventário                                                     |
|  404   | `inv_id` não encontrado ou não pertence ao usuário; `offset/limit` fora do intervalo aceito (offset após o fim) |

---

**Permissões:** Este endpoint requer autenticação (JWT) e exige `role = 0` (owner) para o inventário especificado.

#### 5.3.2 POST /inventories/{inv_id}/users

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Path Params:**

| Campo    | Tipo | Obrigatório | Descrição                               |
|:---------|:----:|:-----------:|:----------------------------------------|
| `inv_id` | UUID |     Sim     | Inventário onde o acesso será concedido |

**Payload:**

| Campo   |  Tipo  | Obrigatório | Descrição                                            | Restrições                                    |
|:--------|:------:|:-----------:|:-----------------------------------------------------|:----------------------------------------------|
| `login` | string |     Sim     | Email de um usuário já existente que receberá acesso | Formato de email válido; deve existir na base |
| `role`  |  int   |     Sim     | Role atribuída ao usuário no inventário              | Apenas `1` (editor) ou `2` (reader)           |

**Status Codes:**

| Código | Descrição                                                                                      |
|:------:|:-----------------------------------------------------------------------------------------------|
|  201   | Usuário adicionado ao inventário com a role informada (`1` editor ou `2` reader).              |
|  400   | Payload inválido ou `login`/`role` ausente, email inválido ou role fora do conjunto permitido. |
|  401   | Token ausente, inválido ou expirado.                                                           |
|  403   | Usuário autenticado não possui `role = 0` para o inventário.                                   |
|  404   | Inventário não encontrado ou `login` não existe.                                               |
|  409   | Usuário já possui acesso ao inventário.                                                        |

**Resposta (201):**
```json
{
  "inventory": {
    "inv_id": "123e4567-e89b-12d3-a456-426614174000",
    "inv_name": "Estoque da Loja"
  },
  "user": {
    "user_id": "660e8400-e29b-41d4-a716-446655440000",
    "name": "Maria Editor",
    "login": "maria.editor@example.com",
    "role": 1
  }
}
```

**Exemplo de Requisição:**
```bash
curl -X POST https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "login": "maria.editor@example.com",
    "role": 1
  }'
```

**Erros:**

| Código | Situação                                                                  |
|:------:|:--------------------------------------------------------------------------|
|  400   | `login` vazio ou fora do formato de email; `role` diferente de `1` ou `2` |
|  401   | Token não enviado ou inválido                                             |
|  403   | Usuário autenticado não possui `role = 0` para o inventário               |
|  404   | `inv_id` não encontrado ou `login` (email) inexistente                    |
|  409   | Usuário já está vinculado ao inventário                                   |

---

**Permissões:** Este endpoint requer autenticação (JWT) e exige `role = 0` (owner) para o inventário especificado.

#### 5.3.3 PATCH /inventories/{inv_id}/users/{user_id}

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Path Params:**

| Campo     | Tipo | Obrigatório | Descrição                                   |
|:----------|:----:|:-----------:|:--------------------------------------------|
| `inv_id`  | UUID |     Sim     | Inventário no qual a role será atualizada   |
| `user_id` | UUID |     Sim     | Usuário cuja role será alterada             |

**Payload:**

| Campo  | Tipo | Obrigatório | Descrição                          | Restrições                          |
|:-------|:----:|:-----------:|:-----------------------------------|:------------------------------------|
| `role` | int  |     Sim     | Nova role do usuário no inventário | Apenas `1` (editor) ou `2` (reader) |

**Status Codes:**

| Código | Descrição                                                                                  |
|:------:|:-------------------------------------------------------------------------------------------|
|  200   | Role do usuário atualizada com sucesso.                                                    |
|  400   | Payload inválido ou `role` ausente/fora do conjunto permitido.                             |
|  401   | Token ausente, inválido ou expirado.                                                       |
|  403   | Usuário autenticado não possui `role = 0` para o inventário.                               |
|  404   | Inventário não encontrado, `user_id` inexistente ou vínculo com o inventário não existe.   |
|  409   | Owner não pode alterar a própria role no inventário.                                       |

**Resposta (200):**
```json
{
  "inventory": {
    "inv_id": "123e4567-e89b-12d3-a456-426614174000",
    "inv_name": "Estoque da Loja"
  },
  "user": {
    "user_id": "660e8400-e29b-41d4-a716-446655440000",
    "name": "Maria Editor",
    "login": "maria.editor@example.com",
    "role": 2
  }
}
```

**Exemplo de Requisição:**
```bash
curl -X PATCH https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/users/660e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "role": 2
  }'
```

**Erros:**

| Código | Situação                                                                          |
|:------:|:----------------------------------------------------------------------------------|
|  400   | `role` diferente de `1` ou `2`                                                    |
|  401   | Token não enviado ou inválido                                                     |
|  403   | Usuário autenticado não possui `role = 0` para o inventário                       |
|  404   | `inv_id` não encontrado, `user_id` inexistente ou vínculo não existe              |
|  409   | `user_id` é o mesmo do owner autenticado; alteração da própria role não permitida |

---

**Permissões:** Este endpoint requer autenticação (JWT) e exige `role = 0` (owner) para o inventário especificado.

#### 5.3.4 DELETE /inventories/{inv_id}/users/{user_id}

**Headers:**
```
Authorization: Bearer <token>
```

**Path Params:**

| Campo     | Tipo | Obrigatório | Descrição                                  |
|:----------|:----:|:-----------:|:-------------------------------------------|
| `inv_id`  | UUID |     Sim     | Inventário do qual o usuário será removido |
| `user_id` | UUID |     Sim     | Usuário a ter o acesso revogado            |

**Payload:** -

**Status Codes:**

| Código | Descrição                                                    |
|:------:|:-------------------------------------------------------------|
|  204   | Acesso removido com sucesso (sem corpo).                     |
|  401   | Token ausente, inválido ou expirado.                         |
|  403   | Usuário autenticado não possui `role = 0` para o inventário. |
|  404   | Inventário ou usuário não encontrado, ou relação não existe. |
|  409   | Owner não pode remover o próprio acesso ao inventário.       |

**Exemplo de Requisição:**
```bash
curl -X DELETE https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/users/660e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Erros:**

| Código | Situação                                                             |
|:------:|:---------------------------------------------------------------------|
|  401   | Token não enviado ou inválido                                        |
|  403   | Usuário autenticado não possui `role = 0` para o inventário          |
|  404   | `inv_id` não encontrado, `user_id` inexistente ou vínculo não existe |
|  409   | `user_id` é o mesmo do owner autenticado; auto-remoção não permitida |

---

**Permissões:** Este endpoint requer autenticação (JWT) e exige `role = 0` (owner) para o inventário especificado.

<a id="sec-5-4"></a>
### 5.4. Tipos

|  ID   | Método     | Endpoint                                       | Descrição                                                      |
|:-----:|:-----------|:-----------------------------------------------|:---------------------------------------------------------------|
| 5.4.1 | **GET**    | `/api/v1/inventories/{inv_id}/types`           | Lista até 200 tipos. Inclui `next_page`.                       |
| 5.4.2 | **GET**    | `/api/v1/inventories/{inv_id}/types/{type_id}` | Retorna um tipo específico pelo `type_id`.                     |
| 5.4.3 | **POST**   | `/api/v1/inventories/{inv_id}/types`           | **Cria** um novo tipo. Retorna o `type_id`.                    |
| 5.4.4 | **PATCH**  | `/api/v1/inventories/{inv_id}/types/{type_id}` | **Atualiza** parcialmente dados de um tipo.                    |
| 5.4.5 | **DELETE** | `/api/v1/inventories/{inv_id}/types/{type_id}` | **Remove** o tipo. Retorna erro se houver produtos vinculados. |

#### 5.4.1 GET /inventories/{inv_id}/types

**Headers:**
```
Authorization: Bearer <token>
```

**Path Params:**

| Campo    | Tipo | Obrigatório | Descrição                             |
|:---------|:----:|:-----------:|:--------------------------------------|
| `inv_id` | UUID |     Sim     | Inventário cujos tipos serão listados |

**Query Params (opcional):**

| Campo    | Tipo | Descrição                             | Restrições        |
|:---------|:----:|:--------------------------------------|:------------------|
| `limit`  | int  | Quantidade máxima de tipos a retornar | 1-200, padrão 200 |
| `offset` | int  | Deslocamento para paginação (offset)  | ≥ 0, padrão 0     |

**Ordenação:**
`ORDER BY type_id ASC`

**Payload:** -

**Status Codes:**

| Código | Descrição                                                                                     |
|:------:|:----------------------------------------------------------------------------------------------|
|  200   | Lista de tipos do inventário, com paginação (offset). Omitir `next_page` no fim.              |
|  401   | Token ausente, inválido ou expirado.                                                          |
|  404   | Inventário não encontrado para o `inv_id` informado; `offset/limit` fora do intervalo aceito. |

**Resposta (200):**
```json
{
  "types": [
    {
      "type_id": 1,
      "nome": "Bebidas",
      "has_image": true
    },
    {
      "type_id": 2,
      "nome": "Alimentos",
      "has_image": false
    }
  ],
  "next_page": "https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/types?limit=200&offset=200"
}
```

**Exemplo de Requisição:**
```bash
curl -X GET "https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/types?limit=200&offset=0" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Erros:**

| Código | Situação                                                                                                        |
|:------:|:----------------------------------------------------------------------------------------------------------------|
|  401   | Token não enviado ou inválido                                                                                   |
|  404   | `inv_id` não encontrado ou não pertence ao usuário; `offset/limit` fora do intervalo aceito (offset após o fim) |

---

**Permissões:** Este endpoint requer autenticação (JWT). Retorna apenas tipos pertencentes ao inventário onde o usuário tem acesso.

#### 5.4.2 GET /inventories/{inv_id}/types/{type_id}

**Headers:**
```
Authorization: Bearer <token>
```

**Path Params:**

| Campo     | Tipo  | Obrigatório | Descrição                             |
|:----------|:-----:|:-----------:|:--------------------------------------|
| `inv_id`  | UUID  |     Sim     | Inventário ao qual o tipo pertence    |
| `type_id` | int64 |     Sim     | Identificador do tipo a ser retornado |

**Payload:** -

**Status Codes:**

| Código | Descrição                                                             |
|:------:|:----------------------------------------------------------------------|
|  200   | Tipo encontrado e retornado.                                          |
|  401   | Token ausente, inválido ou expirado.                                  |
|  404   | Tipo ou inventário não encontrado para os identificadores informados. |

**Resposta (200):**
```json
{
  "type_id": 1,
  "nome": "Bebidas",
  "imagem": "data:image/png;base64,iVBORw0KGgoAAA...",
  "inv_id": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Exemplo de Requisição:**
```bash
curl -X GET https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/types/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Erros:**

| Código | Situação                                                        |
|:------:|:----------------------------------------------------------------|
|  401   | Token não enviado ou inválido                                   |
|  404   | `inv_id` ou `type_id` não encontrado ou não pertence ao usuário |

---

**Permissões:** Este endpoint requer autenticação (JWT). Retorna apenas tipos do inventário ao qual o usuário autenticado tem acesso.

#### 5.4.3 POST /inventories/{inv_id}/types

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Path Params:**

| Campo    | Tipo | Obrigatório | Descrição                          |
|:---------|:----:|:-----------:|:-----------------------------------|
| `inv_id` | UUID |     Sim     | Inventário onde o tipo será criado |

**Payload:**

| Campo    |       Tipo        | Obrigatório | Descrição                | Restrições                                                              |
|:---------|:-----------------:|:-----------:|:-------------------------|:------------------------------------------------------------------------|
| `nome`   |      string       |     Sim     | Nome do tipo             | 1-255 caracteres                                                        |
| `imagem` | string (data URI) |     Não     | Imagem associada ao tipo | Formato `data:<mime>;base64,<dados>`, MIME obrigatório; tamanho ≤ 5 MiB |

**Status Codes:**

| Código | Descrição                                                                        |
|:------:|:---------------------------------------------------------------------------------|
|  201   | Tipo criado com sucesso. Retorna o `type_id`.                                    |
|  400   | Payload inválido, imagem fora do formato `data:<mime>;base64` ou acima de 5 MiB. |
|  401   | Token ausente, inválido ou expirado.                                             |
|  403   | Usuário autenticado não possui permissão de escrita no inventário.               |
|  404   | Inventário não encontrado para o `inv_id` informado.                             |
|  409   | Já existe outro tipo com o mesmo `nome` no inventário.                           |

**Resposta (201):**
```json
{
  "type_id": 10,
  "nome": "Higiene",
  "imagem": null,
  "inv_id": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Exemplo de Requisição:**
```bash
curl -X POST https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/types \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "nome": "Higiene",
    "imagem": null
  }'
```

**Erros:**

| Código | Situação                                                                                                 |
|:------:|:---------------------------------------------------------------------------------------------------------|
|  400   | `nome` vazio ou acima de 255 caracteres; `imagem` fora do formato `data:<mime>;base64` ou acima de 5 MiB |
|  401   | Token não enviado ou inválido                                                                            |
|  403   | Usuário autenticado não possui `role = 0` ou `role = 1` para o inventário                                |
|  404   | `inv_id` não encontrado ou não pertence ao usuário                                                       |
|  409   | Já existe um tipo com o mesmo `nome` nesse inventário                                                    |

---

**Permissões:** Este endpoint requer autenticação (JWT) e exige permissão de escrita no inventário: `role = 0` (owner) ou `role = 1` (editor).

#### 5.4.4 PATCH /inventories/{inv_id}/types/{type_id}

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Path Params:**

| Campo     | Tipo  | Obrigatório | Descrição                              |
|:----------|:-----:|:-----------:|:---------------------------------------|
| `inv_id`  | UUID  |     Sim     | Inventário ao qual o tipo pertence     |
| `type_id` | int64 |     Sim     | Identificador do tipo a ser atualizado |

**Payload (campos opcionais, patch):**

| Campo    |            Tipo             | Obrigatório | Descrição                                          | Restrições                                                                         |
|:---------|:---------------------------:|:-----------:|:---------------------------------------------------|:-----------------------------------------------------------------------------------|
| `nome`   |           string            |     Não     | Novo nome do tipo                                  | 1-255 caracteres                                                                   |
| `imagem` | string (data URI) ou `null` |     Não     | Substitui a imagem do tipo; `null` remove a imagem | Se string: formato `data:<mime>;base64,<dados>`, MIME obrigatório; tamanho ≤ 5 MiB |

**Status Codes:**

| Código | Descrição                                                                                         |
|:------:|:--------------------------------------------------------------------------------------------------|
|  200   | Tipo atualizado com sucesso.                                                                      |
|  400   | Payload inválido (nenhum campo enviado ou imagem fora de `data:<mime>;base64` ou acima de 5 MiB). |
|  401   | Token ausente, inválido ou expirado.                                                              |
|  403   | Usuário autenticado não possui permissão de escrita no inventário.                                |
|  404   | Tipo ou inventário não encontrado para os identificadores informados.                             |
|  409   | Já existe outro tipo com o mesmo `nome` no inventário.                                            |

**Resposta (200):**
```json
{
  "type_id": 10,
  "nome": "Higiene e Limpeza",
  "imagem": null,
  "inv_id": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Exemplo de Requisição:**
```bash
curl -X PATCH https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/types/10 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "nome": "Higiene e Limpeza",
    "imagem": null
  }'
```

**Erros:**

| Código | Situação                                                                                                     |
|:------:|:-------------------------------------------------------------------------------------------------------------|
|  400   | Nenhum campo enviado; `nome` fora do limite; `imagem` fora do formato `data:<mime>;base64` ou acima de 5 MiB |
|  401   | Token não enviado ou inválido                                                                                |
|  403   | Usuário autenticado não possui `role = 0` ou `role = 1` para o inventário                                    |
|  404   | `inv_id` ou `type_id` não encontrado ou não pertence ao usuário                                              |
|  409   | Já existe um tipo com o mesmo `nome` nesse inventário                                                        |

---

**Permissões:** Este endpoint requer autenticação (JWT) e exige permissão de escrita no inventário: `role = 0` (owner) ou `role = 1` (editor).

#### 5.4.5 DELETE /inventories/{inv_id}/types/{type_id}

**Headers:**
```
Authorization: Bearer <token>
```

**Path Params:**

| Campo     | Tipo  | Obrigatório | Descrição                            |
|:----------|:-----:|:-----------:|:-------------------------------------|
| `inv_id`  | UUID  |     Sim     | Inventário ao qual o tipo pertence   |
| `type_id` | int64 |     Sim     | Identificador do tipo a ser removido |

**Payload:** -

**Status Codes:**

| Código | Descrição                                                             |
|:------:|:----------------------------------------------------------------------|
|  204   | Tipo removido com sucesso (sem corpo).                                |
|  401   | Token ausente, inválido ou expirado.                                  |
|  403   | Usuário autenticado não possui permissão de escrita no inventário.    |
|  404   | Tipo ou inventário não encontrado para os identificadores informados. |
|  409   | Não é possível remover: existem produtos vinculados ao tipo.          |

**Exemplo de Requisição:**
```bash
curl -X DELETE https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/types/10 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Erros:**

| Código | Situação                                                                  |
|:------:|:--------------------------------------------------------------------------|
|  401   | Token não enviado ou inválido                                             |
|  403   | Usuário autenticado não possui `role = 0` ou `role = 1` para o inventário |
|  404   | `inv_id` ou `type_id` não encontrado ou não pertence ao usuário           |
|  409   | Existem produtos associados ao tipo (`ON DELETE RESTRICT`)                |

---

**Permissões:** Este endpoint requer autenticação (JWT) e exige permissão de escrita no inventário: `role = 0` (owner) ou `role = 1` (editor). Não permite exclusão se houver produtos vinculados.

<a id="sec-5-5"></a>
### 5.5. Produtos

|  ID   | Método     | Endpoint                                                | Descrição                                         |
|:-----:|:-----------|:--------------------------------------------------------|:--------------------------------------------------|
| 5.5.1 | **GET**    | `/api/v1/inventories/{inv_id}/products`                 | Retorna 200 produtos. Inclui `next_page`.         |
| 5.5.2 | **GET**    | `/api/v1/inventories/{inv_id}/products/{product_id}`    | Retorna um produto específico pelo `product_id`.  |
| 5.5.3 | **GET**    | `/api/v1/inventories/{inv_id}/types/{type_id}/products` | Lista produtos filtrados por um tipo específico.  |
| 5.5.4 | **POST**   | `/api/v1/inventories/{inv_id}/products`                 | **Cria** um novo produto. Retorna o `product_id`. |
| 5.5.5 | **PATCH**  | `/api/v1/inventories/{inv_id}/products/{product_id}`    | **Atualiza** parcialmente dados de um produto.    |
| 5.5.6 | **DELETE** | `/api/v1/inventories/{inv_id}/products/{product_id}`    | **Remove** o produto.                             |

#### 5.5.1 GET /inventories/{inv_id}/products

**Headers:**
```
Authorization: Bearer <token>
```

**Path Params:**

| Campo    | Tipo | Obrigatório | Descrição                                |
|:---------|:----:|:-----------:|:-----------------------------------------|
| `inv_id` | UUID |     Sim     | Inventário cujos produtos serão listados |

**Query Params (opcional):**

| Campo    | Tipo | Descrição                                | Restrições        |
|:---------|:----:|:-----------------------------------------|:------------------|
| `limit`  | int  | Quantidade máxima de produtos a retornar | 1-200, padrão 200 |
| `offset` | int  | Deslocamento para paginação (offset)     | ≥ 0, padrão 0     |

**Ordenação:**
`ORDER BY product_id ASC`

**Payload:** -

**Status Codes:**

| Código | Descrição                                                                                     |
|:------:|:----------------------------------------------------------------------------------------------|
|  200   | Lista de produtos do inventário, com paginação (offset). Omitir `next_page` no fim.           |
|  401   | Token ausente, inválido ou expirado.                                                          |
|  404   | Inventário não encontrado para o `inv_id` informado; `offset/limit` fora do intervalo aceito. |

**Resposta (200):**
```json
{
  "products": [
    {
      "product_id": 101,
      "type_id": 1,
      "marca": "Acme",
      "dataValidade": "2026-12-31T00:00:00-03:00",
      "unidade": 1,
      "unidadeMedida": "un",
      "quantidade": 50,
      "inv_id": "123e4567-e89b-12d3-a456-426614174000"
    },
    {
      "product_id": 102,
      "type_id": 2,
      "marca": "FreshFarm",
      "dataValidade": null,
      "unidade": 500,
      "unidadeMedida": "g",
      "quantidade": 20,
      "inv_id": "123e4567-e89b-12d3-a456-426614174000"
    }
  ],
  "next_page": "https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/products?limit=200&offset=200"
}
```

**Exemplo de Requisição:**
```bash
curl -X GET "https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/products?limit=200&offset=0" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Erros:**

| Código | Situação                                                                                                        |
|:------:|:----------------------------------------------------------------------------------------------------------------|
|  401   | Token não enviado ou inválido                                                                                   |
|  404   | `inv_id` não encontrado ou não pertence ao usuário; `offset/limit` fora do intervalo aceito (offset após o fim) |

---

**Permissões:** Este endpoint requer autenticação (JWT). Retorna apenas produtos do inventário onde o usuário tem acesso.

#### 5.5.2 GET /inventories/{inv_id}/products/{product_id}

**Headers:**
```
Authorization: Bearer <token>
```

**Path Params:**

| Campo        | Tipo  | Obrigatório | Descrição                             |
|:-------------|:-----:|:-----------:|:--------------------------------------|
| `inv_id`     | UUID  |     Sim     | Inventário ao qual o produto pertence |
| `product_id` | int64 |     Sim     | Identificador do produto              |

**Payload:** -

**Status Codes:**

| Código | Descrição                                                                |
|:------:|:-------------------------------------------------------------------------|
|  200   | Produto encontrado e retornado.                                          |
|  401   | Token ausente, inválido ou expirado.                                     |
|  404   | Produto ou inventário não encontrado para os identificadores informados. |

**Resposta (200):**
```json
{
  "product_id": 101,
  "type_id": 1,
  "marca": "Acme",
  "dataValidade": "2026-12-31T00:00:00-03:00",
  "unidade": 1,
  "unidadeMedida": "un",
  "quantidade": 50,
  "inv_id": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Exemplo de Requisição:**
```bash
curl -X GET https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/products/101 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Erros:**

| Código | Situação                                                           |
|:------:|:-------------------------------------------------------------------|
|  401   | Token não enviado ou inválido                                      |
|  404   | `inv_id` ou `product_id` não encontrado ou não pertence ao usuário |

---

**Permissões:** Este endpoint requer autenticação (JWT). Retorna apenas produtos do inventário ao qual o usuário autenticado tem acesso.

#### 5.5.3 GET /inventories/{inv_id}/types/{type_id}/products

**Headers:**
```
Authorization: Bearer <token>
```

**Path Params:**

| Campo     | Tipo  | Obrigatório | Descrição                                  |
|:----------|:-----:|:-----------:|:-------------------------------------------|
| `inv_id`  | UUID  |     Sim     | Inventário cujos produtos serão listados   |
| `type_id` | int64 |     Sim     | Tipo pelo qual os produtos serão filtrados |

**Query Params (opcional):**

| Campo    | Tipo | Descrição                                | Restrições        |
|:---------|:----:|:-----------------------------------------|:------------------|
| `limit`  | int  | Quantidade máxima de produtos a retornar | 1-200, padrão 200 |
| `offset` | int  | Deslocamento para paginação              | ≥ 0, padrão 0     |

**Ordenação:**
`ORDER BY product_id ASC`

**Payload:** -

**Status Codes:**

| Código | Descrição                                                                                                            |
|:------:|:---------------------------------------------------------------------------------------------------------------------|
|  200   | Lista de produtos do inventário filtrados pelo tipo, com paginação (offset). Omitir `next_page` no fim.              |
|  401   | Token ausente, inválido ou expirado.                                                                                 |
|  404   | Inventário não encontrado; tipo não encontrado ou não pertence ao `inv_id`; `offset/limit` fora do intervalo aceito. |

**Resposta (200):**
```json
{
  "products": [
    {
      "product_id": 201,
      "type_id": 1,
      "marca": "Acme",
      "dataValidade": "2026-12-31T00:00:00-03:00",
      "unidade": 1,
      "unidadeMedida": "un",
      "quantidade": 50,
      "inv_id": "123e4567-e89b-12d3-a456-426614174000"
    }
  ],
  "next_page": "https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/types/1/products?limit=200&offset=200"
}
```

**Exemplo de Requisição:**
```bash
curl -X GET "https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/types/1/products?limit=200&offset=0" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Erros:**

| Código | Situação                                                                                                                     |
|:------:|:-----------------------------------------------------------------------------------------------------------------------------|
|  401   | Token não enviado ou inválido                                                                                                |
|  404   | `inv_id` ou `type_id` não encontrado ou não pertence ao usuário; `offset/limit` fora do intervalo aceito (offset após o fim) |

---

**Permissões:** Este endpoint requer autenticação (JWT). Retorna apenas produtos do inventário ao qual o usuário autenticado tem acesso.

#### 5.5.4 POST /inventories/{inv_id}/products

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Path Params:**

| Campo    | Tipo | Obrigatório | Descrição                             |
|:---------|:----:|:-----------:|:--------------------------------------|
| `inv_id` | UUID |     Sim     | Inventário onde o produto será criado |

**Payload:**

| Campo           |          Tipo           | Obrigatório | Descrição                              | Restrições                                                      |
|:----------------|:-----------------------:|:-----------:|:---------------------------------------|:----------------------------------------------------------------|
| `type_id`       |          int64          |     Sim     | ID do tipo ao qual o produto pertence  | Deve existir e pertencer ao mesmo inventário                    |
| `marca`         |         string          |     Não     | Marca ou fabricante                    | Até 255 caracteres                                              |
| `dataValidade`  | timestamp with timezone |     Não     | Data de validade                       | ISO 8601 com offset explícito (ex: `2026-12-31T00:00:00-03:00`) |
| `unidade`       |          int64          |     Não     | Unidade de medida base (ex: 1, 500)    | 0 a 999999999999999999                                          |
| `unidadeMedida` |         string          |     Não     | Texto da unidade (ex: `un`, `g`, `ml`) | Até 255 caracteres                                              |
| `quantidade`    |          int64          |     Não     | Quantidade inicial                     | 0 a 999999999999999999, padrão 0                                |

**Status Codes:**

| Código | Descrição                                                             |
|:------:|:----------------------------------------------------------------------|
|  201   | Produto criado. Retorna o `product_id`.                               |
|  400   | Payload malformatado ou campos obrigatórios ausentes.                 |
|  401   | Token ausente, inválido ou expirado.                                  |
|  403   | Usuário autenticado não possui permissão de escrita no inventário.    |
|  404   | Inventário ou tipo não encontrado para os identificadores informados. |

**Resposta (201):**
```json
{
  "product_id": 301,
  "type_id": 1,
  "marca": "Acme",
  "dataValidade": "2026-12-31T00:00:00-03:00",
  "unidade": 1,
  "unidadeMedida": "un",
  "quantidade": 50,
  "inv_id": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Exemplo de Requisição:**
```bash
curl -X POST https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "type_id": 1,
    "marca": "Acme",
    "dataValidade": "2026-12-31T00:00:00-03:00",
    "unidade": 1,
    "unidadeMedida": "un",
    "quantidade": 50
  }'
```

**Erros:**

| Código | Situação                                                                  |
|:------:|:--------------------------------------------------------------------------|
|  400   | Payload malformatado ou campos obrigatórios ausentes                      |
|  401   | Token não enviado ou inválido                                             |
|  403   | Usuário autenticado não possui `role = 0` ou `role = 1` para o inventário |
|  404   | `inv_id` ou `type_id` não encontrado ou não pertence ao usuário           |

---

**Permissões:** Este endpoint requer autenticação (JWT) e exige permissão de escrita no inventário: `role = 0` (owner) ou `role = 1` (editor).

#### 5.5.5 PATCH /inventories/{inv_id}/products/{product_id}

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Path Params:**

| Campo        | Tipo  | Obrigatório | Descrição                                 |
|:-------------|:-----:|:-----------:|:------------------------------------------|
| `inv_id`     | UUID  |     Sim     | Inventário ao qual o produto pertence     |
| `product_id` | int64 |     Sim     | Identificador do produto a ser atualizado |

**Payload (campos opcionais, patch):**

| Campo           |          Tipo           | Obrigatório | Descrição            | Restrições                                   |
|:----------------|:-----------------------:|:-----------:|:---------------------|:---------------------------------------------|
| `type_id`       |          int64          |     Não     | Novo tipo do produto | Deve existir e pertencer ao mesmo inventário |
| `marca`         |         string          |     Não     | Marca ou fabricante  | Até 255 caracteres                           |
| `dataValidade`  | timestamp with timezone |     Não     | Data de validade     | ISO 8601 com offset explícito                |
| `unidade`       |          int64          |     Não     | Unidade base         | 0 a 999999999999999999                       |
| `unidadeMedida` |         string          |     Não     | Texto da unidade     | Até 255 caracteres                           |
| `quantidade`    |          int64          |     Não     | Quantidade           | 0 a 999999999999999999                       |

**Status Codes:**

| Código | Descrição                                                                  |
|:------:|:---------------------------------------------------------------------------|
|  200   | Produto atualizado com sucesso.                                            |
|  400   | Payload inválido (nenhum campo enviado ou violações de formato/tamanho).   |
|  401   | Token ausente, inválido ou expirado.                                       |
|  403   | Usuário autenticado não possui permissão de escrita no inventário.         |
|  404   | Produto ou inventário não encontrado; tipo não pertence ao mesmo `inv_id`. |

**Resposta (200):**
```json
{
  "product_id": 301,
  "type_id": 2,
  "marca": "Acme",
  "dataValidade": "2027-01-01T00:00:00-03:00",
  "unidade": 500,
  "unidadeMedida": "g",
  "quantidade": 75,
  "inv_id": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Exemplo de Requisição:**
```bash
curl -X PATCH https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/products/301 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "type_id": 2,
    "dataValidade": "2027-01-01T00:00:00-03:00",
    "unidade": 500,
    "unidadeMedida": "g",
    "quantidade": 75
  }'
```

**Erros:**

| Código | Situação                                                                      |
|:------:|:------------------------------------------------------------------------------|
|  400   | Nenhum campo enviado; campos fora de faixa ou formato inválido                |
|  401   | Token não enviado ou inválido                                                 |
|  403   | Usuário autenticado não possui `role = 0` ou `role = 1` para o inventário     |
|  404   | `inv_id`, `product_id` ou `type_id` não encontrado ou não pertence ao usuário |

---

**Permissões:** Este endpoint requer autenticação (JWT) e exige permissão de escrita no inventário: `role = 0` (owner) ou `role = 1` (editor).

#### 5.5.6 DELETE /inventories/{inv_id}/products/{product_id}

**Headers:**
```
Authorization: Bearer <token>
```

**Path Params:**

| Campo        | Tipo  | Obrigatório | Descrição                               |
|:-------------|:-----:|:-----------:|:----------------------------------------|
| `inv_id`     | UUID  |     Sim     | Inventário ao qual o produto pertence   |
| `product_id` | int64 |     Sim     | Identificador do produto a ser removido |

**Payload:** -

**Status Codes:**

| Código | Descrição                                                                |
|:------:|:-------------------------------------------------------------------------|
|  204   | Produto removido com sucesso (sem corpo).                                |
|  401   | Token ausente, inválido ou expirado.                                     |
|  403   | Usuário autenticado não possui permissão de escrita no inventário.       |
|  404   | Produto ou inventário não encontrado para os identificadores informados. |

**Exemplo de Requisição:**
```bash
curl -X DELETE https://api.exemplo.com/api/v1/inventories/123e4567-e89b-12d3-a456-426614174000/products/301 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Erros:**

| Código | Situação                                                                  |
|:------:|:--------------------------------------------------------------------------|
|  401   | Token não enviado ou inválido                                             |
|  403   | Usuário autenticado não possui `role = 0` ou `role = 1` para o inventário |
|  404   | `inv_id` ou `product_id` não encontrado ou não pertence ao usuário        |

---

**Permissões:** Este endpoint requer autenticação (JWT) e exige permissão de escrita no inventário: `role = 0` (owner) ou `role = 1` (editor).

---

<a id="sec-6"></a>
## 6. Infraestrutura e Redirecionamento (Nginx)

O Nginx deve ser configurado para gerenciar o tráfego e proteger o servidor de aplicação.

* **Encaminhamento:** `location /api/ { proxy_pass http://backend:53919; }`
* **CORS:** Configurado para permitir apenas a origem da aplicação mobile.
* **Uploads:** `client_max_body_size 5M;` (necessário para payloads que enviam o campo `imagem` nos endpoints de tipos).
* **Banco via Docker:** PostgreSQL em container com volume persistente montado (dados fora do container).
* **Migração/Inicialização:** scripts executados em ordem lexicográfica a partir de `/docker-entrypoint-initdb.d`. O arquivo `00_configure.sql` prepara `cuscrud_app`/`cuscrud` e define a timezone; o arquivo `01_schema.sql` conecta em `cuscrud`, habilita `uuid-ossp` e cria tabelas, índices e constraints.

**Exemplo de server block Nginx**
```nginx
server {
    listen 80;
    server_name api.exemplo.com;

    client_max_body_size 5M;

    location /api/ {
        proxy_pass http://backend:53919;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**Exemplo mínimo de docker-compose (backend + Postgres)**
```yaml
version: "3.9"
services:
  backend:
    build: ./backend
    container_name: cuscrud-backend
    ports:
      - "53919:53919"
    env_file: .env
    environment:
      - DB_URL=jdbc:postgresql://db:5432/cuscrud
      - DB_USER=cuscrud_app
      - DB_PASSWORD=${CUSCRUD_APP_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - LOG_DIR=logs
      - LOG_LEVEL=INFO
    depends_on:
      - db
    volumes:
      - ./backend/logs:/app/logs
  db:
    image: postgres:17
    container_name: cuscrud-db
    env_file: .env
    environment:
      - POSTGRES_DB=postgres
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=${POSTGRES_SUPERUSER_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - db_data:/var/lib/postgresql/data
      - ./db/init:/docker-entrypoint-initdb.d:ro
volumes:
  db_data:
```

**Configuração via .env**
- `POSTGRES_SUPERUSER_PASSWORD`
- `CUSCRUD_APP_PASSWORD`
- `JWT_SECRET`
- `LOG_DIR`
- `LOG_LEVEL`

**Logging**
- Logar todos os erros 4xx/5xx.
- Nível `warning`: logar também todos os requests de autenticação e de alteração de dados (POST, PATCH, DELETE).
- Nível `debug`: logar todos os requests.
- Cada entrada de log deve incluir `request_id` e `client_ip`.
- O `request_id` deve conter o IP do cliente e um sufixo aleatório para rastreio.
- O backend deve escrever dois arquivos de log:
  1. log geral: `cuscrud-backend.log`
  2. log dedicado da aplicação: `cuscrud-backend-application.log`
- O arquivo dedicado da aplicação deve receber apenas logs do namespace `br.com.cuscrudrest`.
- O logger root deve permanecer em `warning`.
- O nível configurável via `LOG_LEVEL` deve se aplicar ao namespace `br.com.cuscrudrest`.
- Rotação de logs: arquivos de até 10 MB; manter no máximo 10 arquivos; arquivos `.2` a `.10` compactados em `.gz`.
- Quando `logrotate` for usado, a rotação deve cobrir tanto o log geral quanto o log dedicado da aplicação.
- Pode usar o sistema de logging do Linux (ex.: `logrotate` ou journald) se for a opção mais simples.
- IO/Blocking: escrita síncrona em arquivo (append); rotação pode bloquear brevemente na troca de arquivo, mas requests não devem ser bloqueados esperando compressão (executar compressão em tarefa separada/assíncrona).
- Em ambiente com Docker Compose, o backend deve persistir os logs fora do container montando `./backend/logs` no caminho `/app/logs`.
- O valor padrão de `LOG_DIR` para este projeto é `logs`.
- O valor padrão de `LOG_LEVEL` para este projeto é `INFO`, aplicado ao logger `br.com.cuscrudrest`.

---

<a id="sec-7"></a>
## 7. Convenções de código

<a id="sec-7-1"></a>
### 7.1. Documentação

- Toda função deve ter Doc Comment no formato nativo da linguagem.
- Descreva de forma explícita: propósito da função (o que entrega), estratégia de funcionamento (como faz) e efeitos colaterais ou recursos que pode alterar.
- Para cada parâmetro, documente nome, tipo, significado e restrições relevantes. Para o retorno, informe tipo e significado.
- Liste as exceções/erros que a função pode lançar e os gatilhos de cada uma.
- Quando um trecho crítico não for autoexplicativo, adicione um comentário curto explicando a decisão ou efeito.

<a id="sec-7-2"></a>
### 7.2. Testes

- Cada função precisa de testes de unidade, preferencialmente escritos antes da implementação (TDD).
- Cada teste deve ter Doc Comment resumindo: objetivo do teste, entradas usadas e comportamento esperado/asserts.
- Em arquivos com testes de múltiplas funções, agrupe os casos de cada função em blocos claramente marcados com comentários de início/fim do grupo.

<a id="sec-7-3"></a>
### 7.3. TDD

- Passo 1: defina a assinatura da função e escreva o Doc Comment conforme 7.1.
- Passo 2: escreva os testes cobrindo casos positivos/negativos, com Doc Comments conforme 7.2.
- Passo 3: implemente até todos os testes passarem.
- Se a interface ou o esperado mudar, volte ao Passo 1, atualize docs e refaça os testes. Se a mudança afetar outra função, repita o ciclo também para ela.

<a id="sec-7-4"></a>
### 7.4. Validação de Email

- Utilizar `Apache Commons Validator` para validação de formato de email.
- Método: `EmailValidator.getInstance().isValid(email)`.
- Validação deve ser aplicada no backend para todos os campos de email (login, cadastro, adição de usuários).
- Dependência: `commons-validator:commons-validator`.

<a id="sec-8"></a>
## 8. Especificações para agentes

- Projeto de API em fase de MVP acadêmico: siga somente o que está definido neste arquivo. Se algo parecer conflitar ou for uma gafe grave, informe o usuário para que ele revise e atualize a arquitetura.
- Respeite as Convenções de código da seção 7, cumprindo-as à risca, em especial o fluxo de TDD descrito em 7.3.
- Sempre que apropriado, recomende ao usuário criar um novo commit para registrar o progresso.
- Toda alteração em arquivos deve ser feita usando `apply_patch`, permitindo revisar diffs com clareza.
- Exemplo de fluxo TDD (Java, rota simples `GET /api/v1/health`):

  - **Doc da função (controller)**
  ```java
  /**
   * GET /api/v1/health
   * Retorna status 200 e payload simples para checagem de vida.
   * Efeitos colaterais: nenhum. Não acessa banco.
   * @return DTO com status e timestamp.
   */
  public HealthDto getHealth() { }
  ```

  - **Doc do teste**
  ```java
  /**
   * Verifica que GET /api/v1/health retorna 200 com body esperado.
   * Entrada: requisição sem auth.
   * Esperado: status 200, body com status "ok" e timestamp.
   */
  @Test
  void shouldReturnOkOnHealth() { }
  ```
  - **Teste primeiro**: implementar teste chamando o endpoint e validando status/body.
  - **Implementação**: criar handler `getHealth` retornando DTO `{status:"ok", time: now}`.
  - **Revisitar docs/testes** se assinatura/comportamento mudar.
