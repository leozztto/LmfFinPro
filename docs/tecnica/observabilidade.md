# Observabilidade

Três peças: **métricas** (Prometheus), **logs estruturados** (JSON) e **alertas** sobre os schedulers.

## Porta de gestão

Os endpoints do actuator (`health`, `info`, `metrics`, `prometheus`) ficam numa porta própria,
`MANAGEMENT_PORT` (padrão **8081**). A porta da API (8080) não serve `/actuator`.

- **Não publique a 8081 na internet** nem a mapeie no `docker-compose` de produção: o endpoint
  `/actuator/**` é liberado no `SecurityConfig` justamente por só existir nessa porta. Deixe-a
  acessível apenas à rede interna / ao Prometheus.
- Nunca configure `MANAGEMENT_PORT` igual à porta da API: isso exporia as métricas publicamente.

Trecho de scrape:

```yaml
scrape_configs:
  - job_name: finpro-backend
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["backend:8081"]
```

Health checks (orquestrador): `GET :8081/actuator/health/liveness` e `/readiness`.

## Métricas dos schedulers

Todo `@Scheduled` roda dentro de `SchedulerMetrics.run`. Tag `scheduler` = nome do lock
(`recurringTransactions`, `recurringBudgets`, `savingsGoalContributions`, `dailyAlerts`,
`exchangeRates`, `refreshTokenCleanup`).

| Métrica | Significado |
| --- | --- |
| `finpro_scheduler_runs_total{scheduler,outcome}` | Execuções; `outcome=failure` quando o job lançou exceção |
| `finpro_scheduler_item_failures_total{scheduler}` | Itens do lote que falharam e foram isolados (o job segue) |
| `finpro_scheduler_duration_seconds` | Duração das execuções |
| `finpro_scheduler_last_success_timestamp_seconds{scheduler}` | Instante (epoch) da última execução sem exceção |

Além delas, vêm de graça as métricas de JVM, Hikari (pool de conexões) e HTTP
(`http_server_requests_seconds_*`).

Observação: uma réplica que não obtém a trava do ShedLock não executa o job, então só a réplica
que executa registra a métrica. Faça o alerta de "parado" sobre o agregado (`max by (scheduler)`),
não por instância, se rodar mais de uma réplica.

## Alertas

`ops/prometheus/finpro-alerts.yml` traz as regras prontas (inclua em `rule_files`):

- **FinProSchedulerFalhou** — uma execução inteira falhou na última hora.
- **FinProSchedulerItensComFalha** — itens isolados falharam (ex.: uma recorrência com dado ruim).
- **FinProSchedulerParado** — job diário sem sucesso há mais de 25 h (cobre scheduler que nunca
  dispara, p.ex. cron mal configurado).
- **FinProPtaxParada** — PTAX sem sucesso há mais de 4 dias (só roda em dias úteis).
- **FinProApiForaDoAr** e **FinProErros5xx** — disponibilidade e erros da API.

O destino das notificações (e-mail, Slack…) é configurado no Alertmanager, fora deste repositório.

## Logs estruturados

Por padrão o log é texto legível (dev). Para JSON, uma linha por evento, ative o perfil:

```
SPRING_PROFILES_ACTIVE=json
```

Config em `logback-spring.xml` (`logstash-logback-encoder`; o Spring Boot 3.3 ainda não tem
`logging.structured.*`, que só chega na 3.4). Cada evento leva `@timestamp`, `level`,
`logger_name`, `message`, `stack_trace` e `requestId`.

### Correlação por requisição

`RequestIdFilter` gera um `requestId` por requisição (ou reaproveita o `X-Request-Id` do proxy,
se for só `[A-Za-z0-9._-]` com até 64 caracteres), coloca no MDC e devolve no header
`X-Request-Id` da resposta. Para investigar um erro relatado pelo usuário, peça o valor desse
header e filtre os logs por ele.

## O que sai no log

- **Linha de acesso** (`RequestIdFilter`): uma por requisição, `Requisição HTTP method=GET path=/api/... status=200 durationMs=12`.
  Em JSON, `method`, `path`, `status` e `durationMs` viram campos do evento. Não inclui query string
  e ignora `/actuator`. Respostas 5xx saem em `WARN`, o resto em `INFO`.
- **Contexto (MDC)**: `requestId` em toda linha de uma requisição, `userId` nas autenticadas e
  `scheduler` nas linhas de um job. No log de texto aparecem como `[requestId|userId]`; no JSON,
  como atributos do evento.
- **Schedulers**: `Scheduler X iniciado` e `Scheduler X finalizado outcome=... durationMs=... items=... produced=... itemFailures=...` (resumo da execução; ver abaixo).
- **Chamadas externas**: BCB e ViaCEP (duração e falha, sem CEP), e-mail (`E-mail enviado kind=... durationMs=...` / `Falha ao enviar e-mail`, nunca o destinatário ou o texto).
- **Erros**: só falha de verdade (5xx) gera `ERROR` com stack trace. URL inexistente (404), método
  não permitido (405), JSON malformado ou parâmetro inválido (400) respondem 4xx sem stack trace.
  No JSON, o stack trace vem com a causa raiz primeiro, limitado a 25 frames e sem frames de
  Tomcat/filtros do Spring.

Exemplo de consulta (Loki): `{app="finpro"} | json | requestId="<valor do X-Request-Id>"`.

## Rastreando o que o usuário fez (fluxos)

Cada ação na tela vira uma requisição à API, e cada requisição gera, em ordem, estas linhas
(todas com o mesmo `requestId` e o `userId`):

```
INFO  Fluxo Transaction.list concluído durationMs=18          <- o caso de uso executado
INFO  Requisição HTTP method=GET path=/api/transactions status=200 durationMs=24   <- o HTTP
```

- **`Fluxo <Serviço>.<método>`** (`UseCaseLoggingAspect`): registra o caso de uso de negócio
  (`Account.create`, `Transfer.create`, `Auth.login`...), a duração, o contexto e, se falhar,
  `falhou error=<TipoDaExceção>`. Só o fluxo de fora é registrado. Dentro de scheduler, o sucesso
  sai em DEBUG (um por item); a falha sai sempre, e o resumo fica na linha do scheduler.
- **Contexto do fluxo**: só ids e contagens, nunca valores, nomes ou e-mails. O aspecto deduz
  sozinho os parâmetros `Long` terminados em `Id` (menos `currentUserId`, que já vai no MDC como
  `userId`), o `resultId` do retorno e o `resultCount` de listas. Desfechos que só o serviço sabe
  entram com `FlowLog.detail("chave", valor)` (classe `application/FlowLog`, no-op fora de um
  fluxo): importação (`rows`, `imported`, `duplicates`, `uncategorized`), alertas (`digestSent`,
  `bills`, `budgets`), recorrências (`generated`, `dueMonths`, `skippedExisting`), aporte
  automático (`contributed`), câmbio (`rates`), login (`reason=unknownEmail|wrongPassword`).
  Exemplo: `Fluxo Import.importFile concluído durationMs=210 accountId=7 batchId=31 rows=120
  imported=112 duplicates=8 uncategorized=15`.
- **`Requisição rejeitada <método> <caminho> -> <status>: <mensagem>`**: todo erro tratado (regra
  de negócio, validação, conflito) com a mesma mensagem que o usuário viu na tela. É a resposta
  para "por que apareceu este erro?". `WARN` para 4xx/503, `INFO` para 404.
- **`Requisição HTTP ... status=401/403`** sai em `WARN`: acesso negado.
- Erros 5xx: `ERROR` com o stack trace (sem mensagem para erros de banco).

Silenciar os fluxos: `LOGGING_LEVEL_COM_LMF_FINPRO_INFRASTRUCTURE_LOGGING_USECASELOGGINGASPECT=WARN`.

Observação: a navegação entre telas do frontend é feita no navegador (SPA); o backend só vê as
chamadas de dados que cada tela dispara. Menu que não carrega dados da API não gera linha.

## Resumo dos schedulers

Cada execução termina com uma linha de `INFO` que diz o que o job fez, mesmo quando não havia nada
a fazer:

```
Scheduler recurringTransactions finalizado outcome=success durationMs=340 items=37 produced=12 itemFailures=1
```

- `items`: itens do lote examinados (recorrências, metas, usuários).
- `produced`: o que o job efetivamente gerou, enviou ou removeu (ocorrências lançadas, e-mails de
  alerta enviados, aportes feitos, cotações salvas, refresh tokens apagados).
- `itemFailures`: itens que falharam e foram isolados (cada um com seu `ERROR`).

`items=N produced=0` significa que o job rodou e não havia nada vencido; `items=0` com o lote
esperado indica problema de dados ou de filtro. Para ver cada item, ligue o `DEBUG` do aspecto
(`LOGGING_LEVEL_COM_LMF_FINPRO_INFRASTRUCTURE_LOGGING_USECASELOGGINGASPECT=DEBUG`).

Ao criar um job novo, chame `metrics.itemDone(produced)` por item (ou `metrics.produced(n)` para
jobs sem lote) para o resumo sair completo.
