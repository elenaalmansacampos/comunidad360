#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

USER="${POSTGRES_USER:-comunidad360}"
DB="${POSTGRES_DB:-comunidad360}"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="${1:-backups/comunidad360-${STAMP}.dump}"

mkdir -p "$(dirname "$OUT")"
docker compose exec -T postgres pg_dump -U "$USER" -d "$DB" --format=custom --no-owner > "$OUT"
docker compose exec -T postgres pg_restore --list < "$OUT" >/dev/null
echo "Backup OK: $OUT ($(du -h "$OUT" | cut -f1))"
