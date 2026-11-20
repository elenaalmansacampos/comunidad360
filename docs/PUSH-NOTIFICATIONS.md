# COMUNIDAD 360 — Notificaciones push

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Implementado; requiere claves VAPID en el entorno |
| Fecha | 2026-11-20 |

---

## 1. Qué incluye

- Suscripción Web Push por dispositivo (Service Worker + VAPID).
- Preferencias por categoría y canal `PUSH` junto a `IN_APP` y `EMAIL`.
- Envío automático al convocar una reunión o publicar el acta.
- Limpieza de suscripciones caducadas (`404`/`410`).
- Registro en `notification_delivery` con canal `PUSH`.

El envío push necesita HTTPS (Render ya lo proporciona). En iOS, Safari solo entrega Web Push si
la web se ha añadido a la pantalla de inicio (iOS 16.4+).

## 2. Generar claves VAPID

Una sola vez. No cambies las claves si ya hay usuarios suscritos: dejarían de recibir avisos.

```bash
cd backend
mvn -q org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.mainClass=com.comunidad360.push.VapidKeys \
  -Dexec.classpathScope=runtime
```

Salida:

```text
APP_PUSH_ENABLED=true
APP_PUSH_PUBLIC_KEY=...
APP_PUSH_PRIVATE_KEY=...
APP_PUSH_SUBJECT=mailto:soporte@example.com
```

## 3. Configuración en Render

En `comunidad360-api` → **Environment**:

```text
APP_PUSH_ENABLED=true
APP_PUSH_PUBLIC_KEY=<clave pública>
APP_PUSH_PRIVATE_KEY=<clave privada>
APP_PUSH_SUBJECT=mailto:soporte@tudominio.com
```

Guarda y espera el reinicio. No subas la clave privada al repositorio.

Si `APP_PUSH_ENABLED=true` y faltan claves, el backend arranca igualmente pero desactiva push y el
frontend muestra "no disponible".

## 4. Uso

1. Inicia sesión y entra en **Preferencias de notificación**.
2. En la tarjeta **Notificaciones push**, pulsa **Activar en este dispositivo**.
3. Acepta el permiso del navegador.
4. Marca o desmarca la columna **Push** por categoría y guarda si quieres cambiar qué avisos llegan.

Para probar sin esperar a una reunión:

```bash
curl -X POST https://comunidad360-api-ihic.onrender.com/api/v1/me/push/test \
  -H "Authorization: Bearer <access-token>"
```

## 5. API

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/v1/me/push/public-key` | Clave pública y si push está activo |
| POST | `/api/v1/me/push/subscriptions` | Alta o actualización de suscripción |
| DELETE | `/api/v1/me/push/subscriptions?endpoint=...` | Baja de suscripción |
| POST | `/api/v1/me/push/test` | Envía una notificación de prueba al usuario |

## 6. Persistencia

Migración `V13__push_subscriptions.sql`:

- `push_subscription`: endpoint, claves `p256dh`/`auth`, usuario, user-agent, último éxito y fallos.
- `notification_delivery` ya admite el canal `PUSH`.

## 7. Limitaciones y seguridad

- Endpoints de push expiran o se revocan: se eliminan al recibir `404`/`410`.
- Si cambias las claves VAPID, todas las suscripciones existentes dejan de ser válidas.
- El límite de payload de Web Push es pequeño; la app recorta título (80) y cuerpo (240).
- El rate limiting global de la API también aplica a estos endpoints.
- La clave privada es un secreto: vive solo en el entorno del servidor.

---

*Fin de docs/PUSH-NOTIFICATIONS.md — v0.1.0.*
