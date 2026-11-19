# ADR-0003 — Autenticación con JWT y refresh tokens rotatorios

- **Estado**: Aceptado (pendiente de validación en FASE 1)
- **Fecha**: 2026-11-19
- **Decisores**: Security Lead, Backend Lead, Frontend Lead

## Contexto

La SPA React necesita autenticarse contra una API REST y un canal WebSocket. El registro es por
invitación. Se requiere cierre de sesión, revocación, recuperación de contraseña y protección frente a
robo de tokens.

## Decisión

- **Access token JWT** de corta duración (15 minutos propuesto), enviado en `Authorization: Bearer`.
- **Refresh token** opaco y rotatorio, almacenado en cookie `HttpOnly`, `Secure` y `SameSite` adecuado.
- El refresh token se guarda en base de datos como hash, con fecha de expiración, revocación y detección
  de reutilización.
- El access token se mantiene en memoria en el frontend; no se persiste en `localStorage`.
- Los permisos se resuelven en el backend por comunidad. El JWT no es la fuente de verdad de permisos.
- El cambio de contraseña y el cierre de todas las sesiones revocan los refresh tokens.
- WebSocket se autentica con el access token y valida la membresía al suscribirse.

## Alternativas consideradas

### Sesiones en servidor con cookie

- **Ventajas**: revocación inmediata, sin tokens en cliente.
- **Inconvenientes**: estado en servidor, CSRF, escalado y WebSocket.
- **Motivo de descarte**: JWT encaja mejor con SPA + WebSocket, manteniendo revocación por refresh.

### Access token de larga duración

- **Ventajas**: simplicidad.
- **Inconvenientes**: mayor ventana de robo, revocación difícil.
- **Motivo de descarte**: riesgo de seguridad inaceptable.

### JWT + refresh rotatorio

- **Ventajas**: buena experiencia, revocación efectiva, compatible con WebSocket.
- **Inconvenientes**: requiere gestión de cookies, CSRF y rotación.
- **Motivo de elección**: equilibrio entre seguridad y arquitectura SPA.

## Consecuencias

### Positivas

- Sesiones revocables sin estado en cada petición.
- Menor exposición del token de acceso.
- Compatible con múltiples dispositivos y cierre global.

### Negativas / riesgos

- Complejidad de rotación y detección de reutilización.
- Necesidad de configurar correctamente cookies, CORS y CSRF.
- El frontend debe gestionar el refresco sin bucles.

## Reglas de seguridad

- Firma con clave robusta y rotación documentada.
- Nunca registrar tokens en logs.
- Rate limiting en login y refresh.
- Cookies `Secure` y `HttpOnly`; `SameSite` según dominio final (Q-06).
- Verificación de email antes de operar.
- Bloqueo temporal tras intentos fallidos.
- Auditoría de login, refresh, logout y cambios de contraseña.
