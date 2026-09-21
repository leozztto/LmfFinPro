# FinPro — Fluxo de Estimativa de Imposto e Projeção de Fluxo de Caixa

*Documentação técnica da Fase 2 do módulo freelancer (ver roadmap em [`../plano.md`](../plano.md#12-roadmap-sugerido-atualizado)). Diagramas em [Mermaid](https://mermaid.js.org/) — renderizam nativamente no GitHub.*

> **Atualização:** a seção 6 deste documento originalmente descrevia a projeção de fluxo de caixa como calculada inteiramente no frontend. Isso mudou numa migração posterior (item 8 do roadmap, "Migrar as agregações do frontend para o backend") — hoje ela é calculada no backend, no mesmo `DashboardAggregator` que o resto do Dashboard. A seção 6 já reflete o estado atual; ver [`fluxo-dashboard.md`](fluxo-dashboard.md) para o Dashboard completo.

## 1. Visão geral

A Fase 2 entrega duas funcionalidades que hoje seguem a mesma convenção arquitetural do resto do sistema — toda regra de negócio e agregação vive no backend, o frontend só exibe:

- **Estimativa de imposto**: precisa ficar salva (o usuário quer comparar meses passados) — módulo de backend completo, arquitetura hexagonal igual aos demais (Account, Client, Transaction, etc.), com uma tela própria (`/impostos`).
- **Projeção de fluxo de caixa**: é uma leitura derivada dos dados que já existem (saldo + transações), sem estado próprio para persistir, mas o CÁLCULO em si (média móvel, combinação com lançamentos futuros já cadastrados) acontece no backend, em `DashboardAggregator.cashFlowProjection`, exposto pelo endpoint `/api/dashboard/cash-flow-projection` — o frontend só busca e exibe.

```mermaid
flowchart LR
    subgraph Backend["Backend (Spring Boot)"]
        TE[("tax_estimates\n(Postgres)")]
        TX[("transactions\n(Postgres)")]
        API["/api/tax-estimates"]
        DashAPI["/api/dashboard/cash-flow-projection"]
        Aggregator["DashboardAggregator\n.cashFlowProjection()"]
        API --> TE
        TX --> Aggregator
        Aggregator --> DashAPI
    end

    subgraph Frontend["Frontend (React)"]
        Impostos["Página Impostos\ncria/lista/remove estimativas"]
        Dashboard["Dashboard\nCashFlowProjectionChart"]
        TxCache["Cache de Transações\n(React Query, p/ pré-preencher receita)"]
    end

    Impostos <-- "REST (fetch)" --> API
    Dashboard <-- "REST (fetch)" --> DashAPI
    TxCache --> Impostos
```

## 2. Tela

**Estimativa de imposto** — rota `/impostos` (`TaxEstimatesPage`): cabeçalho com título "Impostos" e botão "+" que abre um `Modal` com `TaxEstimateForm` (mês, regime tributário, receita bruta pré-preenchida a partir das transações do mês selecionado, alíquota sugerida automaticamente e editável). Abaixo, `TaxEstimateList` — lista de estimativas já criadas, cada linha com mês, regime, receita, alíquota, valor estimado e um botão de remover (com confirmação). Estados: "Carregando estimativas..." enquanto busca, "Nenhuma estimativa cadastrada ainda. Adicione a primeira acima." quando vazia.

**Projeção de fluxo de caixa** — não é uma tela própria, é o `CashFlowProjectionChart` dentro do Dashboard (`/`): um gráfico de linha combinando o histórico real (linha sólida) com a projeção dos próximos meses (linha tracejada), carregado pelo hook `useCashFlowProjection()` de forma independente dos outros cards do Dashboard — aparece assim que sua própria chamada responde, sem esperar o resto da página.

## 3. Estimativa de imposto — arquitetura (hexagonal)

Mesma cadeia de camadas usada em todos os módulos do backend: `web` (HTTP) → `application` (regra de uso) → `domain` (modelo + regra de negócio pura) → `infrastructure/persistence` (banco).

```mermaid
flowchart TD
    Client(["Cliente HTTP\n(frontend)"]) --> Controller

    subgraph web["infrastructure/web"]
        Controller["TaxEstimateController\n/api/tax-estimates"]
        ReqDTO["TaxEstimateRequest"]
        RespDTO["TaxEstimateResponse"]
        WebMapper["TaxEstimateWebMapper"]
    end

    subgraph application["application/taxestimate"]
        Service["TaxEstimateApplicationService\ncreate · list · delete · suggestRate"]
    end

    subgraph domain["domain"]
        Model["TaxEstimate\n(record + create())"]
        Regime["TaxRegime\n(enum)"]
        Estimator["TaxRateEstimator\nsuggestRate(regime, receita)"]
        Port["TaxEstimateRepositoryPort\n(interface)"]
    end

    subgraph infra["infrastructure/persistence"]
        Adapter["TaxEstimateRepositoryAdapter"]
        PMapper["TaxEstimatePersistenceMapper"]
        JpaRepo["TaxEstimateJpaRepository\n(Spring Data)"]
        Entity["TaxEstimateJpaEntity"]
    end

    DB[("tax_estimates\n(Postgres)")]

    Controller -- "@Valid" --> ReqDTO
    Controller --> Service
    Service --> Model
    Service --> Estimator
    Model --> Regime
    Service --> Port
    Port -.->|implementa| Adapter
    Adapter --> PMapper
    Adapter --> JpaRepo
    PMapper --> Entity
    JpaRepo --> DB
    Service --> WebMapper
    WebMapper --> RespDTO
    RespDTO --> Client
```

**Por que essa separação importa aqui:** `TaxRateEstimator` é uma classe de domínio pura (sem `@Service`, sem dependência de banco) — só recebe regime + receita e devolve uma alíquota. Isso permite que o mesmo cálculo seja usado tanto no endpoint de sugestão (`GET /suggested-rate`, que não persiste nada) quanto, futuramente, em qualquer outro lugar do sistema que precise da mesma regra, sem duplicar a tabela de alíquotas.

## 4. Fluxo de criação de uma estimativa

Sequência completa desde a tela até o banco, incluindo o pré-preenchimento automático de receita e a sugestão de alíquota.

```mermaid
sequenceDiagram
    actor U as Usuário
    participant Form as TaxEstimateForm
    participant TxCache as Cache de Transações\n(React Query)
    participant API as taxEstimatesApi
    participant Ctrl as TaxEstimateController
    participant Svc as TaxEstimateApplicationService
    participant Est as TaxRateEstimator
    participant Repo as TaxEstimateRepositoryPort
    participant DB as Postgres

    U->>Form: seleciona mês + regime
    Form->>TxCache: soma receitas (INCOME, sem transferência) do mês
    TxCache-->>Form: receita bruta sugerida
    Form->>API: GET /suggested-rate?regime&grossRevenue (debounced)
    API->>Ctrl: suggestedRate(regime, grossRevenue)
    Ctrl->>Svc: suggestRate(regime, grossRevenue)
    Svc->>Est: suggestRate(regime, grossRevenue)
    Est-->>Svc: alíquota sugerida
    Svc-->>Ctrl: alíquota
    Ctrl-->>API: { rate }
    API-->>Form: preenche campo "alíquota"

    U->>Form: ajusta valores (opcional) e confirma
    Form->>API: POST /tax-estimates { mês, regime, receita, alíquota }
    API->>Ctrl: create(request)
    Ctrl->>Svc: create(userId, mês, regime, receita, alíquota)
    Svc->>Svc: TaxEstimate.create()\nestimatedValue = receita × alíquota
    Svc->>Repo: save(taxEstimate)
    Repo->>DB: INSERT
    DB-->>Repo: registro salvo (com id)
    Repo-->>Svc: TaxEstimate
    Svc-->>Ctrl: TaxEstimate
    Ctrl-->>API: 201 Created (TaxEstimateResponse)
    API-->>Form: sucesso
    Form->>TxCache: invalida cache de estimativas
    Form-->>U: toast "Estimativa criada com sucesso"
```

**Detalhe importante de segurança:** o backend **nunca confia** no `estimatedValue` — mesmo que o frontend calcule e mostre o valor ao vivo para o usuário, `TaxEstimate.create()` sempre recalcula `receita × alíquota` no servidor antes de salvar.

## 5. Lógica de sugestão de alíquota (`TaxRateEstimator`)

Alíquotas simplificadas e educacionais — não substituem orientação contábil (aviso fixo também no formulário).

```mermaid
flowchart TD
    Start(["suggestRate(regime, receita)"]) --> Regime{Regime?}
    Regime -- MEI --> R1["6% fixo"]
    Regime -- SIMPLES_NACIONAL --> R2["6%\n(Anexo III, 1ª faixa)"]
    Regime -- LUCRO_PRESUMIDO --> R3["11,33%\n(efetiva aproximada p/ serviços)"]
    Regime -- OUTRO --> R4["0%\n(usuário preenche manualmente)"]
    Regime -- AUTONOMO --> Faixa{Receita mensal?}

    Faixa -- "≤ R$ 2.259,20" --> A1["0%"]
    Faixa -- "≤ R$ 2.826,65" --> A2["7,5%"]
    Faixa -- "≤ R$ 3.751,05" --> A3["15%"]
    Faixa -- "≤ R$ 4.664,68" --> A4["22,5%"]
    Faixa -- "acima" --> A5["27,5%"]
```

*Tabela do Autônomo espelha a faixa progressiva mensal do IRPF/carnê-leão (valores de referência 2024).*

## 6. Projeção de fluxo de caixa — cálculo (backend, `DashboardAggregator`)

Endpoint `GET /api/dashboard/cash-flow-projection?months=N`, resolvido por `DashboardApplicationService` → `DashboardAggregator.cashFlowProjection(transacoes, saldoAtual, meses)` — método estático, puro, sem dependência de banco (recebe a lista de transações do usuário já carregada pelo service).

```mermaid
flowchart TD
    Start(["GET /api/dashboard/cash-flow-projection"]) --> Load["DashboardApplicationService\ncarrega transações + saldo atual do usuário"]
    Load --> Anchor["saldo atual\n(Account.calculateCurrentBalance, sem transações futuras)"]
    Anchor --> Loop["Para cada um dos próximos N meses\n(DashboardAggregator.nextMonths)"]

    Loop --> Check{"Já existem transações\ncadastradas nesse mês futuro?"}
    Check -- "Sim (recebível/despesa já lançado)" --> Real["líquido = receita − despesa\ndessas transações reais"]
    Check -- "Não" --> Media["líquido = média móvel do\nlíquido dos últimos 3 meses"]

    Real --> Acumula["saldo projetado += líquido"]
    Media --> Acumula
    Acumula --> Loop

    Loop --> Result["List&lt;CashFlowProjectionPoint&gt;\n(mês, saldo, projected: true)"]
    Result --> Mapper["DashboardWebMapper"]
    Mapper --> Response["CashFlowProjectionPointResponse[]"]
    Response --> Hook["useCashFlowProjection()\n(React Query)"]
    Hook --> Chart["CashFlowProjectionChart\n(Recharts) — histórico linha sólida\n+ projeção linha tracejada"]
```

**Por que fica no backend:** o resto do Dashboard (receita x despesa por mês, evolução do saldo, breakdown por categoria/cliente) segue essa mesma convenção desde a migração das agregações do frontend pro backend — toda a lógica de cálculo mora em `DashboardAggregator`, uma classe de domínio pura e testável isoladamente (sem precisar de `@SpringBootTest`), e o frontend só busca e exibe. Isso elimina duplicação de lógica de negócio entre cliente e servidor e garante que qualquer consumidor futuro da API (mobile, outro frontend) tenha a mesma regra de projeção sem reimplementá-la.

## 7. Onde cada peça vive no repositório

| Camada | Estimativa de imposto | Projeção de fluxo de caixa |
|---|---|---|
| Domínio | `domain/model/{TaxEstimate,TaxRegime,TaxRateEstimator}.java` | `domain/model/DashboardAggregator.java` (`cashFlowProjection`) + `domain/model/CashFlowProjectionPoint.java` |
| Port/Adapter | `domain/port/out/TaxEstimateRepositoryPort.java` + `infrastructure/persistence/adapter/TaxEstimateRepositoryAdapter.java` | — (lê `TransactionRepositoryPort`, sem persistência própria) |
| Aplicação | `application/taxestimate/TaxEstimateApplicationService.java` | `application/dashboard/DashboardApplicationService.java` |
| API | `infrastructure/web/controller/TaxEstimateController.java` (`/api/tax-estimates`) | `infrastructure/web/controller/DashboardController.java` (`/api/dashboard/cash-flow-projection`) |
| Migration | `db/migration/V10__add_regime_to_tax_estimates.sql` | — |
| Frontend | `frontend/src/features/taxEstimates/**` (`TaxEstimatesPage`, `TaxEstimateForm`, `TaxEstimateList`, hooks, api, schemas) | `../../frontend/src/features/dashboard/hooks/useCashFlowProjection.ts` + `components/CashFlowProjectionChart.tsx` |
| Testes | `backend/src/test/java/.../integration/taxestimate/TaxEstimateIntegrationTest.java` | `backend/src/test/java/.../domain/model/DashboardAggregatorTest.java` + `integration/dashboard/DashboardIntegrationTest.java` |
