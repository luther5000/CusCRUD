# CusCRUDREST

Servidor de API REST para o projeto CusCRUD.

## Estrutura do repositório

```text
.
|-- architecture.md
|-- backend
|   |-- Dockerfile
|   |-- pom.xml
|   `-- src
|       |-- main
|       |   |-- java/br/com/cuscrudrest
|       |   |   |-- auth
|       |   |   |   |-- jwt
|       |   |   |   |-- login
|       |   |   |   |-- register
|       |   |   |   |-- support
|       |   |   |   |-- user
|       |   |   |   `-- validate
|       |   |   |-- common
|       |   |   |   `-- error
|       |   |   `-- health
|       |   `-- resources
|       `-- test
|           `-- java/br/com/cuscrudrest
|               |-- auth
|               |   |-- jwt
|               |   |-- login
|               |   |-- register
|               |   |-- support
|               |   |-- user
|               |   `-- validate
|               `-- health
|-- docker-compose.yaml
|-- db
|   `-- init
|       |-- 00_configure.sql
|       `-- 01_schema.sql
`-- infra
    `-- nginx
        `-- default.conf
```

## Convenções adotadas

- Backend Java com Spring Boot como base para o esqueleto inicial.
- Acesso a dados com Spring JDBC.
- Segurança HTTP com Spring Security.
- Hash de senha com `BCryptPasswordEncoder`.
- Organização do backend por responsabilidade: borda HTTP em `auth`, casos de uso em subpacotes específicos como `auth/login`, `auth/register` e `auth/validate`, persistência em `auth/user`, utilitários em `auth/support` e erros compartilhados em `common/error`.
- Porta HTTP da aplicação: `53919`.
- Prefixo global da API: `/api/v1`.
- Banco PostgreSQL 17 com bootstrap via `docker-entrypoint-initdb.d`.
- Timezone padrão da aplicação: `America/Recife`.

## Status atual

- `GET /api/v1/health` implementado e testado.
- `POST /api/v1/auth/register` implementado e testado.
- `POST /api/v1/auth/login` implementado e testado.
- `GET /api/v1/auth/validate` implementado e testado.
- Formato padronizado de erro HTTP implementado para validação, conflito e autenticação.
- Suíte Maven passando com `mvn test`.

## Como validar localmente

1. Copie `.env.example` para `.env`.
2. Ajuste as senhas e o `JWT_SECRET`.
3. Execute `mvn test` em `backend`.
4. Execute `docker compose up --build`.
5. Valide o endpoint de saúde:

```bash
curl -i http://localhost:53919/api/v1/health
```

6. Valide o cadastro de usuário:

```bash
curl -i -X POST http://localhost:53919/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Joao Novo",
    "login": "joao.novo@example.com",
    "passwd": "senhaforte456"
  }'
```

7. Valide o login:

```bash
curl -i -X POST http://localhost:53919/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "login": "joao.novo@example.com",
    "passwd": "senhaforte456"
  }'
```

8. Valide o token retornado no login:

```bash
curl -i http://localhost:53919/api/v1/auth/validate \
  -H "Authorization: Bearer <token-retornado-no-login>"
```

## Planejamento para próximos passos

- Introduzir o fluxo JWT em Spring Security para proteger os endpoints autenticados.
- Implementar os endpoints de inventário consumindo o usuário autenticado.
- Evoluir a configuração de segurança de permissiva para protegida por JWT.
