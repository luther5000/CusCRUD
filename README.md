# CusCRUD — Sistema de Gestão de Inventários Sociais

O **CusCRUD** é uma plataforma completa (Mobile e Server) projetada para a gestão inteligente de inventários em bancos de alimentos, ONGs e iniciativas sociais. O sistema permite organizar de forma eficiente tipos de produtos, lotes em estoque, e gerenciar permissões e acessos de múltiplos colaboradores por inventário de forma segura e auditável.

## Índice

* [Visão Geral da Solução](https://www.google.com/search?q=%23-vis%C3%A3o-geral-da-solu%C3%A7%C3%A3o)
* [Arquitetura do Repositório](https://www.google.com/search?q=%23-arquitetura-do-reposit%C3%B3rio)
* [Telas da Aplicação Mobile](https://www.google.com/search?q=%23-telas-da-aplica%C3%A7%C3%A3o-mobile)
* [Funcionalidades do Ecossistema](https://www.google.com/search?q=%23-funcionalidades-do-ecossistema)
* [Ambiente de Desenvolvimento Rápido](https://www.google.com/search?q=%23-ambiente-de-desenvolvimento-r%C3%A1pido)
* [Componentes do Ecossistema](https://www.google.com/search?q=%23-componentes-do-ecossistema)
* [1. Backend (Server API)](https://www.google.com/search?q=%231-backend-server-api)
* [2. Frontend (Mobile Android)](https://www.google.com/search?q=%232-frontend-mobile-android)


* [Licença](https://www.google.com/search?q=%23-licen%C3%A7a)

---

## Visão Geral da Solução

A plataforma é composta por duas aplicações independentes integradas:

1. **CusCRUD REST (Server):** Uma API robusta desenvolvida em **Java + Spring Boot**, que centraliza as regras de negócio, persistência em banco de dados **PostgreSQL**, segurança com **JWT** e controle de acessos adaptável.
2. **CusCRUD Mobile (App):** Um cliente Android nativo moderno desenvolvido em **Kotlin + Jetpack Compose**, seguindo os princípios da *Clean Architecture*, otimizado para operação em campo e com armazenamento local seguro de sessões.

---

## Arquitetura do Repositório

Com a unificação dos repositórios, o projeto passa a adotar uma estrutura modular no estilo monorepo:

```text
.
├── backend/               # Código-fonte da API REST (Spring Boot)
│   ├── src/               # Implementação dos casos de uso, pacotes e testes
│   ├── Dockerfile         # Containerização da aplicação Java
│   └── pom.xml            # Gerenciador de dependências Maven
├── mobile/                # Código-fonte do Aplicativo Android (Kotlin)
│   ├── src/               # Camadas Clean (data, domain, presentation, ui)
│   └── build.gradle.kts   # Configurações do módulo mobile
├── shared-test/           # Recursos e geradores de dados compartilhados do Mobile
├── db/                    # Scripts de inicialização do Banco de Dados (PostgreSQL)
│   └── init/              # Esquemas DDL (.sql) aplicados no bootstrap do container
├── docs/                  # Documentações globais (Gherkin, Diagramas de Classes, etc.)
├── infra/                 # Arquivos de infraestrutura local (Nginx reversa, Logrotate)
├── docker-compose.yaml    # Orquestração do ambiente completo de desenvolvimento
└── README.md              # Este guia global

```

---

## Telas da Aplicação Mobile

---

## ⚙️ Funcionalidades do Ecossistema

* **Autenticação Segura:** Cadastro de usuários e login com senhas criptografadas por `BCrypt` e tokens `JWT (HS256)`.
* **Escopo por Inventário:** Criação, edição e isolamento completo de dados entre inventários/ONGs distintos.
* **Gestão de Estoque:** Cadastro detalhado de itens com controle de marca, quantidade, datas de validade (lotes) e unidades de medida (`un`, `kg`, `cx`, etc.).
* **Agrupamento Inteligente:** Separação do inventário por categorias/tipos de produtos para facilitar a triagem em doações.
* **Controle de Acesso Fino (RBAC):** Adição, edição de papel e exclusão de colaboradores vinculados a um inventário específico diretamente pelo app/API.

---

## ⚡ Ambiente de Desenvolvimento Rápido

A forma mais rápida de rodar o ecossistema completo localmente é utilizando o **Docker Compose** disponível na raiz. O compose irá subir:

* A API do Backend na porta `53919`
* O Banco PostgreSQL 17
* O servidor Nginx como proxy reverso

### Passo 1: Subir o Backend + Banco

Na raiz do repositório, execute:

```bash
# 1. Copie o arquivo de variáveis de ambiente do backend e ajuste se necessário
cp backend/.env.example backend/.env

# 2. Suba toda a infraestrutura automaticamente
docker compose up --build

```

Para validar se o backend está respondendo perfeitamente, acesse o endpoint de saúde:

```bash
curl -i http://localhost:53919/api/v1/health

```

### Passo 2: Configurar e rodar o Mobile

Com o backend rodando localmente na sua máquina:

1. Abra a pasta raiz no **Android Studio**.
2. Abra ou crie o arquivo `local.properties` (dentro da pasta `mobile/` ou na raiz do projeto, dependendo da configuração do seu Gradle) e adicione a URL que aponta para o host local do emulador:
```properties
API_BASE_URL=http://10.0.2.2:8080/api/v1/

```


*(Nota: `10.0.2.2` é o endereço padrão que o emulador Android usa para enxergar o `localhost` da máquina hospedeira).*
3. Sincronize o Gradle e execute o módulo `mobile` no seu dispositivo ou emulador.

---

## Componentes do Ecossistema

### 1. Backend (Server API)

Construído com foco em arquitetura limpa orientada a casos de uso e alta taxa de cobertura de testes.

* **Tecnologias principais:** Java 17, Spring Boot, Spring Security, Spring JDBC, PostgreSQL 17, Maven.
* **Logs organizados:** O backend gera logs rotacionáveis divididos em `cuscrud-backend.log` (fluxo de infra e rotas) e `cuscrud-backend-application.log` (fluxo exclusivo do domínio de negócio).
* **Como testar manualmente (Maven):**
```bash
cd backend/
mvn test

```



> Para detalhes sobre rotas disponíveis da API, payloads do cURL e convenções detalhadas, veja o [README do Backend](https://www.google.com/search?q=./backend/README.md).

### 2. Frontend (Mobile Android)

App construído sob o paradigma reativo e focado em padrões estritos de arquitetura móvel testável (*Clean Architecture* + *MVVM/MVP*).

* **Tecnologias principais:** Kotlin 2.1, Jetpack Compose, Navigation Compose, Hilt (Injeção de dependência), Retrofit + OkHttp, Jetpack DataStore (persistência local de sessão).
* **Alvos de Build:** `compileSdk = 35`, `minSdk = 21`.
* **Como rodar a suíte de testes:**
```bash
# Na raiz do projeto, execute:

# Testes Unitários de JVM
./gradlew :mobile:testDebugUnitTest

# Testes Instrumentados e de UI (requer emulador/celular conectado)
./gradlew :mobile:connectedDebugAndroidTest

```



> Para detalhes de organização de pastas da arquitetura móvel, veja o [README do Mobile](https://www.google.com/search?q=./mobile/README.md).

---

## Licença

Este ecossistema completo está distribuído sob a licença **GNU General Public License v2**. Consulte o arquivo `LICENSE` na raiz para mais detalhes e permissões de uso.
