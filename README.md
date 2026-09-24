# FinPro

Controle financeiro para freelancers e autônomos, com importação de extrato bancário e categorização automática de transações.

Diferente de um "CRUD de receita/despesa" genérico, o FinPro foca em três coisas:

- **Importação inteligente**: sobe um CSV/OFX do banco e o sistema categoriza as transações sozinho, aprendendo com as correções do usuário.
- **Foco em freelancer**: receita organizada por cliente/projeto, estimativa de imposto e projeção de fluxo de caixa irregular.
- **Base técnica sólida**: arquitetura em camadas, testes automatizados, CI e deploy ao vivo.

O plano de escopo completo (modelo de dados, roadmap, telas) está em [`docs/plano.md`](docs/plano.md). O fluxo de estimativa de imposto e projeção de fluxo de caixa, com diagramas, está em [`docs/fluxo-imposto-fluxo-caixa.md`](docs/fluxo-imposto-fluxo-caixa.md).

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3, Maven, Spring Security, Spring Data JPA, Flyway |
| Banco | PostgreSQL |
| Testes | JUnit 5, Mockito, Testcontainers |
| Frontend | React, TypeScript, Tailwind CSS, Vite, React Query, Recharts |
| Infra | Docker Compose, GitHub Actions |

## Estrutura do repositório

```
LmfFinPro/
├── backend/     # API Spring Boot (Maven)
├── frontend/    # SPA React + Tailwind (Vite)
├── docs/        # Documentação do projeto (plano de escopo, etc.)
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
- API: http://localhost:8080 (Swagger UI em `/swagger-ui.html`)
- Postgres: localhost:5432
- Mailpit: http://localhost:8025 — caixa de entrada com todos os e-mails que o backend envia (ex.: link de "esqueci minha senha"); nenhum e-mail sai de verdade

### E-mail (redefinição de senha)

O backend envia e-mail via SMTP quando `SPRING_MAIL_HOST` está definido — no Docker Compose ele aponta para o Mailpit. Rodando o backend fora do Docker sem SMTP configurado, o link de redefinição de senha só é escrito no log do backend. Em produção, configure um SMTP real (`SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`) e `FRONTEND_URL` com o endereço público do frontend (usado para montar o link do e-mail).

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

A API sobe em `http://localhost:8080`. Documentação interativa (Swagger UI) em `http://localhost:8080/swagger-ui.html`.

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

🚧 Em desenvolvimento — MVP em andamento. Veja o roadmap em [`docs/plano.md`](docs/plano.md#11-roadmap-sugerido).
