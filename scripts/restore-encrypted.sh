#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

: "${BACKUP_PASSPHRASE:?Define BACKUP_PASSPHRASE para descifrar el backup}"
FILE="${1:?Uso: scripts/restore-encrypted.sh <backup.dump.enc> [base_destino]}"
TARGET="${2:-comunidad360_restore_test}"
RAW="$(mktemp "${TMPDIR:-/tmp}/comunidad360-restore.XXXXXX.dump")"
trap 'rm -f "$RAW"' EXIT

openssl enc -d -aes-256-cbc -pbkdf2 -pass env:BACKUP_PASSPHRASE -in "$FILE" -out "$RAW"
scripts/restore.sh "$RAW" "$TARGET"
