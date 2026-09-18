# FinPro — Controle Financeiro Inteligente para Autônomos

*Plano de escopo do projeto*

## 1. Status atual da implementação

*Leitura feita direto no código do repositório em 18/09/2026. Desde a última revisão (16/09), o backend ganhou arquitetura hexagonal completa com autenticação JWT real e quatro módulos de CRUD, e o frontend passou de uma tela mockada para um app funcional de ponta a ponta (login, contas, categorias, transações, transferências e dashboard com dados reais).*

**Backend (~65%)**

- ✅ Arquitetura em camadas hexagonal: `domain` (modelo + ports de saída) → `application` (services) → `infrastructure` (controllers, DTOs, adapters de persistência/segurança/cliente externo)
- ✅ Autenticação completa e real: registro/login com JWT (`JwtService`, `JwtAuthenticationFilter`), Spring Security stateless — **não é mais `permitAll()` geral**; só `/api/auth/**`, `/api/cep/**`, Swagger e `/actuator/health` são públicos, todo o resto exige token
- ✅ CRUD completo (Controller → Service → DTO) para **Account**, **Category**, **Transaction** e **Transfer**, com regras de negócio de domínio (ex.: saldo insuficiente em transferência, bloqueio de exclusão de conta/categoria com registros vinculados via `AccountHasLinkedRecordsException`)
- ✅ Integração com ViaCEP para autocompletar endereço no cadastro de usuário
- ✅ Validação de CPF/CNPJ (`CpfValidator`, `CnpjValidator`) e modelagem de endereço (`Address`, `BrazilianState`)
- ✅ Migrations Flyway V1 a V6 (schema inicial, CPF/telefone, troca para "documento" genérico, endereço, tabela de transferências, índices)
- ✅ Testes de integração com Testcontainers cobrindo account, auth, category, transaction e transfer
- ⬜ Testes unitários (Mockito) — hoje só existem testes de integração
- ⬜ Importação de CSV e motor de categorização automática
- ⬜ **Client/projeto, `CategoryRule`, `ImportBatch`, `TaxEstimate`, `Budget`** — as tabelas existem no schema desde o V1, mas não têm entidade de domínio, repository nem endpoint ainda (funcionalidades de Fase 2/3 do módulo freelancer)

**Frontend (~60%)**

- ✅ Setup Vite + React 19 + TypeScript + Tailwind CSS 4
- ✅ Autenticação ponta a ponta: login/registro com validação (React Hook Form + Zod), autocomplete de CEP, validação de CPF/CNPJ, `AuthContext` + `ProtectedRoute` + armazenamento de JWT
- ✅ CRUD funcional consumindo a API real para **Contas**, **Categorias**, **Transações** e **Transferências** (cada módulo com client de API, hooks React Query, formulários e listas com filtros via `CollapsibleFilters`)
- ✅ Dashboard com dados reais (saldo consolidado, receita/despesa do mês, excluindo transferências entre contas próprias do cálculo) — ainda sem gráficos
- ✅ Tema claro/escuro (`ThemeContext`/`ThemeToggle`), notificações toast, layout responsivo (`AppLayout`, `Footer`)
- ✅ Biblioteca de componentes de UI reutilizáveis (Button, Card, Modal, Select, Input, FormField, Checkbox, etc.)
- ⬜ Gráficos do dashboard (Recharts instalado, ainda não consumido)
- ⬜ Receita por cliente/projeto — sem tela e sem dado de origem (módulo de Client não existe)
- ⬜ Importação CSV (upload + preview + revisão de categorização)
- ⬜ Telas de impostos / fluxo de caixa

**Infra & docs (~85%)**

- ✅ Docker Compose com postgres + backend + frontend
- ✅ CI no GitHub Actions, separado por pasta (`backend-ci.yml` / `frontend-ci.yml`)
- ✅ README com setup local e este plano de escopo
- ⬜ Deploy real (Railway/Render + Vercel)
- ⬜ Seed de dados de demonstração e badge de CI no README

Em resumo: autenticação, contas, categorias, transações e transferências já funcionam de ponta a ponta (backend com regras de negócio e testes de integração, frontend consumindo a API real). O que falta para fechar o MVP é: gráficos no dashboard, o módulo de clientes/projetos e a importação com categorização automática — ver seções 5 e 12.

## 2. Pitch (resumo de 30 segundos)

Uma aplicação de controle financeiro que, diferente dos "trackers" genéricos, resolve três dores reais de quem trabalha como autônomo/freelancer: (1) importar e categorizar automaticamente o extrato bancário sem digitação manual, (2) organizar receita por cliente/projeto e estimar impostos e fluxo de caixa irregular, e (3) ser tecnicamente sólida o suficiente (arquitetura limpa, testes, deploy real) para servir como prova de competência full-stack em entrevistas.

## 3. Público-alvo e problema

Freelancers e autônomos (devs, designers, consultores) não têm contracheque fixo nem RH cuidando de imposto. Eles pagam vários clientes, têm meses bons e ruins, e normalmente controlam tudo em planilha solta. O app resolve: "quanto eu realmente ganhei líquido este mês, de quem, e quanto devo guardar pra imposto?"

## 4. Os três diferenciais combinados

1. **Importação + categorização automática**: usuário sobe um CSV/OFX do banco; o sistema categoriza as transações sozinho (regras + aprendizado com correções do usuário).
2. **Foco em freelancer/autônomo**: receita organizada por cliente/projeto, estimativa simplificada de imposto e projeção de fluxo de caixa irregular.
3. **Qualidade técnica de portfólio**: arquitetura em camadas, testes automatizados, CI, deploy ao vivo, dados seed realistas.

## 5. Escopo

### MVP (Fase 1)

- Cadastro/login de usuário (Spring Security + JWT) — ✅ **feito**, ponta a ponta (backend + frontend)
- Cadastro manual de contas bancárias/carteiras — ✅ **feito**
- CRUD de transações (receita/despesa) — ✅ **feito**
- Cadastro de categorias (padrão do sistema + customizadas) — ✅ **feito**
- Transferência entre contas próprias — ✅ **feito** (não estava no plano original; adicionado com validação de saldo)
- Importação de extrato via CSV — ⬜ *pendente*
- Categorização automática por regras (palavra-chave no descritivo → categoria) — ⬜ *pendente*
- Cadastro de clientes/projetos e vínculo de receitas a eles — ⬜ *pendente (tabela `clients` existe no schema, sem entidade/endpoint)*
- Dashboard: saldo atual, receita x despesa no mês — ✅ **feito**; gráfico por categoria e receita por cliente — ⬜ *pendente*
- Deploy funcional com dados de exemplo — ⬜ *pendente*

### Fase 2

- Importação de OFX
- Aprendizado de categorização a partir das correções do usuário
- Estimativa de imposto simplificada (educacional, não é orientação fiscal)
- Projeção de fluxo de caixa (média móvel + recebíveis futuros)
- Metas/orçamento por categoria

### Fase 3

- Recibo/cobrança em PDF por cliente
- Multi-moeda
- Modo escuro, responsividade mobile completa — *modo escuro e responsividade já entregues na Fase 1, adiantado em relação ao plano original*
- Exportação de relatórios (PDF/Excel)

## 6. Modelo de dados (entidades principais)

*O schema completo abaixo foi aplicado via Flyway desde o `V1__init_schema.sql`, mas hoje só **User, Account, Category, Transaction** têm entidade de domínio (`domain/model`), repository (`domain/port/out` + adapter) e endpoints REST. **Transfer** foi adicionado depois (V5) com o mesmo tratamento completo. `CategoryRule`, `Client`, `ImportBatch`, `TaxEstimate` e `Budget` existem só como tabela — sem código de aplicação ainda.*

- **User** ✅: id, nome, email, senha (hash), documento (CPF/CNPJ), telefone, endereço, regime_tributario
- **Account** ✅: id, user_id, nome, tipo, saldo_inicial
- **Category** ✅: id, user_id (null = padrão do sistema), nome, tipo, cor, ícone
- **Transaction** ✅: id, account_id, category_id, client_id, transfer_id, descrição, valor, data, tipo, origem
- **Transfer** ✅: id, conta origem, conta destino, valor, data — gera duas `Transaction` vinculadas (débito/crédito) excluídas dos somatórios de receita/despesa do dashboard
- **CategoryRule** ⬜: id, user_id, padrão_texto, category_id, peso/confiança — *só schema*
- **Client** ⬜: id, user_id, nome, ativo — *só schema*
- **ImportBatch** ⬜: id, user_id, account_id, arquivo_original, formato, data_importação, status — *só schema*
- **TaxEstimate** ⬜: id, user_id, mês/ano, receita_bruta, alíquota_aplicada, valor_estimado — *só schema*
- **Budget** ⬜: id, user_id, category_id, mês/ano, valor_limite — *só schema*

## 7. Arquitetura técnica

**Backend — Java 21 + Spring Boot 3 + Maven + PostgreSQL**

- Camadas: Controller → Service → Repository, com ports/adapters (hexagonal) e DTOs separados de entidades — ✅ *implementado para Account, Category, Transaction, Transfer, Auth e Cep*
- Autenticação: Spring Security + JWT — ✅ *feito e plugado (filtro real, sem `permitAll()` fora de auth/cep/swagger/health)*
- Migrations: Flyway — ✅ *feito (V1 a V6)*
- Documentação da API: springdoc-openapi (Swagger UI) — *dependência configurada*
- Testes: JUnit 5 + Mockito (unidade), Testcontainers + Postgres real (integração) — ✅ *integração cobrindo os 5 módulos principais*; ⬜ *nenhum teste unitário com Mockito ainda*

**Frontend — React + Tailwind CSS + Vite**

- React Query para estado de chamadas à API — ✅ *em uso em todos os módulos*
- React Router — ✅ *em uso (rotas públicas de login/registro + rotas protegidas com layout)*
- Recharts para os gráficos do dashboard — *instalado, ainda não consumido*
- React Hook Form + Zod para formulários — ✅ *em uso em auth, contas, categorias, transações e transferências*

**Infraestrutura**

- Docker Compose (postgres + backend + frontend) para desenvolvimento local — *feito*
- GitHub Actions (CI separado para backend e frontend, por path) — *feito*
- Deploy: backend em Railway/Render, frontend em Vercel — *pendente*

## 8. Motor de categorização automática

*Pendente — só a modelagem (`ImportBatch`, `CategoryRule`) existe no schema; a lógica abaixo ainda não foi implementada.*

1. Importação cria um `ImportBatch` e lê cada linha como transação
2. Busca `CategoryRule` do usuário com match no descritivo
3. Se não encontrar, tenta regras padrão do sistema (ex: "UBER" → Transporte)
4. Se nada bater, marca como "Sem categoria" para revisão manual
5. Correção manual do usuário reforça/cria uma regra para descritivos parecidos

## 9. Módulo freelancer

*Pendente por completo — depende da entidade `Client`, que ainda não existe além do schema.*

- Receita vinculada a `Client`
- Dashboard de receita por cliente no período
- Estimativa de imposto configurável por regime (aviso: estimativa educacional, não substitui contador)
- Projeção de fluxo de caixa por média móvel

## 10. Telas principais

- Visão geral (saldo, receita/despesa) — ✅ *feito*; variação/gráficos — ⬜ *pendente*
- Extrato (filtros por conta/categoria/período) — ✅ *feito* (`TransactionsPage` com `CollapsibleFilters`); filtro por cliente — ⬜ *pendente (sem módulo Client)*
- Contas — ✅ *feito*
- Categorias — ✅ *feito*
- Transferências entre contas — ✅ *feito* (não previsto no plano original)
- Importação (upload + preview + revisão de categorização) — ⬜ *pendente*
- Clientes (receita acumulada e ao longo do tempo) — ⬜ *pendente*
- Impostos/Fluxo de caixa — ⬜ *pendente*

## 11. Checklist de qualidade técnica

- Testes automatizados nos fluxos críticos (auth, contas, categorias, transações, transferências) — ✅ *feito via Testcontainers*; testes unitários (Mockito) — ⬜ *pendente*
- Lint/formatação consistente — *pendente de verificação formal*
- CI verde (badge no README) — *CI ok, sem badge*
- Dados seed realistas (script com usuário demo + ~6 meses de transações) — *pendente*
- Deploy ao vivo com usuário de demonstração — *pendente*
- README completo: problema, diferenciais, prints, stack, setup local, link do deploy — *sem prints ainda*

## 12. Roadmap sugerido (atualizado)

1. ~~Semana 1-2: modelagem, setup Spring Boot + Postgres + Flyway, auth JWT, CRUD básico~~ — ✅ **concluído** (auth JWT real, CRUD de Account/Category/Transaction/Transfer com testes de integração)
2. **Próximo passo A — Dashboard visual**: plugar Recharts (gráfico de receita x despesa por categoria e por mês) na `DashboardPage`, que hoje só mostra os três cards numéricos
3. **Próximo passo B — Módulo de clientes/projetos**: criar entidade `Client` (domain + port + adapter + controller + DTO), vincular a `Transaction`, e tela de CRUD no frontend seguindo o padrão já usado em Account/Category
4. **Próximo passo C — Importação CSV + motor de regras**: `ImportBatch` + `CategoryRule` (domain + application + infra), endpoint de upload, tela de preview/revisão de categorização
5. Semana seguinte: receita por cliente no dashboard (depende do passo B)
6. Depois: estimativa de imposto + projeção de fluxo de caixa (Fase 2, depende de dados de receita por cliente/categoria já consolidados)
7. Antes do deploy: testes unitários (Mockito) nos services de aplicação, lint/formatação, seed de dados de demonstração
8. Fechamento: deploy (Railway/Render + Vercel), badge de CI, prints e link no README, vídeo curto de demo

## 13. Stack resumida

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3, Maven, Spring Security, Spring Data JPA |
| Banco | PostgreSQL, Flyway |
| Testes | JUnit 5, Mockito, Testcontainers |
| Frontend | React, Tailwind CSS, Vite, React Query, Recharts |
| Infra | Docker Compose, GitHub Actions, Railway/Render + Vercel |
