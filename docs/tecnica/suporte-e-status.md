# FinPro — Suporte e status da plataforma

*Canal de atendimento e página de status. Complementa o FAQ (`/faq`), que continua sendo a primeira linha de ajuda.*

## 1. Página `/suporte`

Rota **pública** (fora do `AppLayout`), para continuar acessível a quem não consegue entrar. Links: rodapé do app, rodapé das telas e item "Suporte" no menu lateral.

- **Canais:** e-mail e telefone vêm de `LEGAL_ENTITY` (`frontend/src/features/legal/legalEntity.ts`), a mesma fonte dos Termos e da Política. Trocar o dado lá atualiza tudo.
- **Orientação:** o que informar ao escrever, aviso de que a senha nunca é pedida e remissão à Política de Privacidade para pedidos de LGPD.
- **Não há** horário de atendimento nem prazo de resposta: só acrescentar quando a operação definir.

## 2. Status da plataforma

`GET /api/status` (público, `Cache-Control: no-store`):

```json
{ "status": "OPERATIONAL", "checkedAt": "2026-10-08T12:00:00" }
```

- O estado geral é `OPERATIONAL` ou `OUTAGE`, sem detalhe por componente (não expomos a infraestrutura). A API respondendo já prova que ela está de pé; o banco é checado com `Connection.isValid(2s)` (`PlatformHealthJdbcAdapter`, porta `PlatformHealthPort`). Banco fora → `OUTAGE`.
- `PlatformStatusService` não leva o sufixo `ApplicationService` de propósito: o aspecto de log de fluxo registraria cada consulta anônima.
- O frontend consulta a cada minuto (`usePlatformStatus`) e tem botão "Atualizar". Se nem o endpoint responde, a página diz que não conseguiu falar com o servidor.
- **Limite por IP**, como no login (`AuthRateLimitFilter`): 30 consultas por minuto por IP (`finpro.rate-limit.status`), 429 com `Retry-After` acima disso.
- **Monitor externo:** `.github/workflows/uptime-check.yml` consulta `/api/status` de fora a cada ~5 min. Se a plataforma não responder ou não estiver `OPERATIONAL`, manda um push urgente para o celular do dono pelo ntfy (app gratuito, sem conta: o tópico secreto `NTFY_TOPIC` é a "senha"), sem depender das notificações do GitHub. Configuração: variável `STATUS_URL`, secret `NTFY_TOPIC` e, opcionalmente, variável `NTFY_SERVER`. Sem `STATUS_URL` o monitor não faz nada.
- **Limites:** o agendamento do GitHub é best-effort (mínimo de 5 min, atrasos de alguns minutos, pausa após 60 dias sem atividade no repositório), e o aviso se repete a cada execução enquanto estiver fora. Para checagem de 1 min, ligação/SMS e histórico, use também um serviço de uptime dedicado (ex.: UptimeRobot, Better Stack).

## 3. Onde cada peça vive

- **Backend:** `application/status` (`PlatformStatusService`, `PlatformStatus`), `PlatformHealthPort`, `PlatformHealthJdbcAdapter`, `StatusController`; `/api/status` liberado em `SecurityConfig`.
- **Frontend:** `features/support` (`SupportPage`, `usePlatformStatus`, `statusApi`).
