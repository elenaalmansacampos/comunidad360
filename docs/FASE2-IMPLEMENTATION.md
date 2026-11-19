# COMUNIDAD 360 — FASE 2: Fundaciones implementadas

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Fundaciones ejecutables; validación funcional pendiente |
| Fecha | 2026-11-19 |
| Alcance | Autenticación, multi-tenancy, roles, auditoría e i18n base |

---

## 1. Resumen

Se ha creado la base técnica ejecutable del producto:

- Backend Spring Boot 3.3 con Java 21.
- Frontend React 18 + TypeScript + Vite 8.
- PostgreSQL 16 con migraciones Flyway.
- Autenticación JWT con refresh token rotatorio en cookie `HttpOnly`.
- Multi-tenancy con validación de membresía y permisos en backend.
- Roles y permisos configurables mediante tablas.
- Auditoría base de operaciones sensibles.
- i18n español/catalán en frontend y mensajes backend.
- Tests unitarios e integración con PostgreSQL real mediante Testcontainers.
- Dockerfiles, Docker Compose, CI y documentación de arranque.

## 2. Backend

### 2.1. Estructura

```text
backend/src/main/java/com/comunidad360/
├── Comunidad360Application.java
├── config/          # propiedades, seed local
├── common/          # errores, i18n, trace id
├── security/        # JWT, cookies, filtros, CORS
├── auth/            # login, refresh, invitaciones, reset
├── user/            # perfil y preferencia de idioma
├── community/       # comunidad, dashboard, miembros, invitaciones
├── membership/      # membresías, roles, permisos, tenant access
└── audit/           # auditoría base
```

Se mantiene el monolito modular y las capas `controller`, `service`, `repository`, `entity`, `dto`,
`exception` y `validation` en los módulos implementados.

### 2.2. Endpoints implementados

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | `/api/v1/auth/login` | Login con email y contraseña |
| POST | `/api/v1/auth/refresh` | Rotación de refresh token |
| POST | `/api/v1/auth/logout` | Cierre de sesión |
| POST | `/api/v1/auth/logout-all` | Cierre de todas las sesiones |
| POST | `/api/v1/auth/password/forgot` | Solicitud de recuperación |
| POST | `/api/v1/auth/password/reset` | Restablecimiento con token |
| POST | `/api/v1/auth/invitations/accept` | Aceptar invitación y crear sesión |
| GET | `/api/v1/me` | Perfil y membresías |
| PATCH | `/api/v1/me` | Actualizar nombre |
| PUT | `/api/v1/me/language` | Cambiar idioma preferido |
| GET | `/api/v1/communities` | Comunidades del usuario |
| GET | `/api/v1/communities/{id}` | Detalle de comunidad |
| PATCH | `/api/v1/communities/{id}` | Actualizar configuración |
| GET | `/api/v1/communities/{id}/dashboard` | Resumen por rol |
| GET | `/api/v1/communities/{id}/members` | Miembros visibles |
| POST | `/api/v1/communities/{id}/invitations` | Crear invitación |
| GET | `/api/v1/communities/{id}/invitations` | Listar invitaciones |

Documentación interactiva: `/swagger-ui.html`.

### 2.3. Autenticación

- Access token JWT de 15 minutos.
- Refresh token opaco de 30 días, almacenado como hash SHA-256.
- Cookie `HttpOnly`, `SameSite=Lax` y `Secure` configurable.
- Rotación de refresh y detección de reutilización con revocación global.
- Bloqueo de cuenta tras 5 intentos fallidos durante 15 minutos.
- Logout local y global.
- Contraseñas con BCrypt.
- Tokens de invitación y recuperación de un solo uso con caducidad.
- Recuperación de contraseña revoca todas las sesiones.

### 2.4. Multi-tenancy y permisos

- `community_id` en entidades de negocio.
- `TenantAccessService` valida membresía activa y estado de la comunidad.
- `PermissionService` resuelve permisos por roles, ajustes por comunidad y concesiones directas.
- Roles iniciales: `SUPER_ADMIN`, `COMMUNITY_ADMIN`, `PRESIDENT`, `SECRETARY`, `TREASURER`, `OWNER`,
  `TENANT` y `PROVIDER`.
- Matriz de permisos inicial sembrada con Flyway.
- Accesos cruzados devuelven `403` sin revelar datos.

### 2.5. Auditoría

Se registran eventos como login correcto/fallido, logout, invitaciones, cambios de comunidad y
restablecimiento de contraseña. El modelo `audit_log` permite `before_data` y `after_data` en JSONB.

Pendiente: visor de auditoría, retención y particionado.

## 3. Base de datos

Migraciones:

- `V1__baseline.sql`: usuarios, comunidades, roles, permisos, membresías, invitaciones, refresh tokens,
  recuperación de contraseña y auditoría.
- `V2__seed_roles_permissions.sql`: roles, catálogo de permisos y asignaciones iniciales.

Decisión de implementación: el email se almacena como `VARCHAR(320)` y se normaliza a minúsculas en
servicio, evitando la dependencia de `CITEXT` y facilitando la validación de Hibernate.

## 4. Frontend

### 4.1. Estructura

```text
frontend/src/
├── app/              # App, rutas protegidas
├── components/       # Button, Input, Alert, LanguageSwitcher
├── features/         # auth y communities
├── layouts/          # AuthLayout, AppLayout
├── locales/          # es/ca y configuración i18next
├── pages/            # login, invitación, selector, dashboard, miembros
├── services/         # cliente API con refresh automático
├── styles/           # tokens y estilos globales
├── types/            # tipos de API
└── utils/            # fechas, importes, porcentajes
```

### 4.2. Funcionalidad

- Login y aceptación de invitación.
- Sesión persistente mediante refresh cookie y bootstrap al cargar.
- Selector de comunidad.
- Dashboard básico con miembros activos y roles.
- Listado de miembros con control de acceso.
- Selector de idioma español/catalán con persistencia.
- Formatos regionales con `Intl`.
- Cliente API con refresco automático ante `401`.
- Diseño responsive mobile-first.

## 5. Pruebas y validaciones ejecutadas

| Validación | Resultado |
| --- | --- |
| `mvn test` backend | 9 tests, 0 fallos |
| Tests de integración con PostgreSQL 16 (Testcontainers) | Correctos |
| Aislamiento entre comunidades | Correcto (`403`) |
| Permiso denegado a `OWNER` en listado de miembros | Correcto (`403`) |
| Rotación y reutilización de refresh token | Correcto |
| `npm test` frontend | 5 tests, 0 fallos |
| `npm run typecheck` | Correcto |
| `npm run build` | Correcto |
| `npm audit --omit=dev` | 0 vulnerabilidades |
| `npm audit` | 0 vulnerabilidades |
| `docker compose config` | Válido |
| `docker compose --profile app build` | Correcto (backend y frontend) |
| Smoke test del stack completo (nginx → backend → PostgreSQL) | Login, `/me`, `/communities`, refresh y logout sin access token correctos |
| OpenAPI v0 con Redocly | Válido; 1 warning esperado por servidor localhost |

## 6. Entorno de verificación

- Java 21.0.11.
- Maven 3.9.16.
- Node.js 24.16.0 (build local); Docker usa Node 22.
- PostgreSQL 16.15 vía Testcontainers.
- Docker Engine 29.5.2 con Colima en macOS.

Nota: el entorno local tenía Java 26 como versión por defecto de Maven. Los comandos de verificación se
ejecutaron con `JAVA_HOME` apuntando a Java 21. La CI fija Java 21.

## 7. Limitaciones conocidas

- Solo están implementados los módulos de fundaciones; el resto del MVP queda para FASE 3.
- El envío real de emails no está conectado: recuperación e invitaciones generan tokens y los registran
  solo en modo desarrollo.
- La exposición de tokens de desarrollo debe estar desactivada en producción
  (`APP_SECURITY_EXPOSE_DEV_TOKENS=false`).
- El seed de datos de demostración solo se ejecuta con perfil `local`.
- No hay rate limiting distribuido; solo bloqueo de cuenta por intentos fallidos.
- CSRF está desactivado en la API; se mitiga con `SameSite`, CORS restringido y validación de origen
  pendiente. Debe revisarse antes de producción.
- No se ha habilitado RLS de PostgreSQL; el aislamiento se basa en aplicación, permisos y pruebas.
- No hay integración con S3 ni email en esta fase; la infraestructura local incluye S3Mock y Mailpit, lista para FASE 3.
- No hay visor de auditoría ni políticas de retención implementadas.
- No hay UI para crear invitaciones, recuperar contraseña ni configurar la comunidad.
- No hay Playwright instalado todavía; se añadirá cuando existan flujos completos.
- No hay MFA, push ni integración bancaria.

## 8. Trazabilidad con el backlog

| Historia | Estado |
| --- | --- |
| US-001 Bootstrap técnico | Hecho |
| US-002 Docker de desarrollo | Hecho (PostgreSQL, S3Mock, Mailpit, Redis opcional) |
| US-003 Migraciones y seeds | Hecho |
| US-004 Login | Hecho |
| US-005 Invitación | Hecho; envío real de email pendiente |
| US-006 Verificación de email | Parcial: la aceptación de invitación verifica el email |
| US-007 Recuperación de contraseña | Parcial: backend; envío real de email pendiente |
| US-008 Sesiones y logout global | Hecho |
| US-009 Perfil e idioma | Hecho |
| US-010 Comunidad y membresía | Hecho |
| US-011 TenantGuard y aislamiento | Hecho con pruebas |
| US-012 Roles y permisos | Hecho; UI de administración pendiente |
| US-013 Auditoría base | Hecho; visor y retención pendientes |
| US-014 Acceso de soporte | Pendiente |
| Frontend base, i18n y dashboard | Hecho |

## 9. Próximos pasos

1. Revisar y validar la base con la comunidad piloto.
2. Implementar FASE 3.1: edificios, unidades, elementos comunes y coeficientes.
3. Implementar FASE 3.2: propietarios, inquilinos y titularidad.
4. Implementar FASE 3.3: finanzas y reglas de redondeo.
5. Añadir rate limiting, CSRF y revisión de seguridad antes de producción.
6. Conectar email real y S3.
7. Añadir Playwright para flujos E2E.
8. Mantener actualizados `docs/SPEC.md`, `DATABASE.md`, `API.md` y `SECURITY.md`.

---

*Fin de docs/FASE2-IMPLEMENTATION.md — v0.1.0.*
