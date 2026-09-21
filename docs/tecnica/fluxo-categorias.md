# FinPro — Fluxo de Categorias e Regras de Categorização

*Documentação técnica do módulo de categorias (ver roadmap em [`../plano.md`](../plano.md)). Diagramas em [Mermaid](https://mermaid.js.org/) — renderizam nativamente no GitHub.*

## 1. Visão geral

Categorias classificam transações em receita ou despesa (`CategoryType.INCOME`/`EXPENSE`) e alimentam o Dashboard, os Orçamentos e a importação de extrato. Existem dois "donos" possíveis:

- **Categorias globais** (`userId = null`): padrão do sistema, visíveis para todos os usuários, somente leitura via API — não podem ser editadas nem removidas por ninguém.
- **Categorias próprias**: criadas pelo usuário, só ele enxerga e só ele edita/remove.

**Regras de categorização** (`CategoryRule`) não são um CRUD independente na navegação principal: são o motor que categoriza automaticamente transações importadas de um extrato CSV, e vivem dentro da tela de **Importações**, não da tela de Categorias. Este documento cobre o CRUD de categorias e o modelo/API das regras; o fluxo completo de como uma regra nasce e é reforçada durante uma importação está em [`fluxo-importacao-extrato.md`](fluxo-importacao-extrato.md).

```mermaid
flowchart LR
    subgraph Backend["Backend (Spring Boot)"]
        CatDB[("categories\n(Postgres)")]
        RuleDB[("category_rules\n(Postgres)")]
        CatAPI["/api/categories"]
        RuleAPI["/api/category-rules"]
        CatAPI --> CatDB
        RuleAPI --> RuleDB
        RuleAPI -.->|categoryId referencia| CatDB
    end

    subgraph Frontend["Frontend (React)"]
        CategoriasPage["Página Categorias\n/categorias"]
        RulesPanel["Painel Regras de categorização\naba dentro de /importacoes"]
    end

    CategoriasPage <-- "REST (fetch)" --> CatAPI
    RulesPanel <-- "REST (fetch)" --> RuleAPI
    RulesPanel -- "lista categorias p/ o select" --> CatAPI
```

## 2. Tela

### 2.1 Categorias (`/categorias`)

- Botão "+" no topo abre um `Modal` com o `CategoryForm` (criação).
- `CategoryList` traz um filtro colapsável (`CollapsibleFilters`) com três campos: nome (busca por texto), tipo (Receita/Despesa/Todos) e origem (Minhas categorias/Padrão do sistema/Todas).
- Cada categoria aparece como um cartão com uma bolinha colorida (`category.color`), nome, tipo e, se for global, o rótulo "· padrão do sistema".
- Categorias globais **não mostram os botões de editar/remover** — só as próprias têm ícones de lápis (abre o mesmo `Modal`/`CategoryForm` em modo edição) e lixeira (confirma via `ConfirmContext` antes de remover).
- Estados: "Carregando categorias..." enquanto a query não resolve; "Nenhuma categoria cadastrada ainda" se a lista vier vazia; "Nenhuma categoria encontrada com os filtros aplicados" se o filtro zerar o resultado.
- No formulário de edição, o campo **Tipo fica desabilitado** (`disabled={isEditing}`) — reflete no frontend a mesma regra que o backend já impõe.

### 2.2 Regras de categorização (dentro de `/importacoes`)

- `ImportsPage` usa um componente `Tabs` com duas abas: "Importar arquivo" e "Regras de categorização" (`CategoryRulesPanel`).
- O painel tem um formulário inline (padrão + select de categoria) e uma lista abaixo mostrando padrão → categoria (com a cor da categoria) e o **peso** atual da regra.
- Cada linha tem um botão de remover, com confirmação explicando a consequência: "Transações futuras com esse descritivo deixarão de ser categorizadas automaticamente."
- Estados: "Carregando regras..." e "Nenhuma regra cadastrada ainda."

## 3. Categorias — arquitetura (hexagonal)

```mermaid
flowchart TD
    Client(["Cliente HTTP\n(frontend)"]) --> Controller

    subgraph web["infrastructure/web"]
        Controller["CategoryController\n/api/categories"]
        ReqDTO["CategoryRequest"]
        RespDTO["CategoryResponse\n(inclui global: boolean)"]
        WebMapper["CategoryWebMapper"]
    end

    subgraph application["application/category"]
        Service["CategoryApplicationService\ncreate · list · getById · update · delete"]
    end

    subgraph domain["domain"]
        Model["Category\n(record + create()/withDetails())"]
        Type["CategoryType\n(INCOME/EXPENSE)"]
        Port["CategoryRepositoryPort"]
        TxPort["TransactionRepositoryPort\n(só p/ checar vínculo no delete)"]
    end

    subgraph infra["infrastructure/persistence"]
        Adapter["CategoryRepositoryAdapter"]
        PMapper["CategoryPersistenceMapper"]
        JpaRepo["CategoryJpaRepository"]
        Entity["CategoryJpaEntity"]
    end

    DB[("categories\n(Postgres)")]

    Controller -- "@Valid" --> ReqDTO
    Controller --> Service
    Service --> Model
    Model --> Type
    Service --> Port
    Service --> TxPort
    Port -.->|implementa| Adapter
    Adapter --> PMapper
    Adapter --> JpaRepo
    PMapper --> Entity
    JpaRepo --> DB
    Service --> WebMapper
    WebMapper --> RespDTO
    RespDTO --> Client
```

`Category.isGlobal()` é simplesmente `userId == null` — não existe uma flag separada no banco, a "globalidade" é inferida da ausência de dono. `isVisibleTo(userId)` (usada no `list`/`getById`) é `isGlobal() || isOwnedBy(userId)`; `isOwnedBy(userId)` (usada no `update`/`delete`) exige dono, então uma tentativa de editar/remover uma categoria global cai no mesmo 404 de "não encontrada" — o backend não distingue "não existe" de "não é sua" (mesma convenção usada em todos os módulos, evita vazar informação sobre recursos de terceiros).

## 4. Fluxo de criação, edição e remoção

```mermaid
sequenceDiagram
    actor U as Usuário
    participant Form as CategoryForm
    participant List as CategoryList
    participant API as categoriesApi
    participant Ctrl as CategoryController
    participant Svc as CategoryApplicationService
    participant TxRepo as TransactionRepositoryPort
    participant DB as Postgres

    U->>Form: preenche nome, tipo, cor e confirma
    Form->>API: POST /categories { name, type, color, icon }
    API->>Ctrl: create(request)
    Ctrl->>Svc: create(userId, name, type, color, icon)
    Svc->>DB: INSERT (via Category.create() + repository)
    DB-->>Svc: categoria salva (com id)
    Svc-->>Ctrl: Category
    Ctrl-->>API: 201 Created
    API-->>List: invalida cache de categorias
    Form-->>U: toast "Categoria criada com sucesso"

    Note over U,Svc: Edição — o tipo NUNCA muda, mesmo se o request enviar um valor diferente
    U->>Form: edita nome/cor e confirma (tipo desabilitado na UI)
    Form->>API: PUT /categories/{id} { name, type, color, icon }
    API->>Ctrl: update(id, request)
    Ctrl->>Svc: update(userId, id, name, color, icon)
    Svc->>Svc: findOwnedOrThrow(id)\nexisting.withDetails(name, existing.type(), color, icon)
    Note right of Svc: existing.type() — o tipo do REQUEST é ignorado
    Svc->>DB: UPDATE
    DB-->>Svc: categoria atualizada
    Svc-->>Ctrl: Category
    Ctrl-->>API: 200 OK
    API-->>List: invalida cache

    Note over U,DB: Remoção — bloqueada se houver transação vinculada
    U->>List: clica em remover, confirma no ConfirmContext
    List->>API: DELETE /categories/{id}
    API->>Ctrl: delete(id)
    Ctrl->>Svc: delete(userId, id)
    Svc->>Svc: findOwnedOrThrow(id)
    Svc->>TxRepo: existsByCategoryId(id)
    alt existe transação vinculada
        TxRepo-->>Svc: true
        Svc-->>Ctrl: EntityHasLinkedRecordsException
        Ctrl-->>API: 409 Conflict
        API-->>List: toast de erro
    else sem vínculo
        TxRepo-->>Svc: false
        Svc->>DB: DELETE
        DB-->>Svc: ok
        Svc-->>Ctrl: void
        Ctrl-->>API: 204 No Content
        API-->>List: invalida cache + toast de sucesso
    end
```

**Por que o tipo é imutável:** transações já lançadas numa categoria foram validadas contra o tipo dela no momento da criação (uma despesa não pode usar categoria de receita, e vice-versa — ver [`fluxo-transacoes.md`](fluxo-transacoes.md)). Se o tipo pudesse mudar depois, transações antigas ficariam inconsistentes com o tipo atual da categoria. Essa regra foi endurecida numa auditoria de responsabilidades desta sessão: o backend ignora o campo `type` do `CategoryRequest` na atualização (`existing.withDetails(name, existing.type(), color, icon)`), e o frontend desabilita o campo no formulário de edição só por clareza de UX — quem garante a regra de verdade é o backend.

## 5. Regras de categorização (`CategoryRule`) — modelo e API

```mermaid
flowchart TD
    Create(["POST /category-rules\n{ pattern, categoryId }"]) --> Visible{"Categoria é visível\npro usuário?\n(própria ou global)"}
    Visible -- não --> NotFound["404 — categoria não encontrada"]
    Visible -- sim --> Save["CategoryRule.create(userId, pattern.trim(), categoryId)\nweight inicial = 1"]
    Save --> DB[("category_rules")]

    Match(["rule.matches(descricaoTransacao)"]) --> Contains{"descrição contém\npattern (case-insensitive)?"}
    Contains -- sim --> Yes["true — regra candidata"]
    Contains -- não --> No["false"]

    Reinforce(["rule.reinforcedWith(categoriaConfirmada)"]) --> Same{"categoria confirmada ==\ncategoria da regra?"}
    Same -- sim --> Inc["weight += 1\n(regra fica mais forte)"]
    Same -- não --> Reset["categoryId muda p/ a nova\nweight volta a 1"]
```

`CategoryRule` é um record de domínio puro (`matches`, `reinforcedWith`, `withDetails`) sem dependência de banco — a lógica de "aprendizado" (reforçar o peso a cada confirmação, resetar quando o usuário corrige) está inteiramente nele, testável sem Spring. `CategoryRuleApplicationService.create` só adiciona uma checagem: a categoria referenciada precisa ser visível ao usuário (própria ou global), senão é 404 — o mesmo padrão de `CategoryApplicationService`. A API expõe só `list`, `create` e `delete` — não existe `update` direto pelo usuário; o único jeito de uma regra mudar é sendo reforçada automaticamente durante a revisão de uma importação (detalhado em [`fluxo-importacao-extrato.md`](fluxo-importacao-extrato.md)).

## 6. Onde cada peça vive no repositório

| Camada | Categorias | Regras de categorização |
|---|---|---|
| Domínio | `domain/model/{Category,CategoryType}.java` | `domain/model/CategoryRule.java` |
| Port/Adapter | `domain/port/out/CategoryRepositoryPort.java` + `infrastructure/persistence/adapter/CategoryRepositoryAdapter.java` | `domain/port/out/CategoryRuleRepositoryPort.java` + `infrastructure/persistence/adapter/CategoryRuleRepositoryAdapter.java` |
| Aplicação | `application/category/CategoryApplicationService.java` | `application/categoryrule/CategoryRuleApplicationService.java` |
| API | `infrastructure/web/controller/CategoryController.java` (`/api/categories`) | `infrastructure/web/controller/CategoryRuleController.java` (`/api/category-rules`) |
| Frontend | `frontend/src/features/categories/**` (`CategoriesPage`, `CategoryForm`, `CategoryList`, hooks, `api/categoriesApi.ts`, `schemas.ts`, `types.ts`) | `frontend/src/features/categoryRules/**` (`CategoryRulesPanel`, hooks, `api/categoryRulesApi.ts`, `schemas.ts`) — renderizado dentro de `../../frontend/src/features/importBatches/components/ImportsPage.tsx` |
| Testes | `backend/src/test/java/.../integration/category/CategoryIntegrationTest.java` + `application/category/CategoryApplicationServiceTest.java` + `domain/model/CategoryTest.java` | `application/categoryrule/CategoryRuleApplicationServiceTest.java` + `domain/model/CategoryRuleTest.java` (a cobertura de integração do fluxo completo de reforço está em `ImportBatchIntegrationTest`) |
