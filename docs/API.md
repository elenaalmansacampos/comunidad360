# COMUNIDAD 360 — Contratos de API

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Convenciones y catálogo inicial (FASE 0) |
| Fecha | 2026-11-19 |
| Base URL | `/api/v1` |
| Formato | JSON UTF-8 |
| Autenticación | JWT Bearer + refresh token en cookie |

---

> **Estado FASE 3.9:** implementados autenticación, `/me`, comunidades, miembros, invitaciones,
> estructura, titularidad, inquilinos, finanzas núcleo, banca, incidencias, documentos,
> reuniones, notificaciones, dashboard, proveedores, chat y votaciones consultivas.
> El resto del catálogo es contrato objetivo para fases posteriores.

## 1. Principios

1. API REST versionada y documentada con OpenAPI.
2. El backend es la única autoridad de autorización.
3. Todo recurso de negocio se resuelve dentro de una comunidad autorizada.
4. Errores homogéneos y traducibles mediante claves.
5. Paginación obligatoria en listados.
6. Idempotencia en operaciones financieras.
7. Fechas en ISO 8601 con zona horaria; importes como string decimal.
8. No se exponen entidades JPA; solo DTOs.

## 2. Versionado y rutas

- Prefijo: `/api/v1`.
- Rutas de plataforma: `/api/v1/auth`, `/api/v1/me`, `/api/v1/communities`.
- Rutas de tenant: `/api/v1/communities/{communityId}/...`.
- Cambios incompatibles requieren `/api/v2` o un proceso de deprecación documentado.
- Los identificadores son UUID en formato canónico.

## 3. Autenticación

### Cabeceras

```http
Authorization: Bearer <access_token>
Content-Type: application/json
Accept-Language: es | ca
X-Request-Id: <uuid opcional>
```

- `Accept-Language` se usa para mensajes de error y contenido localizado cuando aplique.
- El idioma persistente del usuario prevalece para notificaciones y emails.
- El access token dura 15 minutos (propuesto).
- El refresh token viaja en cookie `HttpOnly`; no se expone en el cuerpo de la respuesta.

### Flujo

1. `POST /auth/login` devuelve access token y establece cookie de refresh.
2. El cliente usa `Authorization: Bearer`.
3. Ante `401`, intenta `POST /auth/refresh` una vez.
4. Si el refresco falla, limpia la sesión y redirige a login.

## 4. Contexto de comunidad

- El `communityId` de la ruta se valida contra una membresía activa.
- Nunca se confía únicamente en el identificador enviado.
- Si el usuario no pertenece a la comunidad, se responde `403` o `404` sin revelar datos.
- `SUPER_ADMIN` accede mediante rutas de soporte auditadas; no se mezcla con el flujo normal.
- El cambio de comunidad en el frontend invalida la caché de consultas.

## 5. Formato de datos

| Tipo | Representación | Ejemplo |
| --- | --- | --- |
| UUID | string | `"8f14e45f-ea0d-4b8b-9c1a-2f7d6a1b3c4d"` |
| Fecha | `YYYY-MM-DD` | `"2026-11-19"` |
| Fecha y hora | ISO 8601 con offset | `"2026-11-19T18:30:00+01:00"` |
| Importe | string decimal | `"1234.56"` |
| Coeficiente | string decimal | `"12.345678"` |
| Idioma | ISO 639-1 | `"es"`, `"ca"` |
| Moneda | ISO 4217 | `"EUR"` |
| Booleano | boolean | `true` |

Reglas:

- Los importes se envían y reciben como string para evitar pérdida de precisión en JavaScript.
- No se usan números flotantes para dinero.
- Los enums se representan en `UPPER_SNAKE_CASE`.
- Los campos JSON usan `camelCase`.

## 6. Errores

Formato basado en RFC 7807:

```json
{
  "type": "https://api.comunidad360.example/errors/tenant-forbidden",
  "title": "Access denied",
  "status": 403,
  "code": "TENANT_FORBIDDEN",
  "messageKey": "errors.tenant.forbidden",
  "params": {},
  "traceId": "4f1c2a7e9b3d"
}
```

- `messageKey` permite traducir en frontend o backend.
- `title` es un texto técnico estable; la UI usa `messageKey`.
- No se incluyen trazas internas, SQL ni nombres de tablas.
- `traceId` permite correlacionar con logs.

### Códigos HTTP

| Código | Uso |
| --- | --- |
| `200` | Lectura o actualización correcta |
| `201` | Recurso creado |
| `204` | Operación sin contenido |
| `400` | Validación de entrada |
| `401` | No autenticado o token inválido |
| `403` | Autenticado sin permiso o fuera de tenant |
| `404` | Recurso inexistente o no visible |
| `409` | Conflicto, versión desactualizada o duplicado |
| `422` | Regla de negocio incumplida |
| `429` | Límite de peticiones |
| `500` | Error interno |

### Códigos de negocio iniciales

```text
AUTH_INVALID_CREDENTIALS
AUTH_EMAIL_NOT_VERIFIED
AUTH_ACCOUNT_LOCKED
AUTH_TOKEN_EXPIRED
AUTH_REFRESH_REUSED
INVITATION_EXPIRED
INVITATION_ALREADY_USED
TENANT_FORBIDDEN
PERMISSION_DENIED
VALIDATION_ERROR
RESOURCE_NOT_FOUND
VERSION_CONFLICT
DUPLICATE_RESOURCE
COEFFICIENT_SUM_INVALID
OWNERSHIP_SUM_INVALID
FINANCIAL_PERIOD_CLOSED
ASSESSMENT_ALREADY_ISSUED
PAYMENT_EXCEEDS_CHARGE
RECONCILIATION_CONFLICT
DOCUMENT_TYPE_UNSUPPORTED
DOCUMENT_TOO_LARGE
DOCUMENT_EMPTY
DOCUMENT_SCAN_PENDING
DOCUMENT_INFECTED
FILE_TOO_LARGE
UNSUPPORTED_FILE_TYPE
VOTING_NOT_OPEN
VOTING_ALREADY_CAST
```

## 7. Paginación, filtros y ordenación

### Listados generales

```http
GET /api/v1/communities/{cid}/incidents?page=0&size=20&sort=createdAt,desc&status=OPEN
```

Respuesta:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Reglas:

- `page` empieza en 0.
- `size` máximo 100; por defecto 20.
- `sort` usa campos permitidos y `asc|desc`.
- Los filtros se documentan por endpoint.
- Los listados grandes usan cursor.

### Cursor (chat y auditoría)

```http
GET /api/v1/communities/{cid}/chat/channels/{channelId}/messages?before=<messageId>&limit=50
```

Respuesta:

```json
{
  "items": [],
  "nextCursor": "uuid-o-null"
}
```

## 8. Idempotencia y concurrencia

### Idempotencia

Operaciones financieras (`POST` de cargos, pagos, conciliaciones y apuntes) aceptan:

```http
Idempotency-Key: <uuid>
```

- La clave se asocia a usuario, endpoint y payload.
- Reintentos con la misma clave devuelven el mismo resultado.
- Ventana de retención propuesta: 24 horas.
- Una clave reutilizada con payload distinto devuelve `409`.

### Concurrencia optimista

- Los recursos editables incluyen `version`.
- Las actualizaciones envían `If-Match: <version>` o el campo `version` en el cuerpo.
- Si la versión no coincide: `409 VERSION_CONFLICT`.

## 9. Límites y archivos

- Límite general de petición: configurable (propuesto 1 MB para JSON).
- Subida de documentos: multipart `multipart/form-data`, tamaño máximo a validar (Q-13).
- Tipos permitidos: PDF, imágenes, ofimática y texto según categoría; lista blanca.
- Validación de MIME real, extensión y hash.
- Análisis antivirus antes de publicar.
- Descarga mediante URL firmada de corta duración.
- Nombre de archivo saneado en `Content-Disposition`.
- No se permite ejecución de contenido subido.

## 10. Catálogo inicial de endpoints

> El catálogo es una propuesta funcional. Los contratos exactos se fijarán en OpenAPI durante FASE 1.
> `cid` = `communityId`, `uid` = `unitId`.

### 10.1. Autenticación y usuario

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| POST | `/auth/login` | Público | Inicio de sesión |
| POST | `/auth/refresh` | Cookie | Renovar access token |
| POST | `/auth/logout` | Autenticado | Cerrar sesión actual |
| POST | `/auth/logout-all` | Autenticado | Cerrar todas las sesiones |
| POST | `/auth/password/forgot` | Público | Solicitar recuperación |
| POST | `/auth/password/reset` | Público + token | Restablecer contraseña |
| POST | `/auth/email/verify` | Público + token | Verificar email |
| POST | `/auth/invitations/accept` | Público + token | Aceptar invitación |
| GET | `/me` | Autenticado | Perfil y membresías |
| PATCH | `/me` | Autenticado | Editar perfil |
| PUT | `/me/language` | Autenticado | Cambiar idioma preferido |

### 10.2. Comunidades

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | `/communities` | Autenticado | Comunidades del usuario |
| POST | `/communities` | `SUPER_ADMIN` | Crear comunidad |
| GET | `/communities/{cid}` | `COMMUNITY_VIEW` | Detalle |
| PATCH | `/communities/{cid}` | `COMMUNITY_MANAGE` | Configuración |
| GET | `/communities/{cid}/dashboard` | Membresía | Resumen por rol, finanzas, incidencias, reuniones y evolución |
| GET | `/communities/{cid}/reports/summary` | `REPORT_VIEW` | Informe PDF con auditoría |

### 10.3. Edificios y unidades

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | `/communities/{cid}/buildings` | `UNIT_VIEW` | Listar edificios |
| POST | `/communities/{cid}/buildings` | `UNIT_MANAGE` | Crear edificio |
| GET | `/communities/{cid}/buildings/{id}` | `UNIT_VIEW` | Detalle |
| PATCH | `/communities/{cid}/buildings/{id}` | `UNIT_MANAGE` | Editar |
| DELETE | `/communities/{cid}/buildings/{id}` | `UNIT_MANAGE` | Archivar (sin unidades activas) |
| GET | `/communities/{cid}/units` | `UNIT_VIEW` | Listar unidades |
| POST | `/communities/{cid}/units` | `UNIT_MANAGE` | Crear unidad |
| GET | `/communities/{cid}/units/{uid}` | `UNIT_VIEW` | Detalle |
| PATCH | `/communities/{cid}/units/{uid}` | `UNIT_MANAGE` | Editar |
| DELETE | `/communities/{cid}/units/{uid}` | `UNIT_MANAGE` | Archivar |
| GET | `/communities/{cid}/common-elements` | `UNIT_VIEW` | Listar elementos comunes |
| POST | `/communities/{cid}/common-elements` | `UNIT_MANAGE` | Crear elemento |
| GET | `/communities/{cid}/common-elements/{id}` | `UNIT_VIEW` | Detalle |
| PATCH | `/communities/{cid}/common-elements/{id}` | `UNIT_MANAGE` | Editar |
| DELETE | `/communities/{cid}/common-elements/{id}` | `UNIT_MANAGE` | Archivar |
| GET | `/communities/{cid}/structure/summary` | `UNIT_VIEW` | Resumen y validación de coeficientes |

### 10.4. Membresías, propietarios e inquilinos

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | `/communities/{cid}/members` | `MEMBER_VIEW` | Listar membresías |
| PATCH | `/communities/{cid}/members/{id}` | `MEMBER_MANAGE` | Estado y roles |
| POST | `/communities/{cid}/invitations` | `MEMBER_INVITE` | Invitar |
| GET | `/communities/{cid}/invitations` | `MEMBER_VIEW` | Listar invitaciones |
| DELETE | `/communities/{cid}/invitations/{id}` | `MEMBER_MANAGE` | Revocar invitación |
| GET | `/communities/{cid}/units/{uid}/ownerships` | `OWNERSHIP_MANAGE` o `UNIT_VIEW` propio | Titularidad |
| POST | `/communities/{cid}/units/{uid}/ownerships` | `OWNERSHIP_MANAGE` | Alta de titularidad |
| PATCH | `/communities/{cid}/units/{uid}/ownerships/{id}` | `OWNERSHIP_MANAGE` | Editar titularidad |
| DELETE | `/communities/{cid}/units/{uid}/ownerships/{id}` | `OWNERSHIP_MANAGE` | Finalizar titularidad |
| GET | `/communities/{cid}/units/{uid}/tenancies` | `OWNERSHIP_MANAGE` o propio | Alquileres |
| POST | `/communities/{cid}/units/{uid}/tenancies` | `OWNERSHIP_MANAGE` | Autorizar inquilino |
| PATCH | `/communities/{cid}/units/{uid}/tenancies/{id}` | `OWNERSHIP_MANAGE` | Editar alquiler |
| DELETE | `/communities/{cid}/units/{uid}/tenancies/{id}` | `OWNERSHIP_MANAGE` | Revocar alquiler |

### 10.5. Finanzas

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | `/communities/{cid}/finance/summary` | `FINANCE_VIEW` | Saldo y cuotas |
| GET | `/communities/{cid}/finance/fiscal-years` | `FINANCE_VIEW` | Ejercicios |
| POST | `/communities/{cid}/finance/fiscal-years` | `FINANCE_MANAGE` | Abrir ejercicio |
| POST | `/communities/{cid}/finance/fiscal-years/{id}/close` | `FINANCE_MANAGE` | Cerrar ejercicio |
| GET | `/communities/{cid}/finance/budgets` | `FINANCE_VIEW` | Presupuestos |
| POST | `/communities/{cid}/finance/budgets` | `FINANCE_MANAGE` | Crear presupuesto |
| POST | `/communities/{cid}/finance/budgets/{id}/items` | `FINANCE_MANAGE` | Añadir partida |
| GET | `/communities/{cid}/finance/assessments` | `FINANCE_VIEW` | Cuotas y derramas |
| POST | `/communities/{cid}/finance/assessments` | `FINANCE_MANAGE` | Crear cuota/derrama |
| POST | `/communities/{cid}/finance/assessments/{id}/issue` | `FINANCE_MANAGE` | Emitir cargos |
| GET | `/communities/{cid}/finance/charges` | `FINANCE_VIEW` o propias | Cargos |
| GET | `/communities/{cid}/finance/invoices` | `FINANCE_VIEW` | Facturas |
| POST | `/communities/{cid}/finance/invoices` | `INVOICE_MANAGE` | Registrar factura |
| GET | `/communities/{cid}/finance/payments` | `FINANCE_VIEW` | Pagos |
| POST | `/communities/{cid}/finance/payments` | `FINANCE_MANAGE` | Registrar pago |
| GET | `/communities/{cid}/finance/ledger-entries` | `FINANCE_VIEW` | Apuntes |
| POST | `/communities/{cid}/finance/ledger-entries` | `FINANCE_MANAGE` | Crear apunte |
| POST | `/communities/{cid}/finance/ledger-entries/{id}/post` | `FINANCE_MANAGE` | Contabilizar |
| POST | `/communities/{cid}/finance/ledger-entries/{id}/reverse` | `FINANCE_MANAGE` | Reverso |
| GET | `/communities/{cid}/reports/summary` | `REPORT_VIEW` | Informe PDF de comunidad |

### 10.6. Banca

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | `/communities/{cid}/banking/accounts` | `BANK_ACCOUNT_VIEW` | Cuentas |
| POST | `/communities/{cid}/banking/accounts` | `FINANCE_MANAGE` | Registrar cuenta |
| POST | `/communities/{cid}/banking/imports` | `BANK_IMPORT` | Importar CSV |
| GET | `/communities/{cid}/banking/imports` | `BANK_IMPORT` | Histórico de importaciones |
| GET | `/communities/{cid}/banking/transactions` | `BANK_ACCOUNT_VIEW` | Movimientos |
| POST | `/communities/{cid}/banking/transactions/{id}/reconcile` | `BANK_RECONCILE` | Conciliar |
| DELETE | `/communities/{cid}/banking/reconciliations/{id}` | `BANK_RECONCILE` | Deshacer conciliación |

### 10.7. Incidencias

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | `/communities/{cid}/incidents` | `INCIDENT_VIEW_ALL` o propias | Listar |
| POST | `/communities/{cid}/incidents` | `INCIDENT_CREATE` | Crear |
| GET | `/communities/{cid}/incidents/{id}` | Permiso según recurso | Detalle |
| PATCH | `/communities/{cid}/incidents/{id}` | `INCIDENT_MANAGE` | Editar estado, prioridad o asignación |
| POST | `/communities/{cid}/incidents/{id}/comments` | Acceso al recurso | Comentar |
| POST | `/communities/{cid}/incidents/{id}/attachments` | Acceso al recurso | Adjuntar |

### 10.8. Documentos

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | `/communities/{cid}/documents?category=ACTA` | `DOCUMENT_VIEW` | Listar visibles |
| POST | `/communities/{cid}/documents` | `DOCUMENT_UPLOAD` o `DOCUMENT_MANAGE` | Crear con `multipart/form-data` |
| GET | `/communities/{cid}/documents/{id}` | `DOCUMENT_VIEW` + visibilidad | Detalle con versiones |
| PATCH | `/communities/{cid}/documents/{id}` | `DOCUMENT_MANAGE` | Metadatos/visibilidad/estado |
| DELETE | `/communities/{cid}/documents/{id}` | `DOCUMENT_MANAGE` | Archivar |
| POST | `/communities/{cid}/documents/{id}/versions` | `DOCUMENT_UPLOAD` | Nueva versión |
| GET | `/communities/{cid}/documents/{id}/download?versionId=` | `DOCUMENT_VIEW` + visibilidad | Descarga autenticada |

Las respuestas documentales incluyen `downloadUrl`: URL prefirmada de corta duración con S3 o ruta
autenticada del backend con almacenamiento local. El detalle incluye todas las versiones y sus URLs.


### 10.9. Chat

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | `/communities/{cid}/chat/channels` | `CHAT_VIEW` | Canales accesibles |
| POST | `/communities/{cid}/chat/channels` | `CHAT_MODERATE` | Crear canal temático/privado |
| GET | `/communities/{cid}/chat/channels/{id}/messages` | `CHAT_VIEW` + acceso | Historial (máx. 100) |
| POST | `/communities/{cid}/chat/channels/{id}/messages` | `CHAT_VIEW` + acceso | Enviar mensaje |

La actualización del chat es por polling; WebSocket, adjuntos y moderación son objetivo posterior.

### 10.10. Reuniones y votaciones

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | `/communities/{cid}/meetings` | `MEETING_VIEW` | Listar |
| POST | `/communities/{cid}/meetings` | `MEETING_MANAGE` | Convocar |
| GET | `/communities/{cid}/meetings/{id}` | `MEETING_VIEW` | Detalle |
| PATCH | `/communities/{cid}/meetings/{id}` | `MEETING_MANAGE` | Editar |
| POST | `/communities/{cid}/meetings/{id}/agenda` | `MEETING_MANAGE` | Añadir punto |
| POST | `/communities/{cid}/meetings/{id}/attendance` | `MEETING_MANAGE` | Registrar asistencia |
| POST | `/communities/{cid}/meetings/{id}/agreements` | `MEETING_MANAGE` | Registrar acuerdo |
| POST | `/communities/{cid}/meetings/{id}/minutes` | `MEETING_MANAGE` | Vincular acta |
| GET | `/communities/{cid}/votings` | `VOTE_VIEW` | Listar |
| POST | `/communities/{cid}/votings` | `VOTE_MANAGE` | Crear |
| GET | `/communities/{cid}/votings/{id}` | `VOTE_VIEW` | Detalle y resultados |
| POST | `/communities/{cid}/votings/{id}/open` | `VOTE_MANAGE` | Abrir |
| POST | `/communities/{cid}/votings/{id}/close` | `VOTE_MANAGE` | Cerrar |
| POST | `/communities/{cid}/votings/{id}/votes` | `VOTE_CAST` | Votar |
| GET | `/communities/{cid}/votings/{id}/results` | `VOTE_VIEW` | Resultados |

### 10.11. Proveedores, notificaciones y auditoría

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | `/communities/{cid}/providers` | `PROVIDER_VIEW` | Listar |
| POST | `/communities/{cid}/providers` | `PROVIDER_MANAGE` | Crear |
| GET | `/communities/{cid}/providers/{id}` | `PROVIDER_VIEW` | Detalle |
| PATCH | `/communities/{cid}/providers/{id}` | `PROVIDER_MANAGE` | Editar |
| GET | `/communities/{cid}/providers/{id}/contacts` | `PROVIDER_VIEW` | Contactos |
| POST | `/communities/{cid}/providers/{id}/contacts` | `PROVIDER_MANAGE` | Añadir contacto |
| GET | `/notifications?unreadOnly=` | Autenticado | Notificaciones propias |
| GET | `/notifications/unread-count` | Autenticado | Contador de no leídas |
| PATCH | `/notifications/{id}/read` | Autenticado | Marcar leída |
| POST | `/notifications/read-all` | Autenticado | Marcar todas |
| GET | `/me/notification-preferences` | Autenticado | Preferencias |
| PUT | `/me/notification-preferences` | Autenticado | Actualizar |
| GET | `/communities/{cid}/audit` | `AUDIT_VIEW` | Auditoría |

Reglas de reuniones:

- Estados: `DRAFT`, `CONVENED`, `HELD`, `CANCELLED`.
- `DRAFT → CONVENED|CANCELLED`; `CONVENED → HELD|CANCELLED`.
- Pasar a `CONVENED` genera notificaciones internas y email.
- La asistencia es única por reunión y membresía.
- El coeficiente se calcula desde la titularidad activa.
- Vincular acta pasa la reunión a `HELD` y notifica a los miembros.

## 11. WebSocket

### Conexión

```text
wss://<host>/ws
```

- Autenticación con access token.
- STOMP sobre WebSocket.
- Validación de membresía y permiso al suscribirse.

### Destinos

```text
SUBSCRIBE /topic/communities/{cid}/channels/{channelId}
SEND      /app/communities/{cid}/channels/{channelId}/messages
SUBSCRIBE /user/queue/errors
SUBSCRIBE /user/queue/notifications
```

### Eventos

```json
{
  "type": "MESSAGE_CREATED",
  "channelId": "uuid",
  "message": {
    "id": "uuid",
    "senderId": "uuid",
    "body": "texto",
    "createdAt": "2026-11-19T18:30:00+01:00"
  }
}
```

Tipos iniciales: `MESSAGE_CREATED`, `MESSAGE_UPDATED`, `MESSAGE_DELETED`, `MODERATION_APPLIED`.

Reglas:

- El cliente no decide el remitente ni la comunidad.
- El servidor valida pertenencia al canal.
- Mensajes y adjuntos tienen límites.
- Reconexión con recuperación de mensajes por cursor.
- Errores de autorización se envían a `/user/queue/errors` sin detalles internos.

## 12. OpenAPI

- Documento generado con Springdoc y revisado en cada PR.
- `operationId` estable.
- Ejemplos de request/response.
- Esquemas de error reutilizables.
- Seguridad declarada por endpoint.
- El contrato se valida en CI y se versiona en `docs/API.md` o en un artefacto enlazado.

## 13. Decisiones pendientes

| ID | Decisión | Impacto |
| --- | --- | --- |
| Q-06 | Dominio único, subdominios o ambos | Cookies, CORS, rutas |
| Q-13 | Tamaño máximo y tipos de archivo | Contratos de documentos |
| — | Paginación por cursor vs página en cada listado | Contratos |
| — | Formato exacto de importación CSV | Banca |
| — | Uso de `If-Match` o `version` en cuerpo | Concurrencia |
| — | Contrato definitivo de votaciones | Módulo voting |

---

*Fin de docs/API.md — v0.1.0 (FASE 0).*
