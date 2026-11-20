# COMUNIDAD 360 — Backups y restauración

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Plantilla operativa; validar en host real |
| Fecha | 2026-11-19 |

---

## 1. Objetivo

Disponer de backups cifrados de PostgreSQL, fuera del host, con retención y restauración probada.

## 2. Instalación en el host

Requisitos:

- Repositorio desplegado en `/opt/comunidad360`.
- Docker y Docker Compose operativos.
- OpenSSL instalado.
- Espacio suficiente en `/var/backups/comunidad360`.
- Almacenamiento externo o copia offsite.

```bash
sudo scripts/install-systemd-backup.sh
sudo editor /etc/comunidad360/backup.env
```

Configura:

```text
BACKUP_PASSPHRASE=<frase larga y aleatoria>
BACKUP_DIR=/var/backups/comunidad360
BACKUP_RETENTION_DAYS=30
POSTGRES_USER=comunidad360
POSTGRES_DB=comunidad360
```

Protege el archivo:

```bash
sudo chown root:root /etc/comunidad360/backup.env
sudo chmod 600 /etc/comunidad360/backup.env
```

Activa y comprueba:

```bash
sudo systemctl start comunidad360-backup.service
sudo systemctl status comunidad360-backup.service
sudo systemctl list-timers comunidad360-backup.timer
journalctl -u comunidad360-backup.service -n 100
```

## 3. Programación

- Diaria a las 03:00 con retardo aleatorio de hasta 15 minutos.
- `Persistent=true` recupera la ejecución si el host estaba apagado.
- Retención por defecto: 30 días.

## 4. Restauración

Nunca restaurar sobre producción sin ventana y autorización.

```bash
export BACKUP_PASSPHRASE='...'
scripts/restore-encrypted.sh /var/backups/comunidad360/comunidad360-....dump.enc comunidad360_restore_test
docker compose exec -T postgres psql -U comunidad360 -d comunidad360_restore_test -c "select count(*) from community;"
```

Para producción:

```bash
ALLOW_PROD_RESTORE=true scripts/restore-encrypted.sh <archivo> comunidad360
```

## 5. Copia offsite

- Sincronizar `/var/backups/comunidad360` a almacenamiento externo cifrado.
- Restringir acceso por cuenta de servicio.
- Verificar integridad y restaurar mensualmente.
- No usar el mismo host ni las mismas credenciales.

## 6. RPO/RTO

Definir con el Product Owner. Referencia propuesta:

- RPO: 24 horas con backup diario.
- RTO: 1–2 horas con infraestructura preparada.
- Probar y medir trimestralmente.

## 7. Checklist

- [ ] Passphrase fuera del repositorio.
- [ ] Timer activo.
- [ ] Backup diario generado.
- [ ] Copia offsite.
- [ ] Restauración en base temporal.
- [ ] RPO/RTO medidos.
- [ ] Responsable y alertas definidos.

---

*Fin de docs/BACKUP.md — v0.1.0.*
