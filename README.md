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

Isso sobe três containers: Postgres, backend (Spring Boot) e frontend (build estático servido por Nginx).

- Frontend: http://localhost:5173
- API: http://localhost:8080 (Swagger UI em `/swagger-ui.html`)
- Postgres: localhost:5432

### Rodando cada parte na mão (útil durante o desenvolvimento, com hot reload)

**1. Banco de dados**

```bash
docker compose up -d postgres
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

## Status

🚧 Em desenvolvimento — MVP em andamento. Veja o roadmap em [`docs/plano.md`](docs/plano.md#11-roadmap-sugerido).
