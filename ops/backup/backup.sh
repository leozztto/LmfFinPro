#!/usr/bin/env bash
# Backup do FinPro: dump do Postgres + volume de anexos (já cifrados) + somas de verificação.
# Uso (na raiz do repositório, com o docker compose no ar):  ops/backup/backup.sh
# Variáveis: BACKUP_DIR (padrão ./backups), BACKUP_RETENTION_DAYS (padrão 30),
#            COMPOSE (padrão "docker compose"), POSTGRES_USER/POSTGRES_DB (padrão finpro).
# A chave FINPRO_ATTACHMENTS_ENCRYPTION_KEY NUNCA entra no backup: guarde-a em outro cofre.
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
COMPOSE="${COMPOSE:-docker compose}"
PGUSER="${POSTGRES_USER:-finpro}"
PGDB="${POSTGRES_DB:-finpro}"

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
dest="$BACKUP_DIR/finpro-$stamp"
mkdir -p "$dest"
umask 077

echo "[backup] dump do Postgres..."
$COMPOSE exec -T postgres pg_dump -U "$PGUSER" -d "$PGDB" --format=custom --no-owner \
  > "$dest/db.dump"

echo "[backup] anexos (arquivos já cifrados)..."
$COMPOSE exec -T backend tar -C /app/data/attachments -cf - . > "$dest/attachments.tar"

echo "[backup] lista de anexos esperada pelo banco..."
$COMPOSE exec -T postgres psql -U "$PGUSER" -d "$PGDB" -Atc \
  "select storage_key from transaction_attachments order by 1" > "$dest/attachments.keys"

( cd "$dest" && sha256sum db.dump attachments.tar attachments.keys > SHA256SUMS )
echo "$stamp" > "$dest/CREATED_AT_UTC"

# Sanidade: o dump precisa ser legível pelo pg_restore.
$COMPOSE exec -T postgres pg_restore --list < "$dest/db.dump" > /dev/null

echo "[backup] retenção: apagando backups com mais de $RETENTION_DAYS dias..."
find "$BACKUP_DIR" -maxdepth 1 -type d -name 'finpro-*' -mtime +"$RETENTION_DAYS" \
  -exec rm -rf {} +

echo "[backup] ok -> $dest"
echo "[backup] LEMBRETE: copie $dest para fora do servidor (outra região/provedor)."
