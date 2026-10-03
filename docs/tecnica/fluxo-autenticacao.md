# FinPro — Fluxo de Autenticação (Cadastro, Login e Redefinição de Senha)

*Documentação técnica do módulo de autenticação. Diagramas em [Mermaid](https://mermaid.js.org/) — renderizam nativamente no GitHub.*

## 1. Visão geral

Autenticação é a porta de entrada de todo o sistema: sem ela, nenhuma outra tela é acessível. O FinPro usa **JWT** sem sessão guardada no servidor — o backend valida a assinatura do token a cada requisição e confere só um número por usuário no banco, a **versão de sessão** (`users.session_version`), que permite encerrar todas as sessões de uma vez (ver seção 6). Os casos de uso são `POST /api/auth/register` e `POST /api/auth/login` (ambos devolvem um access token JWT de 15 min e os dados básicos do usuário, e definem o cookie httpOnly do refresh token), `POST /api/auth/refresh` e `POST /api/auth/logout` (seção 5) e o par de redefinição de senha `POST /api/auth/forgot-password` / `POST /api/auth/reset-password`.

Dois detalhes moldam o cadastro: (1) o regime tributário informado precisa ser coerente com o tipo de documento (pessoa jurídica exige CNPJ, pessoa física exige CPF) — é uma regra de negócio validada no backend, não só no formulário; e (2) o endereço é preenchido automaticamente a partir do CEP via ViaCEP, para reduzir o atrito do formulário mais longo do sistema.

## 2. Tela

### `/login` — `LoginPage`

Formulário simples: e-mail, senha, botão "Entrar". Validação client-side mínima (`loginSchema`: e-mail bem formado, senha não vazia) — a validação de verdade (credenciais corretas) é sempre do backend. Estados:

- **Enviando**: botão vira "Entrando...", desabilitado.
- **Erro**: mensagem em vermelho abaixo do formulário com a mensagem que o backend devolveu (`"E-mail ou senha inválidos"` para credenciais erradas — a mesma mensagem tanto para e-mail inexistente quanto para senha errada, de propósito, para não revelar quais e-mails estão cadastrados).
- Link **"Esqueceu sua senha?"** (abaixo do campo de senha) para `/esqueci-senha`, e link para `/registro` para quem ainda não tem conta.
- **Senha redefinida**: ao voltar da tela de redefinição, recebe `state.passwordReset` pelo roteador e mostra o aviso "Senha redefinida com sucesso".

As três telas públicas de acesso (login, esqueci a senha, redefinir senha) compartilham o layout `AuthPageShell`: logo + título dimensionados em unidades de container (`cqw`), proporcionais à largura do box, e o box do formulário abaixo.

### `/esqueci-senha` — `ForgotPasswordPage`

Um campo de e-mail (`forgotPasswordSchema`). Ao enviar, mostra sempre a mesma confirmação ("Se houver uma conta cadastrada com ..., você vai receber um e-mail") — a API também responde igual exista ou não a conta, então a tela não pode afirmar nada diferente.

### `/redefinir-senha?token=...` — `ResetPasswordPage`

Aberta pelo link do e-mail. Nova senha + confirmação (`resetPasswordSchema`: mínimo 8 caracteres, confirmação igual). Sem `token` na URL, mostra aviso de link inválido em vez do formulário. Token recusado pelo backend (expirado, já usado ou substituído por um mais novo) aparece como erro abaixo do formulário, com link para solicitar outro. Sucesso navega para `/login` com o aviso de senha redefinida.

### `/registro` — `RegisterPage`

Formulário mais longo, dividido em quatro seções:

1. **Dados pessoais** — nome completo (exige nome + sobrenome) e e-mail.
2. **Documento e regime** — select de regime tributário (`TAX_REGIME_OPTIONS`: Autônomo, MEI, Simples Nacional, Lucro Presumido, Outro); o campo de documento muda de label/máscara (CPF ↔ CNPJ) automaticamente conforme o regime escolhido (`documentTypeForTaxRegime`); telefone opcional.
3. **Endereço** — CEP com autocomplete: ao sair do campo (`onBlur`) com 8 dígitos, dispara `useCepLookup` (`GET /api/cep/{cep}`) e preenche logradouro/bairro/cidade/estado automaticamente. Enquanto a busca está pendente, esses campos ficam desabilitados (`disabled={cepLookup.isPending}`) e mostra "Buscando endereço...". Se o CEP não existir (404) ou o serviço estiver fora, mostra aviso e libera o preenchimento manual — a busca nunca bloqueia o cadastro.
4. **Segurança** — senha e confirmação (mínimo 8 caracteres), checkbox de aceite dos termos.

Estados: botão "Criando conta..." enquanto envia; erro do backend (e-mail/documento já cadastrado, documento incompatível com o regime) exibido do mesmo jeito que no login. Sucesso navega direto para `/` (dashboard) — não existe tela intermediária de confirmação, o registro já autentica.

### Expiração de sessão (comportamento global, não uma tela própria)

Qualquer tela protegida pode, a qualquer momento, receber um `401` do backend (access token expirado — 15 min por padrão; o `httpClient` tenta renovar sozinho pelo refresh token antes de desistir —, sessão encerrada por uma redefinição de senha, refresh token expirado/revogado, ou nunca ter tido sessão válida). Quando a renovação também falha: a sessão salva é limpa, um toast "Sua sessão expirou. Faça login novamente." aparece, e o roteador manda o usuário de volta pra `/login` — sem exigir recarregar a página manualmente. Ver seção 5.

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
        Controller["AuthController\n/register · /login\n/forgot-password · /reset-password"]
        ReqDTO["RegisterRequest · LoginRequest\nForgotPasswordRequest · ResetPasswordRequest"]
        RespDTO["AuthResponse"]
    end

    subgraph application["application/auth"]
        Service["AuthApplicationService\nregister · login"]
        ResetSvc["PasswordResetApplicationService\nrequestReset · resetPassword"]
    end

    subgraph domain["domain"]
        Model["User\n(register() · withPasswordHash())"]
        ResetToken["PasswordResetToken\n(issue · isUsable · markUsed)"]
        Regime["TaxRegime.expectedDocumentType()"]
        UserPort["UserRepositoryPort"]
        TokenPort["TokenPort"]
        PasswordPort["PasswordHasherPort"]
        ResetTokenPort["PasswordResetTokenRepositoryPort"]
        MailerPort["PasswordResetMailerPort"]
    end

    subgraph infra["infrastructure/persistence"]
        Adapter["UserRepositoryAdapter"]
        ResetAdapter["PasswordResetTokenRepositoryAdapter"]
    end

    subgraph mail["infrastructure/mail"]
        Smtp["SmtpPasswordResetMailer\n(spring.mail.host configurado)"]
        LogMailer["LoggingPasswordResetMailer\n(sem SMTP: link só no log)"]
    end

    DB[("users · password_reset_tokens\n(Postgres)")]
    SmtpServer(["Servidor SMTP\n(Mailpit em dev)"])

    Client -- "Authorization: Bearer <token>\n(rotas protegidas)" --> Filter
    Filter -- "parse(token)" --> JwtSvc
    Filter -- "findSessionVersion(userId)\nversão do token = atual?" --> UserPort
    Filter -- "popula SecurityContext\n(ou segue anônimo)" --> Controller

    Controller -- "@Valid" --> ReqDTO
    Controller --> Service
    Controller --> ResetSvc
    Service --> Model
    Model --> Regime
    Service --> PasswordPort
    PasswordPort -.->|implementa| BCrypt
    Service --> UserPort
    UserPort -.->|implementa| Adapter
    Adapter --> DB
    Service --> TokenPort
    TokenPort -.->|implementa| JwtSvc
    Service --> RespDTO
    RespDTO --> Client

    ResetSvc --> ResetToken
    ResetSvc --> UserPort
    ResetSvc --> PasswordPort
    ResetSvc --> ResetTokenPort
    ResetTokenPort -.->|implementa| ResetAdapter
    ResetAdapter --> DB
    ResetSvc --> MailerPort
    MailerPort -.->|implementa| Smtp
    MailerPort -.->|implementa| LogMailer
    Smtp --> SmtpServer
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
    Svc->>Jwt: generate(userId, email, sessionVersion)
    Jwt-->>Svc: access token JWT (expira em 15 min)
    Svc->>Svc: RefreshTokenApplicationService.startSession()\n(token aleatório; só o SHA-256 vai ao banco)
    Svc-->>Ctrl: AuthResult
    Ctrl-->>Http: 201 Created (AuthResponse)\n+ Set-Cookie finpro_refresh (httpOnly)
    Http-->>AuthCtx: token, userId, name, email
    AuthCtx->>AuthCtx: saveSession(): token só em memória,\nnome/e-mail no localStorage + estado React
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

**Renovação silenciosa (refresh token).** O `401` acima só chega ao usuário se a renovação falhar. Com um access token na requisição, o `httpClient` recebe o `401`, chama `POST /api/auth/refresh` (o navegador anexa o cookie `finpro_refresh`, `httpOnly`, `Path=/api/auth`, `SameSite=Strict`, `Secure`) e repete a requisição uma vez com o token novo. Várias requisições em paralelo compartilham uma única renovação (`refreshSession()`), porque o refresh token é **rotacionado a cada uso**. Ao recarregar a página o access token (só em memória) não existe mais: o `AuthContext` vê o usuário lembrado no localStorage, mostra "Carregando…" no `ProtectedRoute` e retoma a sessão pelo mesmo endpoint.

No backend (`RefreshTokenApplicationService`, tabela `refresh_tokens`): o refresh token é aleatório e opaco, só o SHA-256 é guardado; cada uso revoga o token e emite outro na mesma **família**. Reapresentar um token já rotacionado (sinal de roubo) revoga a família inteira — exceto dentro de `REFRESH_TOKEN_REUSE_LEEWAY_SECONDS` (10 s), janela que tolera duas abas renovando juntas e só devolve um access token, sem novo cookie. O token guarda a versão de sessão da emissão: trocar a senha (seção 7) o invalida. `POST /api/auth/logout` revoga a família e apaga o cookie; um job diário remove os expirados. `REFRESH_COOKIE_SECURE=false` só é necessário em HTTP fora de `localhost`.

**Por que um `EventTarget` próprio, e não `window.dispatchEvent`:** `httpClient` é um módulo puro (sem React), então não pode chamar `setSession()` diretamente — mas também não pode depender de `window`, que não existe no ambiente de teste (Vitest roda em Node puro). Um `EventTarget` dedicado (`authEvents`, exportado por `authStorage.ts`) funciona idêntico nos dois ambientes e mantém o event bus isolado, sem poluir o namespace global de eventos do browser.

## 6. Fluxo de redefinição de senha ("esqueci minha senha")

```mermaid
sequenceDiagram
    actor U as Usuário
    participant Forgot as ForgotPasswordPage
    participant Reset as ResetPasswordPage
    participant Ctrl as AuthController
    participant Svc as PasswordResetApplicationService
    participant Repo as UserRepositoryPort
    participant Tokens as PasswordResetTokenRepositoryPort
    participant Mailer as PasswordResetMailerPort
    participant DB as Postgres

    U->>Forgot: informa o e-mail
    Forgot->>Ctrl: POST /api/auth/forgot-password
    Ctrl->>Svc: requestReset(email)
    Svc->>Repo: findByEmail(email)
    alt conta existe
        Svc->>Tokens: invalidateActiveTokens(userId)
        Svc->>Svc: token aleatório (32 bytes, Base64 URL)\nguarda só o SHA-256
        Svc->>Tokens: save(PasswordResetToken, expira em 30 min)
        Tokens->>DB: INSERT password_reset_tokens
        Svc->>Mailer: sendResetLink(email, nome, FRONTEND_URL/redefinir-senha?token=...)
        Note over Svc,Mailer: falha no envio só vai pro log —\nnão pode mudar a resposta
    else conta não existe
        Note over Svc: não faz nada
    end
    Ctrl-->>Forgot: 202 Accepted (sempre)
    Forgot->>U: "Se houver uma conta... você vai receber um e-mail"

    U->>Reset: abre o link do e-mail e informa a nova senha
    Reset->>Ctrl: POST /api/auth/reset-password {token, password}
    Ctrl->>Svc: resetPassword(token, senha)
    Svc->>Tokens: findByTokenHash(SHA-256(token))
    alt token existe, não usado e dentro da validade
        Svc->>Repo: save(user.withPasswordHash(hash))\nsessionVersion + 1
        Svc->>Tokens: save(token.markUsed())
        Ctrl-->>Reset: 204 No Content
        Reset->>U: navega para /login com "Senha redefinida"
    else inválido, expirado, usado ou substituído
        Ctrl-->>Reset: 400 "Link de redefinição inválido ou expirado"
    end
```

**Envio do e-mail:** `PasswordResetConfig` escolhe o adapter — com `spring.mail.host` configurado usa `SmtpPasswordResetMailer` (e-mail em texto simples); sem SMTP (backend rodando pela IDE, testes) usa `LoggingPasswordResetMailer`, que só escreve o link no log. No `docker-compose`, o backend aponta para o **Mailpit** (`SPRING_MAIL_HOST=mailpit`), que captura todos os e-mails em http://localhost:8025 sem entregar a ninguém.

## 7. Encerramento de sessões ao trocar a senha

JWT não pode ser "apagado" depois de emitido, então cada token carrega a **versão de sessão** do usuário no momento do login (claim `sv`). `User#withPasswordHash` incrementa essa versão, e o `JwtAuthenticationFilter`, depois de validar assinatura e expiração, compara o `sv` do token com `users.session_version` (`UserRepositoryPort.findSessionVersion`, consulta só dessa coluna). Diferente — ou usuário inexistente — o filtro trata como token inválido: a requisição segue anônima e a rota protegida responde `401`, caindo no fluxo da seção 5 no frontend.

Resultado: redefinir a senha derruba **todas** as sessões abertas (outros navegadores e aparelhos) na próxima requisição de cada uma. Tokens emitidos antes da existência do claim são lidos como versão `0`, então a introdução do mecanismo não deslogou ninguém.

## 8. Regras de negócio importantes

- **Senha nunca trafega nem é salva em texto puro**: hash BCrypt (`BCryptPasswordHasherAdapter`) já na entrada, o domínio (`User`) só conhece o hash.
- **E-mail e documento são únicos**: `EmailAlreadyInUseException` / `DocumentAlreadyInUseException` (ambas `409 Conflict`) antes de qualquer tentativa de salvar.
- **Documento precisa bater com o regime tributário** (`@ValidTaxRegimeDocument`, validação de classe): MEI, Simples Nacional e Lucro Presumido exigem CNPJ; Autônomo e Outro exigem CPF (`TaxRegime.expectedDocumentType()`). Validado tanto no frontend (`registerSchema`, para feedback imediato) quanto — de forma independente e definitiva — no backend.
- **CPF/CNPJ são validados por dígito verificador** (`@ValidDocumentNumber`, mesmo algoritmo espelhado em `CpfValidator`/`CnpjValidator` no backend e `shared/validation/{cpf,cnpj}.ts` no frontend), não só por formato.
- **Login não revela qual campo errou**: e-mail inexistente e senha incorreta devolvem a mesma mensagem (`InvalidCredentialsException`, `401`) — evita enumerar e-mails cadastrados por tentativa e erro.
- **"Esqueci minha senha" também não revela quem tem conta**: `forgot-password` responde `202` exista ou não o e-mail, e uma falha no envio do e-mail não altera a resposta.
- **Token de redefinição**: aleatório (`SecureRandom`, 32 bytes), guardado só como hash SHA-256 (quem lê o banco não consegue usá-lo), válido por 30 min (`PASSWORD_RESET_TOKEN_TTL_MINUTES`), uso único, e pedir um novo invalida os anteriores. Nova senha segue a mesma regra do cadastro (mínimo 8 caracteres).
- **Trocar a senha encerra todas as sessões** (versão de sessão, ver seção 7).
- **Sessão = access token curto + refresh token em cookie httpOnly** (ver seção 5): access token de 15 min (`JWT_EXPIRATION_MS`) só em memória no frontend; refresh token de 30 dias (`REFRESH_TOKEN_TTL_DAYS`) que o JavaScript não consegue ler, então um XSS não leva a sessão embora. O localStorage guarda apenas nome/e-mail.
- **CORS explícito**: a SPA roda em origem diferente da API (`CORS_ALLOWED_ORIGINS`), então toda a configuração de CORS mora em `SecurityConfig` — sem ela, toda chamada do frontend falharia silenciosamente no navegador.

- **Rate limit nas rotas públicas** (janela fixa em memória, `RateLimiter`): por IP no `AuthRateLimitFilter` (`login` 10 por 15 min, `register` 5 por 1 h, `forgot-password` 5 por 15 min) e por e-mail no `AuthEmailRateLimiter`, chamado pelo `AuthController` (`login` 10 por 15 min, `forgot-password` 3 por 1 h — contém o spam de e-mail). Estourou o limite, a API responde `429 Too Many Requests` com o header `Retry-After` (segundos) e a mensagem no formato padrão de erro (`TooManyRequestsException` no `GlobalExceptionHandler`). Limites e liga/desliga em `finpro.rate-limit.*` (`RATE_LIMIT_ENABLED`). Atrás do nginx, `RATE_LIMIT_TRUST_PROXY_HEADER=true` faz o filtro usar o `X-Real-IP` (o `docker-compose.yml` já liga); em produção a porta 8080 não deve ficar exposta, senão o header seria forjável.
- **Swagger UI e `/v3/api-docs` desligados por padrão** (`SWAGGER_ENABLED=false`): com a flag desligada as rotas exigem token (`401`); o `SecurityConfig` só as libera quando `springdoc.swagger-ui.enabled` é `true`. O `.env.example` liga para desenvolvimento.

**Limitação conhecida:** o rate limit é em memória — com mais de uma réplica da API cada uma conta separado (limite efetivo N vezes maior); para escalar horizontalmente seria preciso um armazenamento compartilhado (ex.: Redis).

## 9. Onde cada peça vive no repositório

| Camada | Arquivo(s) |
|---|---|
| Domínio | `domain/model/{User,RefreshToken,PasswordResetToken,Address,TaxRegime,DocumentType,BrazilianState}.java`, `domain/model/{CpfValidator,CnpjValidator}.java`, `domain/exception/InvalidPasswordResetTokenException.java` |
| Ports | `domain/port/out/{UserRepositoryPort,TokenPort,TokenClaims,PasswordHasherPort,PasswordResetTokenRepositoryPort,RefreshTokenRepositoryPort,PasswordResetMailerPort}.java` |
| Aplicação | `application/auth/{AuthApplicationService,RefreshTokenApplicationService,RefreshTokenSettings,PasswordResetApplicationService,PasswordResetSettings,RegisterCommand,LoginCommand,AddressCommand,AuthResult}.java` |
| Segurança | `infrastructure/security/{JwtService,JwtAuthenticationFilter,BCryptPasswordHasherAdapter,AuthenticatedUser,RateLimiter,AuthRateLimitFilter,AuthEmailRateLimiter}.java`, `infrastructure/config/{SecurityConfig,JwtProperties,RefreshTokenProperties,RateLimitProperties,CorsProperties,PasswordResetConfig,PasswordResetProperties}.java` |
| E-mail | `infrastructure/mail/{SmtpPasswordResetMailer,LoggingPasswordResetMailer}.java` |
| API | `infrastructure/web/controller/{AuthController,CepController}.java`, `infrastructure/web/dto/auth/*.java`, `infrastructure/web/validation/{ValidDocumentNumber,ValidTaxRegimeDocument,...}.java` |
| Persistência | `infrastructure/persistence/adapter/{UserRepositoryAdapter,PasswordResetTokenRepositoryAdapter,RefreshTokenRepositoryAdapter}.java` (+ entity/mapper/repository correspondentes) |
| Migration | `db/migration/V1__init_schema.sql` (+ `V2`–`V4`: CPF/telefone, documento genérico, endereço; `V11`: `password_reset_tokens`; `V12`: `users.session_version`; `V32`: `refresh_tokens`) |
| Frontend — telas | `features/auth/components/{LoginPage,RegisterPage,ForgotPasswordPage,ResetPasswordPage,AuthPageShell}.tsx` |
| Frontend — lógica | `features/auth/{schemas.ts,hooks/{useLogin,useRegister,useCepLookup,usePasswordReset}.ts,api/{cepApi,passwordResetApi}.ts}`, `shared/auth/{AuthContext,ProtectedRoute,authStorage,types}.tsx/.ts`, `shared/api/httpClient.ts` |
| Testes | `backend/src/test/java/.../integration/auth/{AuthIntegrationTest,PasswordResetIntegrationTest,RefreshTokenIntegrationTest,RateLimitIntegrationTest}.java`, `.../infrastructure/security/RateLimiterTest.java`, `.../application/auth/{AuthApplicationServiceTest,RefreshTokenApplicationServiceTest,PasswordResetApplicationServiceTest}.java`, `../../frontend/src/features/auth/schemas.test.ts`, `../../frontend/src/shared/api/httpClient.test.ts` |

## 10. Configurações: dados cadastrais e troca de senha logado

Acessível pelo **menu do usuário** (ícone no canto superior direito do `AppLayout`, componente `shared/layout/UserMenu`: nome/e-mail, **Configurações** e **Sair**). `/configuracoes` (`features/profile/components/SettingsLayout`) tem um submenu — coluna à esquerda no desktop, abas no celular — com três rotas: `/configuracoes/dados-cadastrais` (`ProfileDataPage`), `/configuracoes/senha` (`PasswordPage`) e `/configuracoes/notificacoes` (`NotificationsPage`, ver [`fluxo-alertas.md`](fluxo-alertas.md)). `/perfil` redireciona para os dados cadastrais. Backend em `ProfileController` (`/api/profile`, sempre o usuário do token) → `ProfileApplicationService`.

- **`GET /api/profile`** devolve os dados cadastrais; **`PUT /api/profile`** salva. O formulário reaproveita as seções do cadastro (`AccountDataFields`, com busca de CEP) e as mesmas validações (`validateAccountFields` no frontend; `@ValidDocumentNumber`/`@ValidTaxRegimeDocument` no `UpdateProfileRequest`). E-mail e documento continuam únicos (`409` se já usados por outra conta).
- **Foto de perfil (opcional)**, migration V31 (`users.photo_key`, `photo_content_type`): `PUT /api/profile/photo` (multipart, campo `file`) substitui a anterior, `DELETE /api/profile/photo` remove e `GET /api/profile/photo` devolve a imagem do próprio usuário. Aceita JPG, PNG ou WEBP de até 2 MB, com o tipo **reconhecido pelos bytes** (mesmo `AttachmentFileType` dos anexos; inválido → 400). O arquivo vai para o `FileStoragePort` (mesmo armazenamento dos anexos, chave UUID), é servido com `nosniff` e `Cache-Control: no-store, private`, e o arquivo antigo é apagado ao trocar/remover. Frontend: `ProfilePhotoEditor` em Dados cadastrais e `UserAvatar` no menu do usuário.
- **Trocar o e-mail exige a senha atual** (`currentPassword`), porque o e-mail é o login — sem isso, quem pegasse uma sessão aberta poderia tomar a conta. O campo só aparece na tela quando o e-mail é alterado.
- **`PUT /api/profile/password`** (senha atual + nova) troca a senha via `User#withPasswordHash` — encerra as outras sessões (seção 7) — e devolve um access token novo e um refresh token novo (cookie), mantendo a sessão de quem trocou.
- **Senha atual errada responde `400`, não `401`** (`IncorrectCurrentPasswordException`): um `401` faria o `httpClient` tratar como sessão expirada e deslogar o usuário (seção 5).
- Depois de salvar o perfil, `updateSession` também atualiza nome/e-mail da sessão, então o nome no topo muda na hora.
