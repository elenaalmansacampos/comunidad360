# COMUNIDAD 360 — FASE 3.9: MVP+ (proveedores, chat y votaciones)

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Implementada la base MVP+; push no validado |
| Fecha | 2026-11-19 |
| Alcance | Proveedores, chat persistente y votaciones consultivas |

---

## 1. Resumen

Se ha implementado la base del MVP+ condicionado:

- Directorio de proveedores y contactos.
- Chat con canal general, canales temáticos y privados.
- Mensajes persistidos con actualización periódica.
- Votaciones consultivas con opciones, elegibilidad, coeficiente, voto único y resultados.
- Aviso legal de votación no vinculante.
- Permisos, auditoría y aislamiento multi-tenant.

Push notifications no se implementa en esta fase al no existir validación expresa del canal ni
proveedor.

## 2. Backend

### 2.1. Migraciones

- `V10__providers.sql`: `provider`, `provider_contact`.
- `V11__chat.sql`: `chat_channel`, `chat_channel_member`, `chat_message`.
- `V12__consultive_votings.sql`: `voting`, `voting_option`, `voting_eligibility`, `vote`.

### 2.2. Proveedores

| Método | Ruta | Permiso |
| --- | --- | --- |
| GET | `/communities/{cid}/providers` | `PROVIDER_VIEW` |
| POST | `/communities/{cid}/providers` | `PROVIDER_MANAGE` |
| GET | `/communities/{cid}/providers/{id}` | `PROVIDER_VIEW` |
| PATCH | `/communities/{cid}/providers/{id}` | `PROVIDER_MANAGE` |
| GET | `/communities/{cid}/providers/{id}/contacts` | `PROVIDER_VIEW` |
| POST | `/communities/{cid}/providers/{id}/contacts` | `PROVIDER_MANAGE` |

Reglas:

- Nombre obligatorio; email normalizado a minúsculas.
- Estado `ACTIVE`/`INACTIVE`.
- Varios contactos por proveedor; solo uno principal a la vez.
- Alta, edición y contacto generan auditoría.
- Aislamiento por comunidad.

Pendiente: vincular proveedores a incidencias y facturas.

### 2.3. Chat

| Método | Ruta | Permiso |
| --- | --- | --- |
| GET | `/communities/{cid}/chat/channels` | `CHAT_VIEW` |
| POST | `/communities/{cid}/chat/channels` | `CHAT_MODERATE` |
| GET | `/communities/{cid}/chat/channels/{id}/messages` | `CHAT_VIEW` + acceso al canal |
| POST | `/communities/{cid}/chat/channels/{id}/messages` | `CHAT_VIEW` + acceso al canal |

Reglas:

- Canal `GENERAL` único por comunidad, creado automáticamente.
- Canales `THEMATIC` visibles para miembros con `CHAT_VIEW`.
- Canales `PRIVATE` solo para participantes.
- Mensajes persistidos, máximo 100 por consulta.
- Cuerpo obligatorio y limitado a 4000 caracteres.
- Auditoría no registra cada mensaje para evitar ruido; se documenta como pendiente.

Limitación importante: la actualización es por polling cada 5 segundos; no hay WebSocket/STOMP todavía.
No hay adjuntos, edición, borrado ni moderación en esta entrega.

### 2.4. Votaciones consultivas

| Método | Ruta | Permiso |
| --- | --- | --- |
| GET | `/communities/{cid}/votings` | `VOTE_VIEW` |
| POST | `/communities/{cid}/votings` | `VOTE_MANAGE` |
| GET | `/communities/{cid}/votings/{id}` | `VOTE_VIEW` |
| POST | `/communities/{cid}/votings/{id}/open` | `VOTE_MANAGE` |
| POST | `/communities/{cid}/votings/{id}/close` | `VOTE_MANAGE` |
| POST | `/communities/{cid}/votings/{id}/votes` | `VOTE_CAST` |
| GET | `/communities/{cid}/votings/{id}/results` | `VOTE_VIEW` |

Reglas:

- Estados: `DRAFT`, `OPEN`, `CLOSED`, `CANCELLED`.
- Mínimo dos opciones; sin duplicados.
- Al abrir se genera elegibilidad para miembros activos.
- Coeficiente calculado desde titularidad activa.
- Un voto por elegible y votación.
- Solo se vota dentro del periodo y con la votación abierta.
- Resultados por votos y coeficiente; quórum por coeficiente.
- Aviso legal: votación consultiva no vinculante jurídicamente.
- No hay voto secreto ni votación formal.

## 3. Frontend

- Nueva ruta `/communities/:communityId/providers`.
- Nueva ruta `/communities/:communityId/providers/:providerId`.
- Nueva ruta `/communities/:communityId/chat`.
- Nueva ruta `/communities/:communityId/votings`.
- Nueva ruta `/communities/:communityId/votings/:votingId`.
- Enlaces `Chat`, `Proveedores` y `Votaciones` en sidebar.
- Tarjetas de acceso en dashboard.
- Proveedores: listado, alta, detalle, contactos y activación/desactivación.
- Chat: canales, creación de temáticos/privados, mensajes y polling.
- Votaciones: listado, creación, apertura, cierre, voto y resultados.
- Aviso legal visible en listado y detalle.
- Textos en español y catalán.

## 4. Pruebas ejecutadas

| Validación | Resultado |
| --- | --- |
| Backend `mvn test` | 53 tests, 0 fallos |
| `ProviderIntegrationTest` | 2 tests, 0 fallos |
| `ChatIntegrationTest` | 3 tests, 0 fallos |
| `VotingIntegrationTest` | 2 tests, 0 fallos |
| Permisos de proveedores y contactos | Correcto |
| Canal general, temático y privado | Correcto |
| Acceso de terceros a canal privado | Correcto (`403`) |
| Voto único y duplicado | Correcto (`409`) |
| Resultados, coeficiente y quórum | Correcto |
| Aislamiento entre comunidades | Correcto (`403`) |
| Frontend `npm test` | 28 tests, 0 fallos |
| Frontend `npm run typecheck` | Correcto |
| Frontend `npm run build` | Correcto |
| Smoke test | Proveedor, contacto, chat y votación completa correctos |

## 5. Limitaciones conocidas

- Chat sin WebSocket; actualización por polling.
- Sin adjuntos, moderación, edición ni borrado de mensajes.
- Sin notificaciones específicas de chat o votaciones.
- Proveedores no vinculados aún a incidencias ni facturas.
- Sin rol externo de proveedor ni portal de proveedor.
- Votaciones solo consultivas; sin voto secreto, quórum legal ni mayoría formal.
- Sin push notifications.
- Sin paginación por cursor en chat.
- OpenAPI v0 pendiente de sincronización completa para MVP+.
- Sin pruebas E2E.

## 6. Trazabilidad

| Historia | Estado |
| --- | --- |
| US-130 Canales | Hecho |
| US-131 Mensajes en tiempo real | Parcial: persistencia y polling, sin WebSocket |
| US-132 Conversaciones privadas | Hecho |
| US-133 Adjuntos y moderación | Pendiente |
| US-140 Directorio de proveedores | Hecho |
| US-141 Contactos | Hecho |
| US-142 Vinculación con incidencias/facturas | Pendiente |
| US-150 Propuesta y opciones | Hecho |
| US-151 Elegibilidad y coeficientes | Hecho |
| US-152 Emisión de voto | Hecho |
| US-153 Quórum, resultados y aviso legal | Hecho |

## 7. Próximos pasos

1. FASE 4: validación y piloto.
2. WebSocket/STOMP para chat en tiempo real.
3. Adjuntos y moderación.
4. Vincular proveedores con incidencias y facturas.
5. Push notifications solo con validación de producto y proveedor.
6. Sincronizar OpenAPI y añadir pruebas E2E.

---

*Fin de docs/FASE3.9-IMPLEMENTATION.md — v0.1.0.*
