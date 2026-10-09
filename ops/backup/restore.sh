#!/usr/bin/env bash
# Restauração do FinPro a partir de um diretório gerado por backup.sh.
#
#   ops/backup/restore.sh --drill <dir>    ENSAIO: restaura num banco e numa pasta temporários,
#                                          confere integridade e apaga tudo. Não toca na produção.
#   ops/backup/restore.sh --apply <dir>    RESTAURA DE VERDADE (substitui banco e anexos). Pede
#                                          confirmação digitando o nome do banco.
#
# Requer o docker compose no ar (serviço postgres). Para --apply, pare o backend antes:
#   docker compose stop backend
set -euo pipefail

MODE="${1:-}"; SRC="${2:-}"
COMPOSE="${COMPOSE:-docker compose}"
PGUSER="${POSTGRES_USER:-finpro}"
PGDB="${POSTGRES_DB:-finpro}"

[[ "$MODE" == "--drill" || "$MODE" == "--apply" ]] && [[ -d "$SRC" ]] || {
  echo "uso: $0 --drill|--apply <diretório-do-backup>" >&2; exit 2; }

echo "[restore] verificando somas SHA-256..."
( cd "$SRC" && sha256sum -c SHA256SUMS )

psql_() { $COMPOSE exec -T postgres psql -U "$PGUSER" -v ON_ERROR_STOP=1 "$@"; }

if [[ "$MODE" == "--drill" ]]; then
  DRILL_DB="finpro_restore_drill"
  WORK="$(mktemp -d)"
  trap 'psql_ -d postgres -c "drop database if exists $DRILL_DB" >/dev/null; rm -rf "$WORK"' EXIT

  echo "[drill] restaurando no banco temporário $DRILL_DB..."
  psql_ -d postgres -c "drop database if exists $DRILL_DB" >/dev/null
  psql_ -d postgres -c "create database $DRILL_DB" >/dev/null
  $COMPOSE exec -T postgres pg_restore -U "$PGUSER" -d "$DRILL_DB" --no-owner --exit-on-error \
    < "$SRC/db.dump"

  echo "[drill] contagens do banco restaurado:"
  psql_ -d "$DRILL_DB" -Atc "select 'users', count(*) from users
    union all select 'households', count(*) from households
    union all select 'accounts', count(*) from accounts
    union all select 'transactions', count(*) from transactions
    union all select 'attachments', count(*) from transaction_attachments
    union all select 'migrations', count(*) from flyway_schema_history"

  echo "[drill] conferindo anexos x banco..."
  mkdir -p "$WORK/att"; tar -C "$WORK/att" -xf "$SRC/attachments.tar"
  ( cd "$WORK/att" && find . -type f ! -name '*.tmp' -printf '%P\n' | sort ) > "$WORK/files.txt"
  psql_ -d "$DRILL_DB" -Atc "select storage_key from transaction_attachments order by 1" \
    | sort > "$WORK/db.txt"
  missing="$(comm -13 "$WORK/files.txt" "$WORK/db.txt" | wc -l)"
  orphans="$(comm -23 "$WORK/files.txt" "$WORK/db.txt" | wc -l)"
  echo "[drill] registros sem arquivo: $missing | arquivos sem registro: $orphans"
  [[ "$missing" -eq 0 ]] || { echo "[drill] FALHOU: há anexos no banco sem arquivo no backup" >&2; exit 1; }

  echo "[drill] anexos sem o prefixo de criptografia (FPE1) — esperado 0 após a migração:"
  plain=0
  while IFS= read -r f; do
    [[ -z "$f" ]] && continue
    [[ "$(head -c4 "$WORK/att/$f")" == "FPE1" ]] || plain=$((plain+1))
  done < "$WORK/files.txt"
  echo "[drill] em claro: $plain"
  [[ "$plain" -eq 0 ]] || echo "[drill] ATENÇÃO: há anexos em claro; rode com FINPRO_ATTACHMENTS_ENCRYPT_EXISTING=true"

  echo "[drill] OK — o backup é restaurável. Registre a data e o tempo gasto no runbook."
  exit 0
fi

# --apply
echo "ISTO SUBSTITUI o banco '$PGDB' e os anexos do volume. O backend deve estar parado."
read -r -p "Digite o nome do banco ($PGDB) para confirmar: " answer
[[ "$answer" == "$PGDB" ]] || { echo "cancelado"; exit 1; }

psql_ -d postgres -c "select pg_terminate_backend(pid) from pg_stat_activity where datname='$PGDB' and pid<>pg_backend_pid()" >/dev/null
psql_ -d postgres -c "drop database if exists $PGDB" >/dev/null
psql_ -d postgres -c "create database $PGDB" >/dev/null
$COMPOSE exec -T postgres pg_restore -U "$PGUSER" -d "$PGDB" --no-owner --exit-on-error < "$SRC/db.dump"

echo "[restore] anexos..."
$COMPOSE run --rm --no-deps -T --entrypoint sh backend -c \
  'rm -rf /app/data/attachments/* && tar -C /app/data/attachments -xf -' < "$SRC/attachments.tar"

echo "[restore] pronto. Suba o backend (com a MESMA FINPRO_ATTACHMENTS_ENCRYPTION_KEY) e valide:"
echo "          docker compose start backend && curl -fsS localhost:8081/actuator/health"
