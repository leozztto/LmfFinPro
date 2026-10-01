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
- **Schedulers**: `Scheduler X iniciado` e `Scheduler X finalizado outcome=... durationMs=...`.
- **Erros**: só falha de verdade (5xx) gera `ERROR` com stack trace. URL inexistente (404), método
  não permitido (405), JSON malformado ou parâmetro inválido (400) respondem 4xx sem stack trace.
  No JSON, o stack trace vem com a causa raiz primeiro, limitado a 25 frames e sem frames de
  Tomcat/filtros do Spring.

Exemplo de consulta (Loki): `{app="finpro"} | json | requestId="<valor do X-Request-Id>"`.
