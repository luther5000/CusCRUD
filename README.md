# CusCRUD

Aplicativo Android para gestão de inventários voltado a bancos de alimentos, ONGs e iniciativas sociais que precisam organizar tipos de produtos, itens em estoque e acessos de colaboradores por inventário.

## Visão geral

O projeto foi construído em Kotlin para Android, com interface em Jetpack Compose e integração com uma API REST. O fluxo principal do app cobre autenticação de usuários, seleção ou criação de inventários/ONGs, consulta de estoque, cadastro e edição de produtos, além do gerenciamento de colaboradores e permissões.

## Funcionalidades principais

- Login e cadastro de usuários
- Seleção do inventário ativo
- Criação, edição e remoção de inventários
- Visualização do inventário agrupado por tipo de produto
- Cadastro, edição, consulta e remoção de produtos
- Consulta de detalhes de produto
- Gerenciamento de colaboradores do inventário
- Controle de permissões por papel de acesso
- Persistência segura da sessão do usuário

## Stack e tecnologias

- Kotlin 2.1
- Android Gradle Plugin 8.7
- Java 17
- Jetpack Compose
- Navigation Compose
- Hilt para injeção de dependências
- Retrofit + OkHttp + Kotlinx Serialization
- DataStore para sessão e preferências
- JUnit4, MockWebServer, MockK e testes instrumentados Android

## Requisitos

- Android Studio recente com suporte a Kotlin 2.1 e Compose
- JDK 17
- Android SDK:
  - `compileSdk = 35`
  - `targetSdk = 35`
  - `minSdk = 21`
- Emulador Android ou dispositivo físico para executar o app
- Backend da API disponível para autenticação, inventários, tipos, produtos e acessos

## Configuração do ambiente

O app lê a URL base da API a partir do arquivo `local.properties`, usando a chave `API_BASE_URL`.

Se a chave não estiver definida, o build `debug` usa o valor padrão:

```properties
http://localhost/api/v1/
```

Exemplo de configuração no `local.properties`:

```properties
API_BASE_URL=http://10.0.2.2:8080/api/v1/
```

Observações:

- Em emulador Android, `10.0.2.2` normalmente aponta para a máquina host.
- O app está com `usesCleartextTraffic="true"` no manifesto, o que facilita desenvolvimento local com HTTP.
- No build `release`, a URL base é definida diretamente em `BuildConfig` como `https://api.cuscrud.com/v1/`.

## Como executar

1. Clone o repositório.
2. Configure a propriedade `API_BASE_URL` no `local.properties`.
3. Abra o projeto no Android Studio.
4. Sincronize as dependências do Gradle.
5. Execute o módulo `app` em um emulador ou dispositivo.

Se preferir usar o terminal:

```bash
./gradlew assembleDebug
```

No Windows:

```bat
gradlew.bat assembleDebug
```

## Testes

O projeto possui testes unitários e testes instrumentados Android.

Executar testes unitários:

```bash
./gradlew testDebugUnitTest
```

Executar testes instrumentados:

```bash
./gradlew connectedDebugAndroidTest
```

Há também uma pasta de apoio a testes compartilhados:

- `shared-test/`

## Estrutura do projeto

```text
CusCRUD/
├── app/
│   ├── src/main/java/com/cuscrud/
│   │   ├── data/
│   │   ├── di/
│   │   ├── domain/
│   │   ├── presentation/
│   │   └── ui/
│   ├── src/test/
│   ├── src/androidTest/
│   └── src/sharedTest/
├── shared-test/
├── docs/
└── gradle/
```

## Arquitetura

O projeto segue uma separação em camadas que facilita manutenção e testes:

- `presentation`: telas, estados de UI, navegação e ViewModels
- `domain`: modelos, contratos de repositório e interactors
- `data`: integração com API, DTOs, mapeadores, interceptors e persistência de sessão
- `di`: módulos de injeção de dependências com Hilt
- `ui`: tema e componentes reutilizáveis

## Fluxo principal da aplicação

1. O usuário realiza login ou cadastro.
2. O app valida a sessão e recupera o contexto salvo.
3. O usuário seleciona ou cria um inventário/ONG.
4. O inventário é exibido agrupado por tipos de produtos.
5. A partir daí, é possível adicionar, editar, consultar e remover produtos.
6. Usuários com permissão adequada podem administrar inventário e colaboradores.

## Documentação adicional

- Diagrama de classes: `docs/degsin/diagrama_de_classes.md`

## Licença

Este projeto está distribuído sob a licença GNU General Public License v2. Consulte o arquivo `LICENSE` para mais detalhes.
