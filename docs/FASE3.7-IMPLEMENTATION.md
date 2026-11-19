# COMUNIDAD 360 — FASE 3.7: Reuniones y notificaciones

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Implementada; validación funcional completada |
| Fecha | 2026-11-19 |
| Alcance | Reuniones, orden del día, asistencia, acuerdos, acta, notificaciones y email |

---

## 1. Resumen

Se ha implementado el séptimo incremento vertical de FASE 3:

- Convocatorias ordinarias y extraordinarias.
- Orden del día ordenado.
- Registro de asistencia presencial, online, representada o ausente.
- Coeficiente calculado a partir de la titularidad activa.
- Acuerdos con votos y resultado.
- Vinculación del acta a un documento de la comunidad.
- Notificaciones internas con campana y contador.
- Preferencias por categoría y canal.
- Email transaccional mediante SMTP/Mailpit.
- Idioma de la notificación y del email según el destinatario.

## 2. Backend

### 2.1. Migración

`backend/src/main/resources/db/migration/V9__meetings_notifications.sql` crea:

- `meeting`
- `meeting_agenda_item`
- `meeting_attendance`
- `meeting_agreement`
- `notification`
- `notification_preference`
- `notification_delivery`

Incluye FKs compuestas por comunidad, unicidad de asistencia, estados, preferencias y entregas.

### 2.2. Endpoints de reuniones

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | `/communities/{cid}/meetings` | `MEETING_VIEW` | Listar |
| POST | `/communities/{cid}/meetings` | `MEETING_MANAGE` | Crear convocatoria |
| GET | `/communities/{cid}/meetings/{id}` | `MEETING_VIEW` | Detalle con agenda, asistencia y acuerdos |
| PATCH | `/communities/{cid}/meetings/{id}` | `MEETING_MANAGE` | Editar o cambiar estado |
| POST | `/communities/{cid}/meetings/{id}/agenda` | `MEETING_MANAGE` | Añadir punto |
| POST | `/communities/{cid}/meetings/{id}/attendance` | `MEETING_MANAGE` | Registrar asistencia |
| POST | `/communities/{cid}/meetings/{id}/agreements` | `MEETING_MANAGE` | Registrar acuerdo |
| POST | `/communities/{cid}/meetings/{id}/minutes` | `MEETING_MANAGE` | Vincular acta |

### 2.3. Estados

```text
DRAFT → CONVENED | CANCELLED
CONVENED → HELD | CANCELLED
HELD → (sin transiciones)
CANCELLED → (sin transiciones)
```

- Crear una reunión en `CONVENED` envía notificaciones.
- Pasar de `DRAFT` a `CONVENED` envía notificaciones.
- Vincular acta pasa de `CONVENED` a `HELD` y notifica el acta.
- La asistencia exige una reunión `CONVENED` o `HELD`.
- El representante debe ser otro miembro activo de la comunidad.
- La asistencia es única por reunión y membresía; volver a registrarla actualiza el registro.

### 2.4. Coeficiente de asistencia

El coeficiente se calcula sumando, para las titularidades activas del usuario:

```text
coeficiente_unidad × porcentaje_titularidad / 100
```

Se guarda una instantánea con seis decimales. Los inquilinos sin titularidad quedan con coeficiente `0`.

### 2.5. Acuerdos

- Votos a favor, en contra y abstenciones.
- Coeficientes opcionales; por defecto `0`.
- `adopted` se calcula como `votesFor > votesAgainst` si no se indica.
- El acuerdo puede vincularse a un punto del orden del día.

### 2.6. Endpoints de notificaciones

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/notifications?unreadOnly=` | Notificaciones propias |
| GET | `/notifications/unread-count` | Contador de no leídas |
| PATCH | `/notifications/{id}/read` | Marcar una como leída |
| POST | `/notifications/read-all` | Marcar todas como leídas |
| GET | `/me/notification-preferences` | Preferencias globales |
| PUT | `/me/notification-preferences` | Actualizar preferencias |

Categorías: `MEETINGS`, `INCIDENTS`, `FINANCE`, `SYSTEM`.
Canales: `IN_APP`, `EMAIL`.

### 2.7. Email

- `spring-boot-starter-mail` con SMTP configurable.
- En desarrollo se usa Mailpit (`localhost:1025`, UI `localhost:8025`).
- `APP_MAIL_ENABLED` activa el envío.
- Asunto y cuerpo se traducen con `MessageSource` en `es` o `ca`.
- El remitente se configura con `APP_MAIL_FROM` y `APP_MAIL_FROM_NAME`.
- Cada intento se registra en `notification_delivery`.
- Un fallo de email no revierte la notificación interna.

## 3. Frontend

- Nueva ruta `/communities/:communityId/meetings`.
- Nueva ruta `/communities/:communityId/meetings/:meetingId`.
- Nueva ruta `/communities/:communityId/notification-preferences`.
- Enlace `Reuniones` en sidebar y tarjeta del dashboard.
- Campana de notificaciones en la cabecera con contador y listado.
- Marcar notificación como leída al abrirla.
- Marcar todas como leídas.
- Preferencias por categoría y canal.
- Creación de reuniones.
- Detalle con estados, agenda, asistencia, acuerdos y acta.
- Formularios de agenda, asistencia, acuerdo y vinculación de acta.
- Textos en español y catalán.

## 4. Pruebas ejecutadas

| Validación | Resultado |
| --- | --- |
| Backend `mvn test` | 43 tests, 0 fallos |
| `MeetingIntegrationTest` | 4 tests, 0 fallos |
| `NotificationIntegrationTest` | 2 tests, 0 fallos |
| Creación, agenda y permisos de reunión | Correcto |
| Transición a convocada y notificación | Correcto |
| Asistencia con coeficiente | Correcto |
| Representación y autolegitimación bloqueada | Correcto (`422`) |
| Acuerdos y acta | Correcto |
| Aislamiento entre comunidades | Correcto (`403`) |
| Preferencias y lectura de notificaciones | Correcto |
| Notificaciones privadas por usuario | Correcto |
| Frontend `npm test` | 23 tests, 0 fallos |
| Frontend `npm run typecheck` | Correcto |
| Frontend `npm run build` | Correcto |
| Smoke test con Mailpit | 2 emails recibidos: convocatoria y acta |
| Rutas frontend | Reuniones, detalle y preferencias correctas |

## 5. Limitaciones conocidas

- El email se envía de forma síncrona y best-effort; no hay outbox ni reintentos automáticos.
- No hay notificaciones push.
- Las preferencias son globales por usuario, no por comunidad.
- No hay plantillas HTML ni gestión de plantillas.
- No hay votaciones formales ni cálculo de quórum/mayorías legales.
- No hay acta PDF generada; se vincula un documento existente.
- No hay edición visual de todos los metadatos de la reunión desde el detalle.
- No hay registro masivo de asistencia ni importación.
- No hay paginación en reuniones o notificaciones.
- No hay visor de entregas de email en frontend.
- La sección de reuniones/notificaciones del OpenAPI v0 no está sincronizada por completo.
- No hay pruebas E2E.

## 6. Trazabilidad

| Historia | Estado |
| --- | --- |
| US-090 Convocatoria y orden del día | Hecho |
| US-091 Asistencia y representación | Hecho |
| US-092 Acuerdos | Hecho |
| US-093 Acta y notificación | Hecho |
| US-100 Notificaciones internas | Hecho |
| US-101 Notificaciones por email | Hecho, sin reintentos |
| US-102 Preferencias de notificación | Hecho |
| US-103 Idioma del destinatario | Hecho |

## 7. Próximos pasos

1. Implementar FASE 3.8: dashboard e informes.
2. Añadir outbox y reintentos de email.
3. Incorporar votaciones formales y quórum.
4. Generar acta PDF y registro masivo de asistencia.
5. Sincronizar OpenAPI y añadir pruebas E2E.

---

*Fin de docs/FASE3.7-IMPLEMENTATION.md — v0.1.0.*
