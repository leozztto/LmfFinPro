# FinPro — Fluxo de Autenticação (Cadastro e Login)

*Documentação técnica do módulo de autenticação. Diagramas em [Mermaid](https://mermaid.js.org/) — renderizam nativamente no GitHub.*

## 1. Visão geral

Autenticação é a porta de entrada de todo o sistema: sem ela, nenhuma outra tela é acessível. O FinPro usa **JWT stateless** — o backend não guarda sessão em memória nem em banco, só valida a assinatura do token a cada requisição. O fluxo cobre dois casos de uso (`POST /api/auth/register` e `POST /api/auth/login`), ambos devolvendo o mesmo formato de resposta: um token e os dados básicos do usuário.

Dois detalhes moldam o cadastro: (1) o regime tributário informado precisa ser coerente com o tipo de documento (pessoa jurídica exige CNPJ, pessoa física exige CPF) — é uma regra de negócio validada no backend, não só no formulário; e (2) o endereço é preenchido automaticamente a partir do CEP via ViaCEP, para reduzir o atrito do formulário mais longo do sistema.

## 2. Tela

### `/login` — `LoginPage`

Formulário simples: e-mail, senha, botão "Entrar". Validação client-side mínima (`loginSchema`: e-mail bem formado, senha não vazia) — a validação de verdade (credenciais corretas) é sempre do backend. Estados:

- **Enviando**: botão vira "Entrando...", desabilitado.
- **Erro**: mensagem em vermelho abaixo do formulário com a mensagem que o backend devolveu (`"E-mail ou senha inválidos"` para credenciais erradas — a mesma mensagem tanto para e-mail inexistente quanto para senha errada, de propósito, para não revelar quais e-mails estão cadastrados).
- Link para `/registro` para quem ainda não tem conta.

### `/registro` — `RegisterPage`

Formulário mais longo, dividido em quatro seções:

1. **Dados pessoais** — nome completo (exige nome + sobrenome) e e-mail.
2. **Documento e regime** — select de regime tributário (`TAX_REGIME_OPTIONS`: Autônomo, MEI, Simples Nacional, Lucro Presumido, Outro); o campo de documento muda de label/máscara (CPF ↔ CNPJ) automaticamente conforme o regime escolhido (`documentTypeForTaxRegime`); telefone opcional.
3. **Endereço** — CEP com autocomplete: ao sair do campo (`onBlur`) com 8 dígitos, dispara `useCepLookup` (`GET /api/cep/{cep}`) e preenche logradouro/bairro/cidade/estado automaticamente. Enquanto a busca está pendente, esses campos ficam desabilitados (`disabled={cepLookup.isPending}`) e mostra "Buscando endereço...". Se o CEP não existir (404) ou o serviço estiver fora, mostra aviso e libera o preenchimento manual — a busca nunca bloqueia o cadastro.
4. **Segurança** — senha e confirmação (mínimo 8 caracteres), checkbox de aceite dos termos.

Estados: botão "Criando conta..." enquanto envia; erro do backend (e-mail/documento já cadastrado, documento incompatível com o regime) exibido do mesmo jeito que no login. Sucesso navega direto para `/` (dashboard) — não existe tela intermediária de confirmação, o registro já autentica.

### Expiração de sessão (comportamento global, não uma tela própria)

Qualquer tela protegida pode, a qualquer momento, receber um `401` do backend (token expirado — expira em 1h por padrão) ou nunca ter tido sessão válida. Quando isso acontece: a sessão salva é limpa, um toast "Sua sessão expirou. Faça login novamente." aparece, e o roteador manda o usuário de volta pra `/login` — sem exigir recarregar a página manualmente. Ver seção 5.

## 3. Arquitetura

Backend em camadas hexagonal (`web` → `application` → `domain` → `infrastructure/persistence`), mais a peça específica de autenticação: o filtro JWT, que roda **antes** do dispatch para qualquer controller.

```mermaid
flowchart TD
    Client(["Cliente HTTP\n(frontend)"]) --> Filter

    subgraph security["infrastructure/security"]
        Filter["JwtAuthenticationFilter\n(OncePerRequestFilter)"]
        JwtSvc["JwtService\nimplements TokenPort"]
        BCrypt["BCryptPasswordHasherAdapter\nimplements PasswordHasherPort"]
    end

    subgraph web["infrastructure/web"]
        Controller["AuthController\n/api/auth/register · /login"]
        ReqDTO["RegisterRequest · LoginRequest\n(@ValidDocumentNumber, @ValidTaxRegimeDocument)"]
        RespDTO["AuthResponse"]
    end

    subgraph application["application/auth"]
        Service["AuthApplicationService\nregister · login"]
    end

    subgraph domain["domain"]
        Model["User\n(record + register())"]
        Regime["TaxRegime.expectedDocumentType()"]
        UserPort["UserRepositoryPort"]
        TokenPort["TokenPort"]
        PasswordPort["PasswordHasherPort"]
    end

    subgraph infra["infrastructure/persistence"]
        Adapter["UserRepositoryAdapter"]
        JpaRepo["UserJpaRepository"]
    end

    DB[("users\n(Postgres)")]

    Client -- "Authorization: Bearer <token>\n(rotas protegidas)" --> Filter
    Filter -- "parse(token)" --> JwtSvc
    Filter -- "popula SecurityContext\n(ou segue anônimo)" --> Controller

    Controller -- "@Valid" --> ReqDTO
    Controller --> Service
    Service --> Model
    Model --> Regime
    Service --> PasswordPort
    PasswordPort -.->|implementa| BCrypt
    Service --> UserPort
    UserPort -.->|implementa| Adapter
    Adapter --> JpaRepo
    JpaRepo --> DB
    Service --> TokenPort
    TokenPort -.->|implementa| JwtSvc
    Service --> RespDTO
    RespDTO --> Client
```

**Por que o filtro nunca lança erro para token inválido:** `JwtAuthenticationFilter` captura `InvalidTokenException` e só limpa o `SecurityContext`, deixando a requisição seguir sem autenticação — quem decide se isso é um problema é o `SecurityFilterChain` padrão do Spring Security (retorna 401 via `RestAuthenticationEntryPoint` se a rota exigir autenticação) ou o próprio controller (se a rota for pública). Assim o mesmo filtro serve tanto rotas públicas (`/api/auth/**`, `/api/cep/**`, Swagger, `/actuator/health`) quanto protegidas, sem duplicar lógica.

## 4. Fluxo de cadastro

```mermaid
sequenceDiagram
    actor U as Usuário
    participant Form as RegisterPage
    participant CepApi as cepApi
    participant AuthCtx as AuthContext
    participant Http as httpClient
    participant Ctrl as AuthController
    participant Svc as AuthApplicationService
    participant Hash as PasswordHasherPort
    participant Repo as UserRepositoryPort
    participant Jwt as TokenPort
    participant DB as Postgres

    U->>Form: preenche CEP e sai do campo
    Form->>CepApi: GET /api/cep/{cep}
    CepApi-->>Form: logradouro, bairro, cidade, UF
    Form->>Form: preenche endereço automaticamente

    U->>Form: completa o restante e confirma
    Form->>Form: registerSchema.superRefine()\n(nome, e-mail, CPF/CNPJ × regime, senha)
    Form->>AuthCtx: register(payload)
    AuthCtx->>Http: POST /auth/register
    Http->>Ctrl: RegisterRequest\n(@ValidDocumentNumber, @ValidTaxRegimeDocument)
    Ctrl->>Svc: register(RegisterCommand)
    Svc->>Repo: existsByEmail? existsByDocumentNumber?
    Repo-->>Svc: não existe
    Svc->>Hash: hash(senha)
    Hash-->>Svc: hash bcrypt
    Svc->>Svc: User.register(...)
    Svc->>Repo: save(user)
    Repo->>DB: INSERT
    DB-->>Repo: usuário salvo (com id)
    Svc->>Jwt: generate(userId, email)
    Jwt-->>Svc: token JWT (expira em 1h)
    Svc-->>Ctrl: AuthResult
    Ctrl-->>Http: 201 Created (AuthResponse)
    Http-->>AuthCtx: token, userId, name, email
    AuthCtx->>AuthCtx: saveSession() no localStorage\n+ atualiza estado React
    AuthCtx-->>Form: sucesso
    Form->>U: navega para "/" (dashboard)
```

Login segue a mesma cauda a partir de `Svc->>Repo`: busca por e-mail, `PasswordHasherPort.matches()` confere a senha contra o hash salvo, e se bater gera o token do mesmo jeito — sem `User.register()`, sem checagem de duplicidade.

## 5. Fluxo de expiração de sessão

Este é um comportamento que atravessa **todas** as telas protegidas, não só uma tela específica — por isso documentado como um fluxo próprio.

```mermaid
sequenceDiagram
    actor U as Usuário
    participant Page as Qualquer tela protegida
    participant Http as httpClient
    participant Ctrl as Controller (qualquer)
    participant Storage as authStorage\n(EventTarget próprio)
    participant AuthCtx as AuthContext
    participant Route as ProtectedRoute
    participant Toast as ToastContext

    Page->>Http: GET/POST/PUT/DELETE ...\ncom o token salvo
    Http->>Ctrl: Authorization: Bearer <token expirado>
    Ctrl-->>Http: 401 Unauthorized
    Http->>Storage: clearSession()\n(apaga o localStorage)
    Http->>Storage: dispatchEvent(SESSION_EXPIRED_EVENT)
    Storage-->>AuthCtx: listener registrado no mount
    AuthCtx->>AuthCtx: setSession(null)
    AuthCtx->>Toast: showToast("Sua sessão expirou.\nFaça login novamente.")
    Http-->>Page: throw ApiError(401, ...)
    Note over Route: session agora é null
    Route->>U: <Navigate to="/login" />
```

**Por que um `EventTarget` próprio, e não `window.dispatchEvent`:** `httpClient` é um módulo puro (sem React), então não pode chamar `setSession()` diretamente — mas também não pode depender de `window`, que não existe no ambiente de teste (Vitest roda em Node puro). Um `EventTarget` dedicado (`authEvents`, exportado por `authStorage.ts`) funciona idêntico nos dois ambientes e mantém o event bus isolado, sem poluir o namespace global de eventos do browser.

## 6. Regras de negócio importantes

- **Senha nunca trafega nem é salva em texto puro**: hash BCrypt (`BCryptPasswordHasherAdapter`) já na entrada, o domínio (`User`) só conhece o hash.
- **E-mail e documento são únicos**: `EmailAlreadyInUseException` / `DocumentAlreadyInUseException` (ambas `409 Conflict`) antes de qualquer tentativa de salvar.
- **Documento precisa bater com o regime tributário** (`@ValidTaxRegimeDocument`, validação de classe): MEI, Simples Nacional e Lucro Presumido exigem CNPJ; Autônomo e Outro exigem CPF (`TaxRegime.expectedDocumentType()`). Validado tanto no frontend (`registerSchema`, para feedback imediato) quanto — de forma independente e definitiva — no backend.
- **CPF/CNPJ são validados por dígito verificador** (`@ValidDocumentNumber`, mesmo algoritmo espelhado em `CpfValidator`/`CnpjValidator` no backend e `shared/validation/{cpf,cnpj}.ts` no frontend), não só por formato.
- **Login não revela qual campo errou**: e-mail inexistente e senha incorreta devolvem a mesma mensagem (`InvalidCredentialsException`, `401`) — evita enumerar e-mails cadastrados por tentativa e erro.
- **Token expira em 1h por padrão** (`JWT_EXPIRATION_MS`, configurável por variável de ambiente) — sem refresh token: expirado, o usuário loga de novo (ver seção 5).
- **CORS explícito**: a SPA roda em origem diferente da API (`CORS_ALLOWED_ORIGINS`), então toda a configuração de CORS mora em `SecurityConfig` — sem ela, toda chamada do frontend falharia silenciosamente no navegador.

## 7. Onde cada peça vive no repositório

| Camada | Arquivo(s) |
|---|---|
| Domínio | `domain/model/{User,Address,TaxRegime,DocumentType,BrazilianState}.java`, `domain/model/{CpfValidator,CnpjValidator}.java` |
| Ports | `domain/port/out/{UserRepositoryPort,TokenPort,PasswordHasherPort}.java` |
| Aplicação | `application/auth/{AuthApplicationService,RegisterCommand,LoginCommand,AddressCommand,AuthResult}.java` |
| Segurança | `infrastructure/security/{JwtService,JwtAuthenticationFilter,BCryptPasswordHasherAdapter,AuthenticatedUser}.java`, `infrastructure/config/{SecurityConfig,JwtProperties,CorsProperties}.java` |
| API | `infrastructure/web/controller/{AuthController,CepController}.java`, `infrastructure/web/dto/auth/*.java`, `infrastructure/web/validation/{ValidDocumentNumber,ValidTaxRegimeDocument,...}.java` |
| Persistência | `infrastructure/persistence/adapter/UserRepositoryAdapter.java` |
| Migration | `db/migration/V1__init_schema.sql` (+ `V2`–`V4`: CPF/telefone, documento genérico, endereço) |
| Frontend — telas | `features/auth/components/{LoginPage,RegisterPage}.tsx` |
| Frontend — lógica | `features/auth/{schemas.ts,hooks/{useLogin,useRegister,useCepLookup}.ts,api/cepApi.ts}`, `shared/auth/{AuthContext,ProtectedRoute,authStorage,types}.tsx/.ts`, `shared/api/httpClient.ts` |
| Testes | `backend/src/test/java/.../integration/auth/AuthIntegrationTest.java`, `.../application/auth/AuthApplicationServiceTest.java`, `../../frontend/src/features/auth/schemas.test.ts`, `../../frontend/src/shared/api/httpClient.test.ts` |
