#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

: "${BACKUP_PASSPHRASE:?Define BACKUP_PASSPHRASE para cifrar el backup}"
OUTDIR="${BACKUP_DIR:-backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
STAMP="$(date +%Y%m%d-%H%M%S)"
RAW="$(mktemp "${TMPDIR:-/tmp}/comunidad360-${STAMP}.XXXXXX.dump")"
trap 'rm -f "$RAW"' EXIT

mkdir -p "$OUTDIR"
scripts/backup.sh "$RAW" >/dev/null
OUT="$OUTDIR/comunidad360-${STAMP}.dump.enc"
openssl enc -aes-256-cbc -salt -pbkdf2 -pass env:BACKUP_PASSPHRASE -in "$RAW" -out "$OUT"
openssl enc -d -aes-256-cbc -pbkdf2 -pass env:BACKUP_PASSPHRASE -in "$OUT" | docker compose exec -T postgres pg_restore --list >/dev/null
find "$OUTDIR" -name 'comunidad360-*.dump.enc' -type f -mtime +"$RETENTION_DAYS" -delete
echo "Backup cifrado OK: $OUT"
