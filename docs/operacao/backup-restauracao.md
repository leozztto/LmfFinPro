# Backup, restauração e criptografia dos anexos

*Runbook de operação. Scripts em [`ops/backup/`](../../ops/backup). Complementa [`observabilidade.md`](../tecnica/observabilidade.md) e [`plano-resposta-incidentes.md`](plano-resposta-incidentes.md).*

## 1. O que existe para guardar

| Dado | Onde fica | Cobertura do backup |
|---|---|---|
| Banco (usuários, grupos, transações, registros dos anexos…) | volume `finpro_postgres_data` | `pg_dump` em formato custom (`db.dump`) |
| Conteúdo dos anexos | volume `finpro_attachments`, **cifrado** (AES-256-GCM) | `attachments.tar` (os mesmos arquivos cifrados) |
| Chave dos anexos (`FINPRO_ATTACHMENTS_ENCRYPTION_KEY`) | `.env` do servidor | **fora do backup, de propósito** (ver seção 4) |
| Segredos (`JWT_SECRET`, SMTP, VAPID) | `.env` do servidor | fora do backup; cofre de segredos |

Um backup de banco **sem** a chave dos anexos restaura as transações, mas os comprovantes ficam ilegíveis. Um backup de anexos **sem** a chave não serve para nada. Por isso a chave tem a sua própria cópia, guardada separadamente.

## 2. Rotina de backup

```bash
# na raiz do repositório, com o docker compose no ar
ops/backup/backup.sh
```

Gera `backups/finpro-<AAAAMMDDThhmmssZ>/` com `db.dump`, `attachments.tar`, `attachments.keys` (chaves de anexo que o banco espera), `SHA256SUMS` e `CREATED_AT_UTC`. Apaga os backups locais mais antigos que `BACKUP_RETENTION_DAYS` (padrão **30 dias**, o prazo que a Política de Privacidade cita; se mudar o prazo, mude a Política e a versão do documento, ver `LegalDocuments`).

Agendamento sugerido (cron do host), diário às 03:00, e **cópia para fora do servidor** (outro provedor/região, com acesso restrito e criptografia em trânsito):

```cron
0 3 * * *  cd /opt/finpro && ops/backup/backup.sh >> /var/log/finpro-backup.log 2>&1
```

Metas iniciais (ajustar ao negócio): **RPO 24 h** (perde-se no máximo um dia) e **RTO 4 h**. Se o RPO precisar ser menor, ligar arquivamento de WAL/PITR no Postgres.

Pontos de atenção:

- O backup precisa ser monitorado: alerta se não houver diretório novo há mais de 26 h.
- Backups contêm dados pessoais (o banco está em claro no dump): proteja o diretório (`umask 077` já é aplicado) e a cópia remota (disco/bucket cifrado, acesso mínimo).
- **Exclusão de conta (LGPD):** os dados excluídos permanecem nos backups até expirarem (30 dias) e **nunca** são usados para restaurar contas excluídas. Depois de qualquer restauração, reaplique as exclusões pedidas desde a data do backup (consulte a tabela `account_deletion_log`) antes de reabrir o serviço.

## 3. Restauração

### 3.1 Ensaio (obrigatório, mensal e depois de mudar o esquema)

```bash
ops/backup/restore.sh --drill backups/finpro-<data>
```

Não toca na produção. Verifica as somas SHA-256, restaura o dump num banco temporário, mostra contagens (usuários, grupos, contas, transações, anexos, migrações), confere se cada registro de anexo tem o arquivo no `tar` e se os arquivos estão com o prefixo de criptografia, e apaga tudo. Falha (código ≠ 0) se faltar arquivo de anexo.

Registre o resultado na tabela abaixo. **Um backup nunca testado não conta como backup.**

| Data | Backup usado | Quem executou | Resultado | Tempo | Observações |
|---|---|---|---|---|---|
| _(preencher no primeiro ensaio)_ | | | | | |

### 3.2 Restauração real

1. Declare o incidente (se for o caso) e siga [`plano-resposta-incidentes.md`](plano-resposta-incidentes.md).
2. Tenha em mãos a **mesma** `FINPRO_ATTACHMENTS_ENCRYPTION_KEY` do momento do backup (cofre).
3. `docker compose stop backend`
4. `ops/backup/restore.sh --apply backups/finpro-<data>` (pede para digitar o nome do banco).
5. `docker compose start backend` e confira `curl -fsS localhost:8081/actuator/health` (a porta de gestão não é publicada; rode de dentro da rede do compose).
6. Faça login com uma conta de teste, abra uma transação com comprovante e baixe o anexo.
7. Reaplique as exclusões de conta posteriores ao backup (seção 2).
8. Registre horário de início/fim e a perda de dados efetiva (RPO real) para o pós-incidente.

## 4. Criptografia dos anexos em repouso

- **Algoritmo:** AES-256-GCM, IV aleatório de 12 bytes por arquivo. O arquivo no disco é `"FPE1"` + IV + texto cifrado + tag. O nome do arquivo (UUID) entra como dado autenticado: arquivo adulterado ou trocado de lugar **falha** na leitura em vez de ser servido.
- **Onde:** `LocalFileStorageAdapter` (porta `FileStoragePort`), então o resto do sistema não muda. O ZIP de comprovantes do ano e o pacote de exportação LGPD leem pelo mesmo caminho e continuam funcionando.
- **Chave:** `FINPRO_ATTACHMENTS_ENCRYPTION_KEY`, Base64 de 32 bytes (`openssl rand -base64 32`). O `docker-compose.yml` **recusa subir sem ela**. Sem a chave em dev o sistema grava em claro e avisa no log.
- **Guarda da chave:** pelo menos **duas cópias** em locais distintos do servidor (cofre de senhas da empresa + cópia offline lacrada), com acesso de poucas pessoas. Perdeu a chave, perdeu os comprovantes.
- **Ativação em ambiente que já tem anexos:**
  1. Faça um backup (seção 2) e **um ensaio** (3.1).
  2. Defina `FINPRO_ATTACHMENTS_ENCRYPTION_KEY` e `FINPRO_ATTACHMENTS_ENCRYPT_EXISTING=true` no `.env`; suba o backend. Os arquivos antigos (legíveis mesmo sem criptografia) são cifrados um a um, com gravação atômica; o log mostra `Anexos antigos cifrados convertidos=N`.
  3. Volte `FINPRO_ATTACHMENTS_ENCRYPT_EXISTING` para `false`.
  4. Rode um novo backup e o ensaio: o relatório deve dizer `em claro: 0`.
- **O que a criptografia protege / não protege:** protege contra cópia do disco/volume e contra backup vazado **sem** a chave. Não protege contra quem tem acesso à aplicação em execução (que tem a chave em memória), por isso o isolamento entre contas continua sendo testado ([`teste-de-invasao-grupos.md`](teste-de-invasao-grupos.md)). O banco de dados em si **não** está cifrado pela aplicação: use disco/volume cifrado no provedor.
- **Rotação da chave — pendente.** Ainda não há comando de rotação. Procedimento manual se a chave vazar: gerar chave nova, ler cada arquivo com a antiga e regravar com a nova (uma ferramenta pontual, a ser escrita), e tratar como incidente. Item aberto no final deste documento.

## 5. Pendências

- Ferramenta de **rotação da chave** dos anexos (re-cifrar tudo com chave nova, com versão da chave no cabeçalho).
- **Alerta de backup** (diretório mais recente com mais de 26 h) e cópia remota automatizada.
- **PITR** (arquivamento de WAL) se o RPO de 24 h for insuficiente.
- Cifrar o dump antes de enviá-lo para fora (`age`/`gpg`), com chave própria.
- Executar e registrar o **primeiro ensaio** na tabela da seção 3.1. Os scripts foram revisados (sintaxe) e o código de criptografia tem testes automatizados, mas o ensaio ponta a ponta com o compose ainda não foi registrado.
