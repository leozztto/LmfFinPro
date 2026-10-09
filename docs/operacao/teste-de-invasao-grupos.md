# Teste de invasão básico: vazamento entre contas e grupos

*Roteiro, resultado da rodada de 09/10/2026 e como repetir. Teste automatizado: `backend/src/test/java/com/lmf/finpro/integration/security/HouseholdIsolationPentestIntegrationTest.java` (roda na CI do backend).*

## 1. Por que

Desde os grupos compartilhados, todo dado financeiro pertence a um grupo (`households`) e o grupo ativo vem do header `X-Household-Id`, escolhido pelo cliente. Um erro de verificação em qualquer ponto transforma o header, um id sequencial ou um link de convite em acesso ao dinheiro e aos comprovantes de outras pessoas. É o maior risco de vazamento do produto.

## 2. Modelo de ameaça

- **Atacante:** usuário autenticado e legítimo (`eve`), sem relação com o grupo-alvo; ou ex-membro; ou quem só tem um link de convite; ou quem não está autenticado.
- **Alvo:** grupo compartilhado de `ana` (dona) e `bia` (membro), com conta, categoria, transação e comprovante (PNG).
- **Fora do escopo desta rodada:** XSS/CSRF no frontend, ataque à infraestrutura (SO, Docker, rede, TLS), força bruta de senha (há rate limit com teste próprio, `RateLimitIntegrationTest`), engenharia social, DoS. Ver seção 5.

## 3. Roteiro e resultado

Resultado de todos os casos: **bloqueado como esperado** (17 testes, 0 falhas). Nenhuma vulnerabilidade de isolamento foi encontrada nesta rodada.

| # | Ataque | Esperado | Teste |
|---|---|---|---|
| 1 | `X-Household-Id` do grupo-alvo em leituras (contas, transações, categorias, painel, patrimônio, ZIP de comprovantes, lista e conteúdo de anexos) | 403, sem dados | `outsiderCannotEnterTheGroupByForgingTheHeader` |
| 2 | Mesmo header em escritas e exclusões (criar conta, apagar transação, apagar anexo) | 403 e nada muda | `forgedHeaderCannotWriteOrDeleteEither` |
| 3 | Header malformado/injeção: `abc`, `-1`, `0`, número gigante, `id OR 1=1`, `id,1`, `1;id`, hex, `id.0` | 403, sem cair no espaço de outro | `malformedHeaderValuesNeverFallBackToAnotherSpace` |
| 4 | Ids do alvo (conta, transação, categoria, anexo) pedidos a partir do espaço pessoal e de outro grupo da `eve` | 404/403, sem dados | `outsiderGuessingIdsFromHerOwnSpaceGetsNothing` |
| 5 | Alterar/excluir por id adivinhado (PUT/DELETE conta, DELETE transação/anexo/categoria, upload de anexo) | 404/403; membros seguem vendo tudo | `outsiderCannotModifyOrDeleteByGuessingIds` |
| 6 | Escrita no espaço da `eve` apontando para registros do grupo (transação em conta alheia, categoria alheia, anexo em transação alheia) | negado | `outsiderCannotPointHerOwnWritesAtTheGroupsRecords` |
| 7 | Listagens do espaço da `eve` não contêm dados do grupo | sem o marcador secreto | `listsOfAnotherSpaceNeverContainTheGroupsData` |
| 8 | Anexo pedido por uma transação que não é a dele, dentro do mesmo grupo | 404 | `attachmentIdIsBoundToItsTransactionEvenInsideTheSameGroup` |
| 9 | Membro removido continua usando o header | 403 na requisição seguinte (leituras e conteúdo de anexo) | `removedMemberLosesAccessOnTheNextRequest` |
| 10 | Membro comum tenta convidar, remover o dono, tomar a posse, ver convites; não-membro lista membros/convites, sai ou compartilha contas do grupo | 403/negado, sem e-mails vazados | `aMemberOnlyManagesPeopleIfHeIsTheOwner` |
| 11 | Membro tenta excluir lançamento de outro membro | 403 (só o autor exclui) | `onlyTheAuthorDeletesARecordInASharedAccount` |
| 12 | Reaproveitar um link de convite já usado | 400 e a `eve` continua de fora | `usedInviteTokenCannotBeReplayedByAnotherAccount` |
| 13 | Tokens de convite chutados e injeção; lista de convites do dono não traz token | 400; sem token na lista | `guessedInviteTokensAreRejectedAndTheListNeverExposesTokens` |
| 14 | Aceitar/recusar pelo app o convite de outro e-mail (id sequencial) | 400 (parece inexistente) | `inAppInviteForAnotherEmailLooksNonexistent` |
| 15 | Sem token, token adulterado, `alg: none`, payload trocado com assinatura antiga, lixo, vazio | 401 | `requestsWithoutAValidTokenAreRejected` |
| 16 | "Esqueci a senha" não revela se o e-mail existe | respostas idênticas | `passwordResetEndpointDoesNotRevealWhichEmailsExist` |
| 17 | Anexo no disco: conteúdo não aparece em claro; membro ainda baixa o original | cifrado em repouso | `attachmentsAreEncryptedOnDiskAndStillServedToMembers` |

Como ler o resultado: um erro de autorização aqui costuma ser silencioso (a tela funciona normalmente), por isso o teste usa um **marcador único** no nome da conta, da categoria, da transação e do grupo e confere que ele **nunca** aparece em resposta destinada a quem não pertence.

## 4. Riscos conhecidos e aceitos (não são falhas do teste)

- **Quem tem o link de convite entra** com qualquer e-mail (decisão de produto, ver `fluxo-grupos.md`, seção 4). O link vale 7 dias, é de uso único e o banco guarda só o hash. Tratar o e-mail do convite como dado sensível; reduzir a validade ou amarrar ao e-mail é uma opção se o risco crescer.
- **Membro vê e edita todos os dados do grupo**, inclusive contas de outros membros (só a exclusão é restrita ao autor). É o modelo de casal/família.
- **Nome da conta do outro lado de uma transferência entre espaços** aparece na lista de transferências (decisão deliberada, `fluxo-grupos.md`, seção 5).
- **Contas vinculáveis entre dois grupos diferentes** (pendência listada em `fluxo-grupos.md`, seção 10).

## 5. Fora do escopo desta rodada e próximos passos

1. **Varredura automática** contra um ambiente de homologação com OWASP ZAP (baseline e API scan) — cobre cabeçalhos, XSS refletido, injeção e erros de configuração que este teste não cobre.
2. **Revisão do frontend**: armazenamento do grupo ativo (`householdStorage`), renderização de campos livres (nomes, descrições) sem `dangerouslySetInnerHTML`, política de segurança de conteúdo (CSP) no nginx.
3. **Teste de cabeçalhos e TLS** no ambiente real (HSTS, cookies `Secure`/`SameSite`, porta 8081 e Swagger fechados): `curl -I`, `testssl.sh`.
4. **Teste externo** por terceiro antes do lançamento público, com escopo e autorização por escrito.
5. Novos endpoints: toda rota nova que receba um id precisa de um caso neste teste (combinar como regra de revisão de PR).

## 6. Como repetir

```bash
cd backend
mvn test -Dtest=HouseholdIsolationPentestIntegrationTest   # requer Docker (Testcontainers)
```

Para um caso manual contra um ambiente de homologação (nunca produção), com duas contas sem relação:

```bash
# eve tenta ler o grupo de ana (G = id do grupo)
curl -i -H "Authorization: Bearer $TOKEN_EVE" -H "X-Household-Id: $G" https://homolog/api/accounts      # esperado: 403
# eve tenta um anexo de ana pelo espaço dela
curl -i -H "Authorization: Bearer $TOKEN_EVE" https://homolog/api/transactions/$TX/attachments/$ANEXO/content   # esperado: 404
```
