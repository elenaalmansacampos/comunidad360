# COMUNIDAD 360 — FASE 3.6: Documentos y almacenamiento S3

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Implementada; validación funcional completada |
| Fecha | 2026-11-19 |
| Alcance | Documentos, categorías, visibilidad, versionado, S3 y auditoría de accesos |

---

## 1. Resumen

Se ha implementado el sexto incremento vertical de FASE 3:

- Subida de documentos con categoría, título y descripción.
- Visibilidad por comunidad, junta, propietarios, rol o ámbito privado.
- Versionado sin sobrescribir archivos.
- Descarga autenticada y URL firmada de corta duración.
- Almacenamiento local o S3 compatible, seleccionable por configuración.
- Auditoría de subidas, actualizaciones, descargas y archivado.
- Pantallas de listado, alta, detalle, edición de metadatos y versiones.

## 2. Backend

### 2.1. Migración

`backend/src/main/resources/db/migration/V8__documents.sql` crea:

- `document`
- `document_version`
- `document_acl`
- `document_access_log`

Incluye FKs compuestas por comunidad, versión actual, estados, visibilidad, ACL y registro de accesos.

### 2.2. Endpoints

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | `/communities/{cid}/documents` | `DOCUMENT_VIEW` | Listar documentos visibles; filtro `category` |
| POST | `/communities/{cid}/documents` | `DOCUMENT_UPLOAD` o `DOCUMENT_MANAGE` | Crear documento con `multipart/form-data` |
| GET | `/communities/{cid}/documents/{id}` | `DOCUMENT_VIEW` + visibilidad | Detalle con versiones |
| PATCH | `/communities/{cid}/documents/{id}` | `DOCUMENT_MANAGE` | Editar metadatos, visibilidad o estado |
| DELETE | `/communities/{cid}/documents/{id}` | `DOCUMENT_MANAGE` | Archivar |
| POST | `/communities/{cid}/documents/{id}/versions` | `DOCUMENT_VIEW` + `DOCUMENT_UPLOAD` | Añadir versión |
| GET | `/communities/{cid}/documents/{id}/download` | `DOCUMENT_VIEW` + visibilidad | Descarga autenticada; `versionId` opcional |

Las respuestas incluyen `downloadUrl`:

- Con S3: URL prefirmada con caducidad configurable.
- Con almacenamiento local: ruta autenticada del backend.

### 2.3. Visibilidad

| Valor | Quién puede ver |
| --- | --- |
| `COMMUNITY` | Miembros activos con `DOCUMENT_VIEW` |
| `BOARD` | Administración, presidencia, secretaría y tesorería |
| `OWNERS` | Junta y propietarios con titularidad activa |
| `ROLE` | Miembros con ACL explícita |
| `PRIVATE` | Propietario del documento o ACL explícita |

Reglas adicionales:

- `DOCUMENT_MANAGE` permite gestionar y ver todos los documentos de la comunidad.
- El propietario del documento siempre puede verlo.
- Las ACL admiten `USER`, `ROLE` y `MEMBERSHIP`.
- La autorización se valida siempre en backend; el frontend solo oculta acciones.
- Ningún documento cruza el límite de comunidad.

### 2.4. Versionado

- Cada documento empieza en `versionNumber = 1`.
- Cada nueva versión incrementa el número y actualiza `current_version_id`.
- Las versiones anteriores se conservan y pueden descargarse con `versionId`.
- Se guardan nombre original saneado, MIME, tamaño, SHA-256 y autor.
- El conflicto de versión de metadatos devuelve `409 VERSION_CONFLICT`.

### 2.5. Validación de archivos

- Tamaño máximo: 25 MB (`APP_STORAGE_MAX_DOCUMENT_BYTES`).
- Tipos permitidos: PDF, JPEG, PNG, WebP, DOCX, XLSX y TXT.
- Se valida la firma real de PDF, imágenes y contenedores ZIP de Office.
- El nombre se sanea antes de persistirlo.
- `scan_status` queda en `CLEAN` tras la validación básica.
- No hay antivirus ni cuarentena real todavía.

### 2.6. Almacenamiento

La interfaz `StorageService` tiene dos implementaciones:

- `LocalStorageService`: desarrollo y tests.
- `S3StorageService`: S3 compatible mediante AWS SDK v2.

Configuración principal:

```text
APP_STORAGE_TYPE=local|s3
APP_STORAGE_LOCAL_PATH=./data/uploads
APP_STORAGE_MAX_DOCUMENT_BYTES=26214400
APP_STORAGE_S3_ENDPOINT=http://s3mock:9090
APP_STORAGE_S3_PUBLIC_ENDPOINT=http://localhost:9090
APP_STORAGE_S3_REGION=us-east-1
APP_STORAGE_S3_BUCKET=comunidad360-documents
APP_STORAGE_S3_ACCESS_KEY=test
APP_STORAGE_S3_SECRET_KEY=test
APP_STORAGE_S3_PATH_STYLE=true
APP_STORAGE_S3_CREATE_BUCKET=true
APP_STORAGE_SIGNED_URL_TTL_SECONDS=300
```

`APP_STORAGE_S3_ENDPOINT` es el endpoint interno del backend. `APP_STORAGE_S3_PUBLIC_ENDPOINT` es el
endpoint usado para firmar URLs accesibles desde el navegador. En Docker Compose se usa S3Mock.

### 2.7. Auditoría

`document_access_log` registra:

- `UPLOAD`, `DOWNLOAD`, `UPDATE` y `DELETE`.
- Usuario, documento, versión, fecha, hash de IP y user-agent.
- La auditoría funcional general también registra creación, versión, edición y archivado.

## 3. Frontend

- Nueva ruta `/communities/:communityId/documents`.
- Nueva ruta `/communities/:communityId/documents/:documentId`.
- Enlace `Documentos` en sidebar.
- Acceso desde las tarjetas del dashboard.
- Listado con filtro por categoría.
- Alta con título, descripción, categoría, visibilidad y archivo.
- Detalle con metadatos, archivo actual y versiones.
- Edición de metadatos para perfiles con gestión.
- Subida de nuevas versiones.
- Descarga de versión actual o histórica.
- Archivado con confirmación.
- Soporte de URL externa prefirmada y descarga autenticada local.
- Textos en español y catalán.

## 4. Pruebas ejecutadas

| Validación | Resultado |
| --- | --- |
| Backend `mvn test` | 37 tests, 0 fallos |
| `DocumentIntegrationTest` | 4 tests, 0 fallos |
| Visibilidad comunidad/junta/propietarios | Correcto |
| Documento privado y permisos de subida | Correcto |
| Versionado y descarga de versión anterior | Correcto |
| Conflicto de versión de metadatos | Correcto (`409`) |
| Archivo no permitido | Correcto (`422`) |
| Aislamiento entre comunidades | Correcto (`403`) |
| Registro de accesos | Correcto |
| Frontend `npm test` | 20 tests, 0 fallos |
| Frontend `npm run typecheck` | Correcto |
| Frontend `npm run build` | Correcto |
| Smoke S3 con S3Mock | Subida, URL prefirmada, descarga, versión y ruta frontend correctos |

## 5. Limitaciones conocidas

- No hay antivirus ni cuarentena real; solo validación de tipo, firma y tamaño.
- No hay interfaz para gestionar ACL de visibilidad `ROLE`.
- No existe todavía `document_link` para vincular documentos a incidencias, facturas o reuniones.
- No hay búsqueda de texto, paginación ni previsualización.
- No hay restauración de versiones ni borrado físico.
- No hay visor de auditoría documental en frontend.
- No hay políticas de retención, ciclo de vida S3 ni cifrado con KMS.
- S3Mock es solo para desarrollo; producción requiere un proveedor S3 real.
- La sección de documentos del OpenAPI v0 no está sincronizada por completo.
- No hay pruebas E2E.

## 6. Trazabilidad

| Historia | Estado |
| --- | --- |
| US-080 Subida y metadatos | Hecho |
| US-081 Permisos y visibilidad | Hecho, salvo gestión visual de ACL |
| US-082 Versionado | Hecho |
| US-083 Descarga firmada y auditoría | Hecho |

## 7. Próximos pasos

1. Implementar FASE 3.7: reuniones y notificaciones.
2. Añadir antivirus/cuarentena y análisis asíncrono.
3. Incorporar gestión de ACL y `document_link`.
4. Añadir búsqueda, paginación y previsualización.
5. Sincronizar OpenAPI con documentos y añadir pruebas E2E.

---

*Fin de docs/FASE3.6-IMPLEMENTATION.md — v0.1.0.*
