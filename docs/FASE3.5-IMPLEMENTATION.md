# COMUNIDAD 360 — FASE 3.5: Incidencias

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Implementada; validación funcional completada |
| Fecha | 2026-11-19 |
| Alcance | Incidencias, seguimiento, comentarios y fotografías |

---

## 1. Resumen

Se ha implementado el quinto incremento vertical de FASE 3:

- Alta de incidencias con título, descripción, categoría, prioridad y ubicación.
- Estados y transiciones controladas.
- Asignación a miembros activos de la comunidad.
- Comentarios públicos y notas internas.
- Histórico de cambios de estado.
- Fotografías con validación y descarga autenticada.
- Visibilidad por rol y por unidad.
- Pantallas de listado, alta y detalle de incidencia.

## 2. Backend

### 2.1. Migración

`backend/src/main/resources/db/migration/V7__incidents.sql` crea:

- `incident`
- `incident_comment`
- `incident_status_history`
- `incident_attachment`

Incluye FKs compuestas por comunidad, prioridades, estados, notas internas y metadatos de archivos.

### 2.2. Endpoints

| Método | Ruta | Permiso |
| --- | --- | --- |
| GET | `/incidents` | `INCIDENT_VIEW_ALL`, `INCIDENT_VIEW_OWN` o creación |
| POST | `/incidents` | `INCIDENT_CREATE` |
| GET | `/incidents/{id}` | acceso a la incidencia |
| PATCH | `/incidents/{id}` | `INCIDENT_MANAGE` |
| POST | `/incidents/{id}/comments` | acceso a la incidencia |
| POST | `/incidents/{id}/attachments` | acceso a la incidencia |
| GET | `/incidents/{id}/attachments/{attachmentId}` | acceso a la incidencia |

### 2.3. Estados y transiciones

```text
OPEN → IN_PROGRESS | WAITING_PROVIDER | CANCELLED
IN_PROGRESS → WAITING_PROVIDER | RESOLVED | CANCELLED
WAITING_PROVIDER → IN_PROGRESS | RESOLVED | CANCELLED
RESOLVED → CLOSED | IN_PROGRESS
CLOSED → (sin transiciones)
CANCELLED → (sin transiciones)
```

### 2.4. Reglas de negocio

- La ubicación debe pertenecer a la misma comunidad.
- El estado inicial es `OPEN`.
- Los cambios de estado generan histórico.
- `RESOLVED` registra fecha de resolución; `CLOSED` registra fecha de cierre.
- La asignación exige que la persona sea miembro activo.
- Las notas internas solo son visibles para perfiles con `INCIDENT_VIEW_ALL`.
- Un propietario o inquilino ve incidencias reportadas por él o asociadas a sus unidades.
- Los proveedores aún no se asignan desde el módulo de incidencias; se incorporará con proveedores.
- Las fotografías admiten JPEG, PNG y WebP hasta 5 MB.
- Se valida la firma real del archivo, no solo el MIME declarado.
- La descarga requiere autenticación y permisos sobre la incidencia.
- Se guardan nombre, MIME, tamaño, hash SHA-256 y autor.
- Cada alta, cambio de estado, comentario o fotografía genera auditoría.

### 2.5. Almacenamiento de fotografías

Las fotografías usan `StorageService`, con almacenamiento local configurable en
`APP_STORAGE_LOCAL_PATH` o S3 compatible. En Docker Compose se usa S3Mock; en tests se usa
almacenamiento local. La base de datos guarda solo metadatos y hash.

## 3. Frontend

- Nueva ruta `/communities/:communityId/incidents`.
- Nueva ruta `/communities/:communityId/incidents/:incidentId`.
- Enlace `Incidencias` en sidebar y navegación móvil.
- Listado con filtros por estado, prioridad y asignación.
- Formulario de alta con ubicación por unidad.
- Detalle con descripción, estado, prioridad, responsable y unidad.
- Cambio de estado y asignación para perfiles con gestión.
- Comentarios públicos y notas internas.
- Subida de fotografías y descarga autenticada.
- Histórico de estados.
- Textos en español y catalán.

## 4. Pruebas ejecutadas

| Validación | Resultado |
| --- | --- |
| Backend `mvn test` | 33 tests, 0 fallos |
| `IncidentIntegrationTest` | 4 tests, 0 fallos |
| Visibilidad por unidad y rol | Correcto |
| Transiciones válidas e inválidas | Correcto |
| Comentarios internos ocultos al propietario | Correcto |
| Subida y descarga de fotografía | Correcto |
| Tipo de archivo no permitido | Correcto (`422`) |
| Aislamiento entre comunidades | Correcto (`403`) |
| Frontend `npm test` | 17 tests, 0 fallos |
| Frontend `npm run typecheck` | Correcto |
| Frontend `npm run build` | Correcto |
| Smoke test completo | Alta, comentario, fotografía, descarga, cambio de estado y ruta frontend correctos |

## 5. Limitaciones conocidas

- No hay asignación a proveedores; requiere el módulo de proveedores.
- No hay notificaciones de incidencias todavía.
- Las fotos usan almacenamiento local o S3 según configuración; no hay antivirus.
- No hay antivirus; solo validación de tipo y tamaño.
- No hay paginación ni búsqueda de texto.
- No hay adjuntos múltiples en una sola petición.
- No hay pruebas E2E.
- La sección de incidencias del OpenAPI v0 no se ha sincronizado por completo.

## 6. Trazabilidad

| Historia | Estado |
| --- | --- |
| US-070 Crear incidencia | Hecho |
| US-071 Fotografías | Hecho con almacenamiento local |
| US-072 Asignación, prioridad y estados | Hecho salvo proveedor |
| US-073 Comentarios y seguimiento | Hecho |
| US-074 Visibilidad de incidencias | Hecho |

## 7. Próximos pasos

1. Implementar FASE 3.7: reuniones y notificaciones.
2. Añadir notificaciones de cambios de incidencia.
3. Incorporar asignación a proveedores.
4. Añadir antivirus y validación avanzada de archivos.
5. Añadir paginación, búsqueda y pruebas E2E.

---

*Fin de docs/FASE3.5-IMPLEMENTATION.md — v0.1.0.*
