# COMUNIDAD 360 — Despliegue de producción

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Plantilla de despliegue; validar en infraestructura real |
| Fecha | 2026-11-19 |
| Perfil | `prod` |

---

## 1. Requisitos

- HTTPS terminado en reverse proxy (Nginx, Traefik, ALB o equivalente).
- PostgreSQL 16 gestionado o con backups automatizados.
- S3 compatible privado para documentos y fotos.
- SMTP transaccional con SPF/DKIM/DMARC.
- Secreto JWT de al menos 64 caracteres aleatorios.
- Dominio y cookies `Secure`.
- Monitorización de salud, logs y errores.
- Backups cifrados fuera del host.

## 2. Variables obligatorias

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
APP_SECURITY_JWT_SECRET=...
APP_CORS_ALLOWED_ORIGINS=https://app.example.com
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=...
SPRING_MAIL_PASSWORD=...
APP_MAIL_FROM=no-reply@example.com
APP_MAIL_FROM_NAME=Comunidad 360
APP_STORAGE_S3_ENDPOINT=https://s3.example.com
APP_STORAGE_S3_PUBLIC_ENDPOINT=https://s3.example.com
APP_STORAGE_S3_REGION=eu-west-1
APP_STORAGE_S3_BUCKET=comunidad360-documents
APP_STORAGE_S3_ACCESS_KEY=...
APP_STORAGE_S3_SECRET_KEY=...
APP_STORAGE_S3_PATH_STYLE=false
APP_RATE_LIMIT_AUTH_PER_MINUTE=20
```

El perfil `prod` no arranca si faltan el secreto JWT, la base de datos o la configuración de correo.
No usa el seed de desarrollo.

## 3. Reverse proxy

Requisitos mínimos:

- TLS 1.2+ y HSTS.
- Redirección de HTTP a HTTPS.
- `X-Forwarded-Proto`, `X-Forwarded-For` y `Host`.
- Límite de tamaño de subida y timeouts.
- Rate limiting adicional en login y API.
- Sanitizar `X-Forwarded-For` en el proxy; el rate limit de aplicación usa la IP de la petición.
- Para varias réplicas, usar rate limiting distribuido en el proxy o Redis.
- CSP, Permissions-Policy, X-Frame-Options y Referrer-Policy.
- No exponer PostgreSQL, S3 interno ni Mailpit.

Ejemplo de cabeceras:

```nginx
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'; object-src 'none'" always;
add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;
add_header X-Frame-Options "DENY" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
```

## 4. Cookies y autenticación

- `app.security.cookie-secure=true`.
- `SameSite=Lax` por defecto; evaluar `Strict` si no rompe flujos.
- Access token de 15 minutos.
- Refresh token `HttpOnly`.
- Rotación de secreto JWT planificada.
- MFA recomendado para administradores antes de comercializar.

## 5. Antimalware

Activar ClamAV antes de aceptar documentos en producción:

```bash
docker compose --profile av up -d clamav
export APP_MALWARE_SCAN_ENABLED=true
export APP_MALWARE_SCAN_HOST=clamav
export APP_MALWARE_SCAN_PORT=3310
```

- El backend escanea documentos y fotografías antes de almacenarlas.
- Si el archivo está infectado o el escáner falla, la subida se rechaza (`422`).
- El perfil `prod` activa el escaneo por defecto.
- Monitorizar disponibilidad de ClamAV y tasa de errores de análisis.
- Para almacenamiento interno, no exponer el puerto 3310 públicamente.

## 6. Backups

Cifrado con OpenSSL:

```bash
export BACKUP_PASSPHRASE='...'
export BACKUP_DIR=/ruta/segura/backups
scripts/backup-encrypted.sh
```

Restauración:

```bash
export BACKUP_PASSPHRASE='...'
scripts/restore-encrypted.sh /ruta/segura/backups/comunidad360-....dump.enc comunidad360_restore_test
```

Reglas:

- Programar diariamente.
- Copiar fuera del host.
- Probar restauración mensual.
- Definir RPO/RTO con el Product Owner.
- No restaurar sobre producción sin ventana y autorización.

## 7. Monitorización

Mínimo:

- `/actuator/health` para liveness/readiness.
- `/actuator/prometheus` para métricas; no exponerlo públicamente, solo a la red de monitorización.
- Alertas de errores 5xx, latencia y saturación.
- Logs centralizados con `X-Trace-Id`.
- Alertas de fallos de email y S3.
- Panel de backups y espacio en disco.
- Revisión periódica de auditoría.

## 8. Despliegue

1. Construir imágenes desde un commit etiquetado.
2. Ejecutar migraciones Flyway al arrancar el backend.
3. Verificar health y login.
4. Ejecutar smoke de lectura.
5. Comprobar que no se exponen puertos internos.
6. Etiquetar la release y registrar el despliegue.

## 9. Rollback

- Mantener la imagen anterior.
- No revertir migraciones destructivas sin plan.
- Si falla el backend, volver a la imagen anterior y restaurar backup solo si hay corrupción.
- Documentar causa, impacto y acciones.

## 10. Checklist go-live

- [ ] HTTPS y HSTS.
- [ ] Secretos gestionados y rotados.
- [ ] Rate limiting en proxy y API.
- [ ] Backups cifrados y restauración probada.
- [ ] SMTP con dominio verificado.
- [ ] S3 privado y política de retención.
- [ ] Monitorización y alertas.
- [x] Integración ClamAV; activar y verificar en producción.
- [ ] Revisión legal y textos aprobados.
- [ ] Accesibilidad manual.
- [ ] Pentest sin críticos/altos abiertos.
- [ ] Soporte y formación del piloto.

---

*Fin de docs/DEPLOYMENT.md — v0.1.0.*
