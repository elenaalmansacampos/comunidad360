#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

FILE="${1:?Uso: scripts/restore.sh <backup.dump> [base_destino]}"
TARGET="${2:-comunidad360_restore_test}"
USER="${POSTGRES_USER:-comunidad360}"

if [ "$TARGET" = "comunidad360" ] && [ "${ALLOW_PROD_RESTORE:-false}" != "true" ]; then
  echo "Restauración sobre la base principal bloqueada. Usa ALLOW_PROD_RESTORE=true solo en una ventana controlada." >&2
  exit 1
fi
if [ ! -f "$FILE" ]; then
  echo "No existe el backup: $FILE" >&2
  exit 1
fi

docker compose exec -T postgres dropdb -U "$USER" --if-exists "$TARGET"
docker compose exec -T postgres createdb -U "$USER" "$TARGET"
docker compose exec -T postgres pg_restore -U "$USER" -d "$TARGET" --no-owner < "$FILE"
echo "Restore OK en base $TARGET desde $FILE"
