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
|       |   |   |   |-- security
|       |   |   |   |-- jwt
|       |   |   |   |-- login
|       |   |   |   |-- register
|       |   |   |   |-- support
|       |   |   |   |-- user
|       |   |   |   `-- validate
|       |   |   |-- common
|       |   |   |   |-- error
|       |   |   |   `-- logging
|       |   |   |-- config
|       |   |   `-- health
|       |   |   `-- inventories
|       |   |       |-- create
|       |   |       |-- delete
|       |   |       |-- list
|       |   |       `-- rename
|       |   `-- resources
|       `-- test
|           `-- java/br/com/cuscrudrest
|               |-- auth
|               |   |-- security
|               |   |-- jwt
|               |   |-- login
|               |   |-- register
|               |   |-- support
|               |   |-- user
|               |   `-- validate
|               |-- common
|               |   `-- logging
|               |-- health
|               `-- inventories
|                   |-- create
|                   |-- delete
|                   |-- list
|                   `-- rename
|-- docker-compose.yaml
|-- db
|   `-- init
|       |-- 00_configure.sql
|       `-- 01_schema.sql
`-- infra
    |-- logrotate
    |   `-- cuscrud-backend.conf
    `-- nginx
        `-- default.conf
```

## Convenções adotadas

- Backend Java com Spring Boot como base para o esqueleto inicial.
- Acesso a dados com Spring JDBC.
- Segurança HTTP com Spring Security.
- Hash de senha com `BCryptPasswordEncoder`.
- JWT HS256 com segredo em `JWT_SECRET` e TTL fixo de 3600 segundos.
- Organização do backend por responsabilidade: borda HTTP nos pacotes raiz de domínio, casos de uso em subpacotes específicos como `auth/login`, `auth/register`, `auth/validate`, `inventories/create`, `inventories/rename`, `inventories/delete` e `inventories/list`, persistência em `auth/user` e `inventories`, utilitários em `auth/support`, segurança em `auth/security`, configuração compartilhada em `config`, erros em `common/error` e logging em `common/logging`.
- Porta HTTP da aplicação: `53919`.
- Prefixo global da API: `/api/v1`.
- Banco PostgreSQL 17 com bootstrap via `docker-entrypoint-initdb.d`.
- Timezone padrão da aplicação: `America/Recife`.
- Logging em dois arquivos:
  - `cuscrud-backend.log` para o fluxo geral
  - `cuscrud-backend-application.log` para `br.com.cuscrudrest`

## Status atual

- `GET /api/v1/health` implementado e testado.
- `POST /api/v1/auth/register` implementado e testado.
- `POST /api/v1/auth/login` implementado e testado.
- `GET /api/v1/auth/validate` implementado e testado.
- `POST /api/v1/inventories` implementado e testado.
- `PATCH /api/v1/inventories/{inv_id}` implementado e testado.
- `DELETE /api/v1/inventories/{inv_id}` implementado e testado.
- `GET /api/v1/inventories` implementado e testado.
- JWT integrado ao Spring Security para autenticar rotas protegidas.
- Formato padronizado de erro HTTP implementado para validação, conflito e autenticação.
- Formato padronizado de erro HTTP implementado para autorização (`403`) e recurso inexistente (`404`).
- Regras compartilhadas de acesso a inventário centralizadas no backend para reaproveitamento entre `PATCH`, `DELETE` e próximos endpoints protegidos.
- Logging HTTP com `request_id` e `client_ip` implementado.
- Suíte Maven passando com `82` testes.

## Como validar localmente

1. Copie `.env.example` para `.env`.
2. Ajuste as senhas, o `JWT_SECRET` e, se necessário, `LOG_LEVEL`.
3. Execute `mvn test` em `backend`.
4. Execute `docker compose up --build`.
5. Valide o endpoint de saúde (`5.0.1`):

```bash
curl -i http://localhost:53919/api/v1/health
```

6. Valide o cadastro de usuário (`5.1.2`):

```bash
curl -i -X POST http://localhost:53919/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Joao Novo",
    "login": "joao.novo@example.com",
    "passwd": "senhaforte456"
  }'
```

7. Valide o login (`5.1.1`):

```bash
curl -i -X POST http://localhost:53919/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "login": "joao.novo@example.com",
    "passwd": "senhaforte456"
  }'
```

8. Valide o token retornado no login (`5.1.3`):

```bash
curl -i http://localhost:53919/api/v1/auth/validate \
  -H "Authorization: Bearer <token-retornado-no-login>"
```

9. Valide a criação de inventário com o mesmo token (`5.2.1`):

```bash
curl -i -X POST http://localhost:53919/api/v1/inventories \
  -H "Authorization: Bearer <token-retornado-no-login>" \
  -H "Content-Type: application/json" \
  -d '{
    "inv_name": "Estoque da Loja"
  }'
```

10. Valide a listagem de inventários do usuário autenticado (`5.2.4`):

```bash
curl -i "http://localhost:53919/api/v1/inventories?limit=200&offset=0" \
  -H "Authorization: Bearer <token-retornado-no-login>"
```

11. Valide a renomeação do inventário (`5.2.2`):

```bash
curl -i -X PATCH http://localhost:53919/api/v1/inventories/<inv_id-retornado-na-criacao> \
  -H "Authorization: Bearer <token-retornado-no-login>" \
  -H "Content-Type: application/json" \
  -d '{
    "inv_name": "Estoque Renomeado"
  }'
```

12. Valide a remoção do inventário (`5.2.3`):

```bash
curl -i -X DELETE http://localhost:53919/api/v1/inventories/<inv_id-retornado-na-criacao> \
  -H "Authorization: Bearer <token-retornado-no-login>"
```

13. Se quiser inspecionar os logs gerados pela aplicação:

```bash
tail -f backend/logs/cuscrud-backend-application.log
```

## Planejamento para próximos passos

- Implementar `GET /api/v1/inventories/{inv_id}/users` com paginação e validação de owner.
- Implementar `POST /api/v1/inventories/{inv_id}/users` para adicionar usuários existentes ao inventário.
- Implementar `PATCH /api/v1/inventories/{inv_id}/users/{user_id}` e `DELETE /api/v1/inventories/{inv_id}/users/{user_id}` reaproveitando a base de acesso já criada.
- Depois disso, iniciar o conjunto de endpoints de `types`.
- Na sequência, iniciar o conjunto de endpoints de `products`.
