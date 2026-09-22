# FinPro — Controle Financeiro Inteligente para Autônomos

*Plano de escopo do projeto*

## 1. Status atual da implementação

*Leitura feita direto no código do repositório em 20/09/2026. Desde a última revisão, backend e frontend ganharam o módulo `Budget` (metas/orçamento por categoria), único item de Fase 2 que ainda faltava.*

**Backend (~80%)**

- ✅ Arquitetura em camadas hexagonal: `domain` (modelo + ports de saída) → `application` (services) → `infrastructure` (controllers, DTOs, adapters de persistência/segurança/cliente externo)
- ✅ Autenticação completa e real: registro/login com JWT (`JwtService`, `JwtAuthenticationFilter`), Spring Security stateless — **não é mais `permitAll()` geral**; só `/api/auth/**`, `/api/cep/**`, Swagger e `/actuator/health` são públicos, todo o resto exige token
- ✅ CRUD completo (Controller → Service → DTO) para **Account**, **Category**, **Transaction**, **Transfer** e **Client**, com regras de negócio de domínio (ex.: saldo insuficiente em transferência, bloqueio de exclusão de conta/categoria/cliente com registros vinculados via `EntityHasLinkedRecordsException`)
- ✅ Importação de extrato CSV (`ImportBatchController`, upload multipart) com motor de categorização automática por `CategoryRule` (match por palavra-chave no descritivo, peso reforçado a cada correção manual do usuário)
- ✅ Integração com ViaCEP para autocompletar endereço no cadastro de usuário
- ✅ Validação de CPF/CNPJ (`CpfValidator`, `CnpjValidator`) e modelagem de endereço (`Address`, `BrazilianState`)
- ✅ Migrations Flyway V1 a V9 (schema inicial, CPF/telefone, troca para "documento" genérico, endereço, tabela de transferências, índices, detalhes/tipo de trabalho do cliente, índices de importação/regras)
- ✅ Testes de integração com Testcontainers cobrindo account, auth, category, client, importbatch, transaction, transfer, tax estimate e budget
- ✅ **`TaxEstimate`**: entidade de domínio, repository, service e endpoint completos (`/api/tax-estimates`), com sugestão de alíquota por regime tributário (`TaxRateEstimator`) — migration V10 adicionou a coluna `regime`. Fluxo completo (com diagramas) em [`fluxo-imposto-fluxo-caixa.md`](./tecnica/fluxo-imposto-fluxo-caixa.md)
- ✅ **`Budget`**: entidade de domínio, repository, service e endpoint completos (`/api/budgets`) — camadas hexagonal completa (domain/application/infrastructure), mesmo padrão dos demais módulos. O "gasto até agora" (`spentValue`) é calculado no backend (`BudgetApplicationService.calculateSpent`, via query agregada nas transações do usuário) e devolvido pronto no `BudgetResponse` — mesma convenção de `Account.calculateCurrentBalance`, não recalculado no frontend
- ✅ Testes unitários (JUnit 5 + Mockito) cobrindo os 12 `ApplicationService` (mocks dos ports, sem Testcontainers) e a lógica pura de domínio (validadores de CPF/CNPJ, `TaxRateEstimator`, `DashboardAggregator`, `CategoryRule`, `Category`, `TaxRegime`, `CsvTransactionParser`) + mappers/validators/adapters de infraestrutura (`ViaCepAdapter` via servidor HTTP local, `CategoryPersistenceMapper`, `UserPersistenceMapper`, validadores de bean validation)
- ✅ JaCoCo configurado (relatório em `target/site/jacoco`) e usado para uma varredura de gaps de cobertura: **252 testes no backend** (unitários + integração via Testcontainers), cobertura final **LINE 99.36% · INSTRUCTION 99.46% · BRANCH 96.71% · METHOD 99.57% · CLASS 100%** — gaps reais fechados incluem endpoints `GET /{id}` e `DELETE` nunca exercitados (Category/Client/Transaction/CategoryRule), validações de categoria/cliente inexistente na revisão de importação, branch de mês inválido no dashboard, handlers de erro `CepServiceUnavailable`/genérico nunca testados, e o `catch (IOException)` do parser de CSV. Os poucos pontos restantes (guardas defensivas `@PrePersist createdAt` em 5 entidades JPA, `main()`, e um `catch` de upload multipart de difícil reprodução em teste) foram avaliados como inalcançáveis ou de baixo valor e deixados de fora conscientemente

**Frontend (~75%)**

- ✅ Setup Vite + React 19 + TypeScript + Tailwind CSS 4
- ✅ Autenticação ponta a ponta: login/registro com validação (React Hook Form + Zod), autocomplete de CEP, validação de CPF/CNPJ, `AuthContext` + `ProtectedRoute` + armazenamento de JWT — inclui tratamento de sessão expirada: o `httpClient` nunca reagia a um 401 (token expirado, padrão de 1h no backend), deixando a sessão "logada" pra sempre no `localStorage` e toda tela presa num spinner que nunca resolvia; agora o `httpClient` limpa a sessão e emite `finpro:session-expired` (via `EventTarget` próprio, não `window`, pra funcionar igual em teste e browser), o `AuthContext` escuta e desloga de verdade, `ProtectedRoute` manda pra `/login` e um toast explica o motivo
- ✅ CRUD funcional consumindo a API real para **Contas**, **Categorias**, **Clientes**, **Transações** e **Transferências** (cada módulo com client de API, hooks React Query, formulários e listas com filtros via `CollapsibleFilters`)
- ✅ Dashboard com dados reais e gráficos (Recharts): cards com variação % vs. mês anterior, receita x despesa por mês, evolução do saldo consolidado, despesa e receita por categoria, **receita por cliente**, saldo por conta — transferências entre contas próprias excluídas dos cálculos de receita/despesa. Todo o cálculo é feito no backend (`DashboardController`/`DashboardAggregator`); cada gráfico busca seus dados com um hook próprio, aparecendo assim que a resposta chega
- ✅ Importação de extrato: upload de CSV, lista de importações com status/contagem de "sem categoria", tela de revisão inline (categoria/cliente por transação) e CRUD de regras de categorização (`ImportsPage`, `CategoryRulesPanel`)
- ✅ Tema claro/escuro (`ThemeContext`/`ThemeToggle`), notificações toast, layout responsivo (`AppLayout`, `Footer`)
- ✅ Biblioteca de componentes de UI reutilizáveis (Button, Card, Modal, Select, Input, FormField, Checkbox, FileInput, etc.)
- ✅ Tela de impostos (`TaxEstimatesPage`): criação de estimativa por mês/regime com receita pré-preenchida a partir das transações e alíquota sugerida, listagem e remoção
- ✅ Projeção de fluxo de caixa no dashboard (`CashFlowProjectionChart`): média móvel dos últimos 3 meses + lançamentos futuros já cadastrados, calculada no backend (`/api/dashboard/cash-flow-projection`) e só exibida no frontend
- ✅ Tela de orçamentos (`BudgetsPage`): criação de meta de gasto por categoria/mês, barra de progresso com o gasto real (`spentValue`, vindo pronto do backend) vs. o limite, alerta visual quando o limite é ultrapassado

**Infra & docs (~85%)**

- ✅ Docker Compose com postgres + backend + frontend
- ✅ CI no GitHub Actions, separado por pasta (`backend-ci.yml` / `frontend-ci.yml`)
- ✅ README com setup local e este plano de escopo
- ⬜ Deploy real (Railway/Render + Vercel)
- ✅ Seed de dados de demonstração (`scripts/seed-demo-data.mjs`); ⬜ badge de CI no README

Em resumo: autenticação, contas, categorias, clientes, transações, transferências, importação de extrato com categorização automática, estimativa de imposto, projeção de fluxo de caixa e orçamento por categoria já funcionam de ponta a ponta (backend com regras de negócio e testes de integração, frontend consumindo a API real) — com isso, todo o escopo de Fase 2 está concluído. O que falta para fechar o MVP é o deploy e os itens de qualidade técnica (testes unitários, lint, seed de demonstração) — ver seções 5, 11 e 12.

**Documentação por fluxo/tela** (arquitetura hexagonal + diagramas Mermaid + regras de negócio + onde cada peça vive no repositório, um `.md` por módulo em `docs/tecnica/`): [`fluxo-autenticacao.md`](./tecnica/fluxo-autenticacao.md) · [`fluxo-contas.md`](./tecnica/fluxo-contas.md) · [`fluxo-categorias.md`](./tecnica/fluxo-categorias.md) · [`fluxo-clientes.md`](./tecnica/fluxo-clientes.md) · [`fluxo-transacoes.md`](./tecnica/fluxo-transacoes.md) · [`fluxo-transferencias.md`](./tecnica/fluxo-transferencias.md) · [`fluxo-importacao-extrato.md`](./tecnica/fluxo-importacao-extrato.md) · [`fluxo-dashboard.md`](./tecnica/fluxo-dashboard.md) · [`fluxo-orcamentos.md`](./tecnica/fluxo-orcamentos.md) · [`fluxo-imposto-fluxo-caixa.md`](./tecnica/fluxo-imposto-fluxo-caixa.md)

**[`../frontend/src/features/faq/docs/FAQ.md`](../frontend/src/features/faq/docs/FAQ.md)** — perguntas frequentes de quem usa o app (não técnico): o que cada campo faz, o que não dá pra editar ainda, como funciona a categorização automática, etc.

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
- Importação de extrato via CSV — ✅ **feito** (upload multipart, formato `date,description,amount`)
- Categorização automática por regras (palavra-chave no descritivo → categoria) — ✅ **feito** (`CategoryRule`, maior peso vence)
- Cadastro de clientes/projetos e vínculo de receitas a eles — ✅ **feito**
- Dashboard: saldo atual, receita x despesa no mês — ✅ **feito**; gráfico por categoria e receita por cliente — ✅ **feito**
- Deploy funcional com dados de exemplo — ⬜ *pendente*

### Fase 2

- Importação de OFX
- Aprendizado de categorização a partir das correções do usuário — ✅ **feito** (correção manual reforça/cria `CategoryRule`, ver seção 8)
- Estimativa de imposto simplificada (educacional, não é orientação fiscal) — ✅ **feito**
- Projeção de fluxo de caixa (média móvel + recebíveis futuros) — ✅ **feito**
- Metas/orçamento por categoria — ✅ **feito**

### Fase 3

- Recibo/cobrança em PDF por cliente
- Multi-moeda
- Modo escuro, responsividade mobile completa — *modo escuro e responsividade já entregues na Fase 1, adiantado em relação ao plano original*
- Exportação de relatórios (PDF/Excel)

## 6. Modelo de dados (entidades principais)

*O schema completo abaixo foi aplicado via Flyway desde o `V1__init_schema.sql` (mais a V10, que adicionou a coluna `regime` a `tax_estimates`). Todas as entidades já têm o tratamento hexagonal completo (domínio, port/adapter, aplicação, API).*

- **User** ✅: id, nome, email, senha (hash), documento (CPF/CNPJ), telefone, endereço, regime_tributario
- **Account** ✅: id, user_id, nome, tipo, saldo_inicial
- **Category** ✅: id, user_id (null = padrão do sistema), nome, tipo, cor, ícone
- **Client** ✅: id, user_id, nome, email, telefone, documento, tipo de trabalho (PJ/autônomo), observações, cor, ativo
- **Transaction** ✅: id, account_id, category_id, client_id, transfer_id, import_batch_id, descrição, valor, data, tipo, origem (manual/importada)
- **Transfer** ✅: id, conta origem, conta destino, valor, data — gera duas `Transaction` vinculadas (débito/crédito) excluídas dos somatórios de receita/despesa do dashboard
- **ImportBatch** ✅: id, user_id, account_id, arquivo_original, formato (CSV/OFX), data_importação, status (processando/concluída/falhou)
- **CategoryRule** ✅: id, user_id, padrão_texto, category_id, peso — motor de categorização automática, ver seção 8
- **TaxEstimate** ✅: id, user_id, mês/ano, regime, receita_bruta, alíquota_aplicada, valor_estimado
- **Budget** ✅: id, user_id, category_id, mês/ano, valor_limite

## 7. Arquitetura técnica

**Backend — Java 21 + Spring Boot 3 + Maven + PostgreSQL**

- Camadas: Controller → Service → Repository, com ports/adapters (hexagonal) e DTOs separados de entidades — ✅ *implementado para Account, Category, Client, Transaction, Transfer, ImportBatch/CategoryRule, TaxEstimate, Budget, Auth e Cep*
- Autenticação: Spring Security + JWT — ✅ *feito e plugado (filtro real, sem `permitAll()` fora de auth/cep/swagger/health)*
- Migrations: Flyway — ✅ *feito (V1 a V10)*
- Documentação da API: springdoc-openapi (Swagger UI) — *dependência configurada*
- Testes: JUnit 5 + Mockito (unidade), Testcontainers + Postgres real (integração) — ✅ *193 testes no total (143 unitários + 50 de integração), `mvn verify` cobrindo tudo*

**Frontend — React + Tailwind CSS + Vite**

- React Query para estado de chamadas à API — ✅ *em uso em todos os módulos*
- React Router — ✅ *em uso (rotas públicas de login/registro + rotas protegidas com layout)*
- Recharts para os gráficos do dashboard — ✅ *em uso (receita x despesa por mês, evolução do saldo, projeção de fluxo de caixa, despesa/receita por categoria, receita por cliente, saldo por conta)*
- React Hook Form + Zod para formulários — ✅ *em uso em auth, contas, categorias, clientes, transações, transferências e regras de categorização*

**Infraestrutura**

- Docker Compose (postgres + backend + frontend) para desenvolvimento local — *feito*
- GitHub Actions (CI separado para backend e frontend, por path) — *feito*
- Deploy: backend em Railway/Render, frontend em Vercel — *pendente*

## 8. Motor de categorização automática

*✅ Implementado (`ImportApplicationService`, `CategoryRuleApplicationService`).*

1. Upload do CSV cria um `ImportBatch` (status `PROCESSING` → `COMPLETED`) e cada linha vira uma `Transaction` com `origin = IMPORTED`
2. Para cada transação, busca as `CategoryRule` do usuário ordenadas por peso decrescente e aplica a primeira cujo padrão está contido no descritivo (case-insensitive) e cujo tipo (receita/despesa) bate com o da transação
3. Se nada bater, a transação fica com `categoryId = null` ("Sem categoria") para revisão manual na tela de importação
4. Correção manual do usuário (`PUT /api/import-batches/{id}/transactions/{id}`) reforça o peso da regra existente para aquele descritivo, ou cria uma nova regra — assim, uma próxima importação com o mesmo descritivo já chega categorizada
5. Regras também podem ser cadastradas manualmente, sem depender de uma importação (`CategoryRulesPanel`)

*Não implementado ainda: regras padrão globais do sistema (ex.: um conjunto pré-cadastrado tipo "UBER" → Transporte disponível para todo usuário) — hoje toda regra é criada pelo próprio usuário, seja manualmente ou por correção.*

## 9. Módulo freelancer

- Receita vinculada a `Client` — ✅ **feito** (`clientId` em `Transaction`, `ClientsPage` com CRUD completo)
- Dashboard de receita por cliente no período — ✅ **feito** (gráfico "Receita por cliente" no dashboard, mês atual)
- Estimativa de imposto configurável por regime (aviso: estimativa educacional, não substitui contador) — ✅ **feito** (`TaxEstimatesPage`, alíquota sugerida por `TaxRateEstimator`)
- Projeção de fluxo de caixa por média móvel — ✅ **feito** (`CashFlowProjectionChart`, média móvel de 3 meses + lançamentos futuros já cadastrados)

## 10. Telas principais

- Visão geral (saldo, receita/despesa, variação % e gráficos) — ✅ *feito*
- Extrato (filtros por conta/categoria/período) — ✅ *feito* (`TransactionsPage` com `CollapsibleFilters`); filtro por cliente — ⬜ *pendente*
- Contas — ✅ *feito*
- Categorias — ✅ *feito*
- Transferências entre contas — ✅ *feito* (não previsto no plano original)
- Importação (upload + preview + revisão de categorização) — ✅ *feito* (`ImportsPage`, `ImportBatchList`, `ImportBatchReviewTable`, `CategoryRulesPanel`)
- Clientes (CRUD + receita por cliente no dashboard) — ✅ *feito*; histórico de receita ao longo do tempo por cliente — ⬜ *pendente*
- Impostos (`TaxEstimatesPage`) e projeção de fluxo de caixa (no dashboard) — ✅ *feito*
- Orçamentos (`BudgetsPage`): meta de gasto por categoria/mês com barra de progresso — ✅ *feito*

## 11. Checklist de qualidade técnica

- Testes automatizados nos fluxos críticos (auth, contas, categorias, clientes, transações, transferências, importação/categorização, tax estimate, budget, dashboard) — ✅ *feito via Testcontainers*; testes unitários (Mockito, backend) e Vitest (frontend: schemas Zod, validadores, formatação, utils do dashboard, `httpClient`, `authStorage`) — ✅ *feito*, 252 testes backend (JaCoCo: 99.4% linhas / 96.7% branches) + 141 testes frontend (cobertura: 51.5% statements / **91.9% branches** — todo arquivo com lógica de verdade está praticamente 100% em branch; o que falta em statements são só wrappers de API de uma linha e hooks do React Query, ver nota abaixo)
- Lint/formatação consistente — ✅ *feito*: ESLint (flat config, `typescript-eslint` + `react-hooks` + `react-refresh`) no frontend e Spotless (Google Java Format, estilo AOSP de 4 espaços, ordem de imports customizada para preservar a convenção `com.lmf.*` → demais → `java.*`) no backend, vinculado à fase `verify` — os dois já rodam no CI (`npm run lint --if-present` e `mvn -B verify`)
- Gate de cobertura mínima — ✅ *feito*: `jacoco:check` (backend) falha o `mvn verify`/CI se a cobertura de linhas cair abaixo de 80%
- Análise de qualidade contínua (SonarCloud) — ✅ *configurado, validado localmente e já rodando no CI real* (etapa 1/2 do item "SonarCloud + Pact"): dois projetos independentes, cada CI validando só a sua parte (sem re-executar os testes do outro lado nem depender de um workflow à parte) — backend (`leozztto_lmffinpro_backend`, plugin `org.sonarsource.scanner.maven:sonar-maven-plugin` declarado em `<build><plugins>` do `pom.xml` — precisa estar declarado, não só nas `<properties>`, senão o prefixo `sonar:sonar` não resolve — reaproveitando o relatório do JaCoCo já gerado pelo `verify`) rodando ao final do `backend-ci.yml`, e frontend (`leozztto_lmffinpro_frontend`, `frontend/sonar-project.properties`, scan via `npx @sonar/scan` — a `SonarSource/sonarcloud-github-action@v3` usada antes foi descontinuada pela SonarSource, sua imagem Docker não tem mais o `sonar-scanner`; trocada depois de quebrar no primeiro run real) rodando ao final do `frontend-ci.yml` reaproveitando a cobertura do `npm run test:coverage`. Os dois passos só rodam se `SONAR_TOKEN` existir — o token é repassado por uma `env:` no nível do job e checado como `if: env.SONAR_TOKEN != ''` (o contexto `secrets` não pode ser referenciado direto num `if:`, só em `run:`/`with:`/`env:` — isso já chegou a quebrar os dois workflows inteiros no primeiro push real, corrigido). Cobertura do frontend no Sonar ajustada em `sonar.coverage.exclusions` pra refletir a mesma decisão consciente já tomada nos testes: componentes React, hooks do React Query, wrappers de API por feature, `types.ts` e listas de dados estáticas ficam fora da métrica — sem isso o Sonar contava esses ~100 arquivos nunca testados e reportava a cobertura agregada do frontend em ~26%, mascarando que tudo que é lógica de verdade está perto de 100%
- Testes de contrato (Pact) entre frontend e backend — ✅ *configurado e validado ponta a ponta localmente* (etapa 2/2 do item "SonarCloud + Pact"), broker PactFlow (`https://lezzotto.pactflow.io`). Um caso representativo prova o fluxo inteiro (`GET /api/categories`): consumer (`frontend/src/features/categories/api/categoriesApi.pact.test.ts`, `@pact-foundation/pact`) roda contra um mock server local e gera `frontend/pacts/*.json`; provider (`backend/src/test/java/.../pact/CategoryProviderPactTest.java`, `pact-jvm` `junit5spring`) sobe o Spring real (Testcontainers) e verifica o contrato de verdade, com um state handler criando usuário+categoria reais e um filtro injetando o JWT real antes da chamada (o header do contrato é só um placeholder — quem autentica de verdade é o filtro). Validei rodando local contra o pact file gerado (`@PactFolder`, sem broker): **`has status code 200 (OK)` / `has a matching body (OK)`**. No caminho, descobri por engenharia reversa do bytecode que `@TargetRequestFilter` (documentado como o jeito "padrão" de injetar auth) não é o mecanismo real usado pelo módulo `junit5spring` — o certo é um parâmetro `HttpRequest` extra no próprio método `@TestTemplate`, resolvido via `ParameterResolver`. Configuração final do provider usa `@PactBroker` + `@EnabledIfEnvironmentVariable(PACT_BROKER_TOKEN)`, então roda de verdade contra o PactFlow só quando o token existe — sem quebrar `mvn verify` pra quem não tem a credencial (mesmo padrão do Sonar). Frontend publica os contratos pro broker no `frontend-ci.yml` via CLI oficial do Pact, também condicionado à secret existir. Falta só criar o secret `PACT_BROKER_TOKEN` no GitHub (mesmo passo que faltou pro Sonar) pra rodar de verdade no CI — os outros ~10 endpoints do app ainda não têm contrato, esse foi só a prova de conceito da infraestrutura
- CI verde (badge no README) — *CI ok, sem badge*
- Dados seed realistas (script com usuário demo + ~6 meses de transações) — ✅ *feito*: `scripts/seed-demo-data.mjs` (Node, sem dependências), cria o usuário demo e popula tudo via chamadas HTTP normais à API (respeita as mesmas validações/regras de negócio de um usuário real, não toca no banco direto) — 3 contas, 10 categorias, 3 clientes, 4 regras de categorização, ~120-130 transações variando mês a mês (receita fixa + variável por cliente, despesas fixas e variáveis por categoria), 12 transferências (reserva mensal + saque pra carteira), 3 orçamentos do mês atual e 2 estimativas de imposto. Idempotente (roda de novo sem duplicar). Achei e corrigi um bug real na primeira rodada: a conta Carteira ficava com saldo negativo (só recebia despesas de Lazer, nunca uma entrada) — adicionado saque mensal da Conta Corrente pra Carteira pra simular retirada de dinheiro em espécie. Validado batendo na API de verdade contra o `docker compose` local: saldos finais positivos e plausíveis nas 3 contas
- Deploy ao vivo com usuário de demonstração — *pendente*
- README completo: problema, diferenciais, prints, stack, setup local, link do deploy — *sem prints ainda*

## 12. Roadmap sugerido (atualizado)

1. ~~Semana 1-2: modelagem, setup Spring Boot + Postgres + Flyway, auth JWT, CRUD básico~~ — ✅ **concluído** (auth JWT real, CRUD de Account/Category/Transaction/Transfer com testes de integração)
2. ~~Dashboard visual~~ — ✅ **concluído**: Recharts plugado na `DashboardPage` com 5 visões (receita x despesa por mês, evolução do saldo, despesa/receita por categoria, saldo por conta) e variação % vs. mês anterior nos cards
3. ~~Módulo de clientes/projetos~~ — ✅ **concluído**: entidade `Client` completa (domain + port + adapter + controller + DTO), vinculada a `Transaction`, CRUD no frontend
4. ~~Importação CSV + motor de regras~~ — ✅ **concluído**: `ImportBatch` + `CategoryRule` (domain + application + infra), upload multipart, tela de preview/revisão com aprendizado automático de regra a partir da correção do usuário
5. ~~Receita por cliente no dashboard~~ — ✅ **concluído**: gráfico "Receita por cliente" (mês atual) ao lado do saldo por conta
6. ~~Estimativa de imposto + projeção de fluxo de caixa~~ — ✅ **concluído**: módulo `TaxEstimate` completo no backend (com `TaxRateEstimator` por regime, migration V10) + tela "Impostos" no frontend; projeção de fluxo de caixa (`CashFlowProjectionChart`) calculada no dashboard a partir de média móvel + lançamentos futuros já cadastrados
7. ~~Metas/orçamento por categoria~~ — ✅ **concluído**: módulo `Budget` completo no backend (domain/application/infrastructure + testes de integração) + tela "Orçamentos" no frontend, com o gasto real (`spentValue`) calculado no backend e devolvido pronto no `BudgetResponse` — fecha o escopo de Fase 2
8. ~~Migrar as agregações do frontend para o backend~~ — ✅ **concluído**: decisão do usuário de que processamento/regra de negócio fica no backend, frontend só exibe e valida formulário. Auditoria módulo a módulo (Contas, Categorias, Clientes, Transações, Transferências, Importações, Auth) encontrou e corrigiu quatro brechas reais (saldo inicial e tipo de categoria editáveis após criação, valor de transação sem validação de sinal, regime tributário sem checagem contra o tipo de documento) — todas com teste de integração cobrindo o caso. O Dashboard, maior concentração de cálculo no cliente, ganhou um `DashboardController` novo (`/api/dashboard/overview`, `/monthly-flow`, `/balance-evolution`, `/cash-flow-projection`, `/category-breakdown`, `/client-breakdown`) com a lógica portada 1:1 do antigo `dashboard/utils.ts` para `DashboardAggregator` (backend); o frontend busca cada gráfico com seu próprio hook do React Query, então cada um aparece assim que sua chamada responde, em vez de esperar tudo pronto
9. ~~Testes unitários + varredura de cobertura (JaCoCo/Vitest)~~ — ✅ **concluído**: backend com JUnit 5 + Mockito + Testcontainers (252 testes, JaCoCo 99.4% linhas / 96.7% branches) e frontend com Vitest (141 testes: schemas Zod, validadores de CPF/CNPJ, formatação, utils do dashboard, `httpClient` — header de auth, FormData vs JSON, parsing de erro — e `authStorage` — sessão em localStorage, fallback em modo privado). Depois de uma segunda passada fechando branches específicos (campo obrigatório vazio por vazio em cada schema, o caso `remainder < 2` do dígito verificador de CPF/CNPJ que nenhum CPF/CNPJ de teste batia), a cobertura de branch nos arquivos com lógica de verdade foi de 79% pra **91.9%**. Ficaram de fora, conscientemente: (1) componentes React (retorno menor para o esforço), (2) hooks do React Query (`useXxx.ts` — exigiriam `renderHook`+`QueryClientProvider`, mesma fronteira dos componentes), (3) os ~30 wrappers de API de uma linha (`categoriesApi.list: () => httpClient.get(...)` etc.) — testá-los só reafirmaria a URL escrita no próprio arquivo — e (4) listas/mapas de label estáticos (`BRAZILIAN_STATES`, `CATEGORY_TYPE_LABELS` etc.) — mesmo motivo
10. Fechamento: deploy (Railway/Render + Vercel), badge de CI, prints e link no README, vídeo curto de demo

## 13. Stack resumida

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3, Maven, Spring Security, Spring Data JPA |
| Banco | PostgreSQL, Flyway |
| Testes | JUnit 5, Mockito, Testcontainers (backend); Vitest (frontend) |
| Frontend | React, Tailwind CSS, Vite, React Query, Recharts |
| Infra | Docker Compose, GitHub Actions, Railway/Render + Vercel |
