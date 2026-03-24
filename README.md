# CusCRUDREST

Servidor de API REST para o projeto CusCRUD.

## Estrutura inicial

```text
.
|-- architecture.md
|-- backend
|   |-- Dockerfile
|   |-- pom.xml
|   `-- src
|       |-- main
|       |   |-- java/br/com/cuscrudrest
|       |   `-- resources
|       `-- test
|           `-- java/br/com/cuscrudrest
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
- Porta HTTP da aplicação: `53919`.
- Prefixo global da API: `/api/v1`.
- Banco PostgreSQL 17 com bootstrap via `docker-entrypoint-initdb.d`.
- Timezone padrão da aplicação: `America/Recife`.

## Como subir o ambiente local

1. Copie `.env.example` para `.env`.
2. Ajuste as senhas e o `JWT_SECRET`.
3. Execute `docker compose up --build`.

## Planejamento para próximos passos

- Implementar `config` compartilhada de erro, serialization e auth.
- Criar o primeiro vertical slice com TDD, preferencialmente `auth`.
- Integrar acesso ao Postgres e migrations no backend.
