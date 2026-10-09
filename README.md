# FinPro

Controle financeiro para freelancers e autônomos, com importação de extrato bancário e categorização automática de transações.

Diferente de um "CRUD de receita/despesa" genérico, o FinPro foca em três coisas:

- **Importação inteligente**: sobe um CSV/OFX do banco e o sistema categoriza as transações sozinho, aprendendo com as correções do usuário.
- **Foco em freelancer**: receita organizada por cliente/projeto, estimativa de imposto e projeção de fluxo de caixa irregular.
- **Base técnica sólida**: arquitetura em camadas, testes automatizados (backend e frontend), CI no GitHub Actions.

O plano de escopo completo (modelo de dados, roadmap, telas) está em [`docs/plano.md`](docs/plano.md). Cada módulo tem seu próprio fluxo técnico, com diagramas, em `docs/tecnica/` — ex.: [`fluxo-imposto-fluxo-caixa.md`](docs/tecnica/fluxo-imposto-fluxo-caixa.md).

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3, Maven, Spring Security, Spring Data JPA, Flyway, ShedLock, Micrometer/Prometheus |
| Banco | PostgreSQL |
| Testes | JUnit 5, Mockito, Testcontainers |
| Frontend | React, TypeScript, Tailwind CSS, Vite, React Query, Recharts |
| Infra | Docker Compose, GitHub Actions |

## Estrutura do repositório

```
LmfFinPro/
├── backend/     # API Spring Boot (Maven)
├── frontend/    # SPA React + Tailwind (Vite)
├── docs/        # Documentação do projeto (plano de escopo, fluxos técnicos, observabilidade)
├── ops/         # Operação: regras de alerta do Prometheus
├── docker-compose.yml
└── .github/workflows/
```

## Rodando localmente

### Tudo junto via Docker (mais simples)

```bash
docker compose up --build
```

Isso sobe quatro containers: Postgres, backend (Spring Boot), frontend (build estático servido por Nginx) e Mailpit (servidor de e-mail falso para desenvolvimento).

- Frontend: http://localhost
- API: http://localhost:8080 (Swagger UI em `/swagger-ui.html`, só com `SWAGGER_ENABLED=true` no `.env` — desligado por padrão)
- Postgres: localhost:5432
- Mailpit: http://localhost:8025 — caixa de entrada com todos os e-mails que o backend envia (ex.: link de "esqueci minha senha"); nenhum e-mail sai de verdade

O compose exige `JWT_SECRET` e `FINPRO_ATTACHMENTS_ENCRYPTION_KEY` no `.env` (gere com `openssl rand -base64 48` e `openssl rand -base64 32`). A segunda cifra os comprovantes em disco e **precisa de uma cópia fora do servidor**.

### Operação e segurança

- Backup e restauração (scripts em [`ops/backup/`](ops/backup)), criptografia dos anexos: [`docs/operacao/backup-restauracao.md`](docs/operacao/backup-restauracao.md)
- Resposta a incidentes: [`docs/operacao/plano-resposta-incidentes.md`](docs/operacao/plano-resposta-incidentes.md)
- Teste de invasão do isolamento entre grupos: [`docs/operacao/teste-de-invasao-grupos.md`](docs/operacao/teste-de-invasao-grupos.md)

### Observabilidade

Métricas (Prometheus) e health ficam numa porta de gestão separada, `MANAGEMENT_PORT` (padrão **8081**), que o `docker-compose.yml` não publica — só a rede interna alcança. Logs em JSON com `requestId` (header `X-Request-Id`) saem com `SPRING_PROFILES_ACTIVE=json`. As regras de alerta dos schedulers e da API estão em [`ops/prometheus/finpro-alerts.yml`](ops/prometheus/finpro-alerts.yml). Detalhes em [`docs/tecnica/observabilidade.md`](docs/tecnica/observabilidade.md).

### E-mail (redefinição de senha)

O backend envia e-mail via SMTP quando `SPRING_MAIL_HOST` está definido — no Docker Compose ele aponta para o Mailpit. Rodando o backend fora do Docker sem SMTP configurado, o link de redefinição de senha só é escrito no log do backend. Em produção, configure um SMTP real (`SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`) e `FRONTEND_URL` com o endereço público do frontend (usado para montar o link do e-mail).

### App instalável e notificações push (PWA)

O frontend é instalável (menu do navegador > "Instalar" / "Adicionar à Tela de Início") e o resumo diário de alertas também pode chegar como notificação push, ativada por aparelho em **Configurações > Notificações**. O push só liga se o backend tiver as chaves VAPID — sem elas a opção some da tela:

```bash
npx web-push generate-vapid-keys   # copie para VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY (e VAPID_SUBJECT) no .env
```

Exige HTTPS (ou localhost); no iPhone/iPad, o app precisa estar instalado na Tela de Início (iOS 16.4+). Detalhes em [`docs/tecnica/fluxo-alertas.md`](docs/tecnica/fluxo-alertas.md#7-notificação-push-pwa).

### Rodando cada parte na mão (útil durante o desenvolvimento, com hot reload)

**1. Banco de dados**

```bash
docker compose up -d postgres
# opcional, para receber os e-mails de redefinição de senha: docker compose up -d mailpit
# e SPRING_MAIL_HOST=localhost / SPRING_MAIL_PORT=1025 no .env do backend
```

**2. Backend**

```bash
cd backend
cp ../.env.example .env   # ajuste as variáveis se necessário
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`. Documentação interativa (Swagger UI) em `http://localhost:8080/swagger-ui.html` — desligada por padrão; ligue com `SWAGGER_ENABLED=true` (já vem assim no `.env.example`). Em produção, mantenha desligada.

**3. Frontend**

```bash
cd frontend
npm install
npm run dev
```

A SPA sobe em `http://localhost:5173` com hot reload (mais rápido para desenvolver do que reconstruir o container a cada mudança).

### Dados de demonstração

Com o backend no ar (Docker ou `mvn spring-boot:run`), popule um usuário demo com ~6 meses de histórico realista (contas, categorias, clientes, transações, transferências, orçamentos e estimativas de imposto):

```bash
node scripts/seed-demo-data.mjs
```

Cria (ou reaproveita, se já existir) o usuário `demo@finpro.app` / `Demo@12345`. Rodar de novo não duplica nada. Para popular uma API que não seja a local, use `API_BASE_URL=https://sua-api.exemplo.com/api node scripts/seed-demo-data.mjs`.

## Status

✅ Escopo de MVP + Fase 2 + Fase 3 concluído (autenticação, importação CSV/OFX, orçamentos e recorrências, alertas por e-mail e push/PWA, metas de economia, pró-labore, anexos, tags, calendário, patrimônio, multi-moeda, relatórios em PDF/CSV, entre outros). Falta só o deploy real. Veja o detalhamento em [`docs/plano.md`](docs/plano.md#1-status-atual-da-implementação) e o roadmap em [`docs/plano.md`](docs/plano.md#12-roadmap-sugerido-atualizado).
