# COMUNIDAD 360 — Despliegue en Render

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Blueprint listo; validar con cuenta Render |
| Fecha | 2026-11-19 |
| Servicios | PostgreSQL, API Docker, frontend estático |

---

## 1. Qué se despliega

El archivo [`../render.yaml`](../render.yaml) define:

- **comunidad360-db**: PostgreSQL gestionado.
- **comunidad360-api**: backend Spring Boot en Docker.
- **comunidad360-web**: frontend estático React/Vite con rewrite SPA.

URLs reales del despliegue actual:

- App: `https://comunidad360-web-q2ln.onrender.com`
- API: `https://comunidad360-api-ihic.onrender.com`
- Salud: `https://comunidad360-api-ihic.onrender.com/actuator/health`

> `comunidad360-web.onrender.com` y `comunidad360-api.onrender.com` ya estaban ocupados por
> otros servicios, así que Render asignó los sufijos `-q2ln` y `-ihic`. Usa siempre las URLs
> reales que muestre el dashboard, no las de `render.yaml` sin sufijo.

## 2. Requisitos

- Cuenta en Render.
- Repositorio `elenaalmansacampos/comunidad360` conectado.
- Opcional: bucket S3 compatible privado. Recomendado: Cloudflare R2.
- Opcional: SMTP transaccional. Recomendado: Resend.
- Email y contraseña para el primer administrador.

> El Blueprint arranca con `APP_STORAGE_TYPE=local` para no bloquear el primer despliegue.
> En el plan gratuito el disco es efímero: los documentos se pierden al reiniciar. Para
> persistencia, configura R2/S3 y cambia `APP_STORAGE_TYPE=s3`.

## 3. Crear el Blueprint

1. Render Dashboard → **New** → **Blueprint**.
2. Conecta el repositorio y selecciona la rama `main`.
3. Render detecta `render.yaml`.
4. Completa los valores `sync: false` obligatorios:

```text
APP_BOOTSTRAP_EMAIL=admin@tudominio.com
APP_BOOTSTRAP_PASSWORD=<mínimo 12 caracteres>
```

   Solo si vas a usar S3/R2, rellena además (y pon `APP_STORAGE_TYPE=s3`):

```text
APP_STORAGE_S3_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
APP_STORAGE_S3_PUBLIC_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
APP_STORAGE_S3_BUCKET=comunidad360-documents
APP_STORAGE_S3_ACCESS_KEY=<access-key>
APP_STORAGE_S3_SECRET_KEY=<secret-key>
```

   No dejes estos valores con el texto literal `${APP_STORAGE_S3_ENDPOINT}`:
   Render no interpola variables y el arranque fallará.

   Para activar notificaciones push, genera las claves VAPID y configura
   `APP_PUSH_ENABLED=true`, `APP_PUSH_PUBLIC_KEY`, `APP_PUSH_PRIVATE_KEY` y `APP_PUSH_SUBJECT`.
   Ver [`PUSH-NOTIFICATIONS.md`](./PUSH-NOTIFICATIONS.md).

5. Aplica el Blueprint.

## 4. Cloudflare R2

En Cloudflare R2:

1. Crea un bucket privado `comunidad360-documents`.
2. Crea un token de API con permiso de lectura/escritura sobre el bucket.
3. Usa el endpoint S3 de la cuenta como `APP_STORAGE_S3_ENDPOINT` y
   `APP_STORAGE_S3_PUBLIC_ENDPOINT`.
4. Mantén `APP_STORAGE_S3_PATH_STYLE=true` y `APP_STORAGE_S3_REGION=auto`.
5. No hagas público el bucket; la app usa URLs firmadas.

## 5. Email

El Blueprint arranca con email desactivado (`APP_MAIL_ENABLED=false`) para no bloquear el primer
despliegue. Para activarlo con Resend:

```text
APP_MAIL_ENABLED=true
SPRING_MAIL_HOST=smtp.resend.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=resend
SPRING_MAIL_PASSWORD=<api-key>
APP_MAIL_FROM=no-reply@tudominio-verificado.com
```

El dominio del remitente debe estar verificado en el proveedor.

## 6. Primer acceso

El bootstrap crea el primer usuario y una comunidad si la base está vacía:

- Email: `APP_BOOTSTRAP_EMAIL`.
- Contraseña: `APP_BOOTSTRAP_PASSWORD`.
- Rol: `COMMUNITY_ADMIN`.
- Comunidad: `Mi Comunidad` por defecto.

Después del primer login:

1. Cambia la contraseña si el producto lo permite o crea otro administrador.
2. Pon `APP_BOOTSTRAP_ENABLED=false`.
3. Redeploy.

El bootstrap no registra la contraseña en logs y no crea datos demo.

### Reset del administrador

Si el admin ya existe pero no recuerdas la contraseña (el bootstrap no la actualiza por sí solo),
activa temporalmente el reset controlado:

```text
APP_BOOTSTRAP_ENABLED=true
APP_BOOTSTRAP_RESET_PASSWORD=true
APP_BOOTSTRAP_EMAIL=<email del admin>
APP_BOOTSTRAP_PASSWORD=<nueva contraseña, mínimo 12 caracteres>
```

Al reiniciar, el runner actualiza la contraseña del usuario con ese email, lo desbloquea, lo marca
como activo/verificado y le asegura el rol `COMMUNITY_ADMIN`. En los logs verás
`Bootstrap admin password reset for ...`. Si el email no existe, crea uno nuevo. Después vuelve a
poner `APP_BOOTSTRAP_RESET_PASSWORD=false` (y `APP_BOOTSTRAP_ENABLED=false`) y redeploy.

## 7. Nombres de servicio

Si Render asigna nombres distintos a `comunidad360-api` o `comunidad360-web`:

- Actualiza `APP_CORS_ALLOWED_ORIGINS` con la URL real del frontend.
- Actualiza `VITE_API_BASE_URL` en el servicio estático con la URL real de la API.
- Redeploy del frontend.

## 8. Comprobaciones

```bash
curl -fsS https://comunidad360-api-ihic.onrender.com/actuator/health
```

En navegador:

- Abre `https://comunidad360-web-q2ln.onrender.com`.
- Login con las credenciales de bootstrap.
- Crea un edificio y una unidad.
- Sube un PDF a Documentos.
- Comprueba que la descarga funciona.
- Crea una incidencia y una reunión.

## 9. Limitaciones del plan gratuito

- Los web services gratuitos se suspenden por inactividad y tardan en despertar.
- PostgreSQL gratuito puede tener límites de retención/almacenamiento.
- El almacenamiento local (`APP_STORAGE_TYPE=local`) es efímero en el plan gratuito.
- Para piloto real, usa planes de pago, backups y monitorización.
- Render no ejecuta ClamAV en este Blueprint; `APP_MALWARE_SCAN_ENABLED=false`.
- El frontend de Render no es la demo de GitHub Pages.
- La demo de Pages sigue disponible en `https://elenaalmansacampos.github.io/comunidad360/`.

## 10. Troubleshooting

| Síntoma | Revisar |
| --- | --- |
| API no arranca | Variables S3 obligatorias, `SPRING_DATASOURCE_*`, logs de Render |
| `Illegal character in path ... ${APP_STORAGE_S3_ENDPOINT}` | El valor literal de la env var; pon un endpoint real o usa `APP_STORAGE_TYPE=local` |
| `UnknownHostException: ${DB_HOST}` | Borra `SPRING_DATASOURCE_URL` del dashboard; se compone desde `DB_HOST`/`DB_PORT`/`DB_NAME` |
| 401/403 en frontend | `VITE_API_BASE_URL`, CORS y URL real del frontend |
| Error de base de datos | Host/puerto/usuario de Render y migraciones Flyway |
| Documentos fallan | Endpoint, bucket, claves y path-style de R2 |
| Emails no llegan | `APP_MAIL_ENABLED`, SMTP, remitente verificado |
| Cold start lento | Plan gratuito; considerar plan de pago |

---

*Fin de docs/RENDER-DEPLOY.md — v0.1.0.*
