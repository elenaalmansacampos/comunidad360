#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "Ejecuta este script con sudo." >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_DIR="${COMUNIDAD360_DIR:-/opt/comunidad360}"
ENV_FILE="/etc/comunidad360/backup.env"

install -d "$TARGET_DIR" "$TARGET_DIR/scripts" "$TARGET_DIR/docs" /etc/comunidad360 /var/backups/comunidad360
install -m 0755 "$ROOT_DIR/scripts/backup.sh" "$TARGET_DIR/scripts/backup.sh"
install -m 0755 "$ROOT_DIR/scripts/backup-encrypted.sh" "$TARGET_DIR/scripts/backup-encrypted.sh"
install -m 0755 "$ROOT_DIR/scripts/restore.sh" "$TARGET_DIR/scripts/restore.sh"
install -m 0755 "$ROOT_DIR/scripts/restore-encrypted.sh" "$TARGET_DIR/scripts/restore-encrypted.sh"
install -m 0644 "$ROOT_DIR/docs/BACKUP.md" "$TARGET_DIR/docs/BACKUP.md"
install -m 0644 "$ROOT_DIR/deploy/systemd/comunidad360-backup.service" /etc/systemd/system/
install -m 0644 "$ROOT_DIR/deploy/systemd/comunidad360-backup.timer" /etc/systemd/system/

if [ ! -f "$ENV_FILE" ]; then
  install -m 0600 "$ROOT_DIR/deploy/systemd/backup.env.example" "$ENV_FILE"
  echo "Revisa y protege $ENV_FILE antes de activar el timer."
fi

systemctl daemon-reload
systemctl enable --now comunidad360-backup.timer
echo "Timer instalado. Estado: systemctl status comunidad360-backup.timer"
