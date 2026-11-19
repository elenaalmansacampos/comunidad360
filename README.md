# COMUNIDAD 360

Plataforma SaaS multi-comunidad para la gestión de comunidades de propietarios.

> **Estado actual: FASE 4 — Validación técnica en curso; piloto real pendiente.**
> El MVP+ cubre autenticación, multi-tenancy, estructura, titularidad, finanzas, banca, incidencias,
> documentación, reuniones, notificaciones, dashboard, proveedores, chat y votaciones consultivas.
> La validación técnica automatizada está en verde; faltan legal, seguridad de producción y piloto.

## Qué incluye el producto

- Gestión de comunidades, edificios, viviendas, locales, garajes y trasteros.
- Propietarios, inquilinos, coeficientes y titularidad compartida.
- Finanzas: ingresos, gastos, cuotas, derramas, facturas, pagos y conciliación bancaria CSV.
- Incidencias con fotografías, prioridades, asignación y seguimiento.
- Documentación con permisos, versionado y auditoría.
- Reuniones, convocatorias, asistencia y actas.
- Chat y notificaciones.
- Dashboard por rol.
- Español y catalán desde el MVP.
- Aislamiento multi-tenant y seguridad desde el diseño.

## Stack

| Capa | Tecnología |
| --- | --- |
| Frontend | React 18, TypeScript, Vite 8, React Router 7, Sass, TanStack Query, Zustand, Recharts, i18next |
| Backend | Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA, Flyway, Maven |
| Base de datos | PostgreSQL 16 |
| Infraestructura | Docker, Docker Compose, S3 compatible, CI/CD |
| Testing | JUnit 5, Mockito, Spring Boot Test, Testcontainers, Vitest, React Testing Library |

## Documentación

| Documento | Contenido |
| --- | --- |
| [`docs/SPEC.md`](docs/SPEC.md) | Alcance, requisitos, roles, MVP y criterios de éxito |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Arquitectura, multi-tenancy, módulos y despliegue |
| [`docs/DATABASE.md`](docs/DATABASE.md) | Modelo ER, diccionario de datos e integridad |
| [`docs/API.md`](docs/API.md) | Convenciones y catálogo inicial de endpoints |
| [`docs/SECURITY.md`](docs/SECURITY.md) | Seguridad, RGPD/LOPDGDD, LPH y matriz de permisos |
| [`docs/DESIGN-SYSTEM.md`](docs/DESIGN-SYSTEM.md) | Identidad visual, componentes y accesibilidad |
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | Fases, dependencias y criterios de salida |
| [`docs/BACKLOG.md`](docs/BACKLOG.md) | Épicas, historias y criterios de aceptación |
| [`docs/FASE1-DESIGN.md`](docs/FASE1-DESIGN.md) | Propuestas de decisión y criterios de salida de FASE 1 |
| [`docs/WIREFRAMES.md`](docs/WIREFRAMES.md) | Navegación y wireframes mobile-first |
| [`docs/openapi.yaml`](docs/openapi.yaml) | Contrato OpenAPI v0 del núcleo del MVP |
| [`docs/FASE2-IMPLEMENTATION.md`](docs/FASE2-IMPLEMENTATION.md) | Qué se ha implementado en FASE 2 y limitaciones |
| [`docs/FASE3.1-IMPLEMENTATION.md`](docs/FASE3.1-IMPLEMENTATION.md) | Estructura: edificios, unidades, elementos comunes y coeficientes |
| [`docs/FASE3.2-IMPLEMENTATION.md`](docs/FASE3.2-IMPLEMENTATION.md) | Titularidad, copropiedad, inquilinos e histórico |
| [`docs/FASE3.3-IMPLEMENTATION.md`](docs/FASE3.3-IMPLEMENTATION.md) | Finanzas: ejercicios, cuotas, pagos, facturas y apuntes |
| [`docs/FASE3.4-IMPLEMENTATION.md`](docs/FASE3.4-IMPLEMENTATION.md) | Banca: cuentas, CSV, movimientos y conciliación |
| [`docs/FASE3.5-IMPLEMENTATION.md`](docs/FASE3.5-IMPLEMENTATION.md) | Incidencias, seguimiento, comentarios y fotografías |
| [`docs/FASE3.6-IMPLEMENTATION.md`](docs/FASE3.6-IMPLEMENTATION.md) | Documentos, visibilidad, versionado, S3 y auditoría |
| [`docs/FASE3.7-IMPLEMENTATION.md`](docs/FASE3.7-IMPLEMENTATION.md) | Reuniones, asistencia, acuerdos, notificaciones y email |
| [`docs/FASE3.8-IMPLEMENTATION.md`](docs/FASE3.8-IMPLEMENTATION.md) | Dashboard por rol, indicadores, evolución e informe PDF |
| [`docs/FASE3.9-IMPLEMENTATION.md`](docs/FASE3.9-IMPLEMENTATION.md) | Base MVP+: proveedores, chat y votaciones consultivas |
| [`docs/FASE4-VALIDATION.md`](docs/FASE4-VALIDATION.md) | E2E, accesibilidad, seguridad, carga y backup/restore |
| [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) | Despliegue de producción, secretos, TLS, backups y rollback |
| [`docs/PILOT-RUNBOOK.md`](docs/PILOT-RUNBOOK.md) | Carga inicial, operación, soporte y cierre del piloto |
| [`docs/ADR/`](docs/ADR/) | Decisiones arquitectónicas |

Documentos de trabajo:

- [`CONTRIBUTING.md`](CONTRIBUTING.md)
- [`.env.example`](.env.example)

## Estructura del repositorio

```text
.
├── backend/             # Spring Boot + Flyway + tests
├── frontend/            # React + Vite + i18n + tests
├── docs/                # Especificación, arquitectura y decisiones
├── .github/workflows/   # CI
├── docker-compose.yml   # Infraestructura y stack de desarrollo
├── README.md
└── CONTRIBUTING.md
```

## Requisitos de desarrollo

- Java 21 LTS.
- Maven 3.9+.
- Node.js 22 LTS y npm.
- Docker y Docker Compose.
- PostgreSQL 16 (o contenedor).
- Git.

## Puesta en marcha local

### 1. Infraestructura

```bash
cp .env.example .env
docker compose up -d postgres s3mock mailpit
```

| Servicio | Puerto | Uso |
| --- | --- | --- |
| PostgreSQL | 5432 | Base de datos |
| S3Mock | 9090 / 9191 | API S3 compatible para desarrollo |
| Mailpit | 1025 / 8025 | SMTP de pruebas y bandeja web |
| Redis | 6379 | Opcional, perfil `redis` |

### 2. Backend

```bash
cd backend
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

API: `http://localhost:8080/api/v1`
Swagger UI: `http://localhost:8080/swagger-ui.html`

El perfil `local` activa datos de demostración y la exposición de tokens de desarrollo:

```text
Email:    admin@comunidad360.local
Password: Admin1234!
```

Estas credenciales son **solo para desarrollo local**. No usar en producción.

### 3. Frontend

```bash
cd frontend
npm ci
npm run dev
```

Aplicación: `http://localhost:5173`

### 4. Stack completo con Docker

```bash
docker compose --profile app up --build
```

Aplicación: `http://localhost:3000`

Si los puertos por defecto están ocupados, se pueden cambiar los puertos de host:

```bash
BACKEND_HTTP_PORT=18080 FRONTEND_HTTP_PORT=13000 docker compose --profile app up --build
```

## Pruebas

### Backend

```bash
cd backend
mvn test
```

Requiere Docker para Testcontainers. En macOS con Colima puede ser necesario configurar el socket:

```bash
DOCKER_HOST=unix://$HOME/.colima/default/docker.sock \
TESTCONTAINERS_RYUK_DISABLED=true \
mvn test -DargLine="-Dapi.version=1.43 -Ddocker.host=unix://$HOME/.colima/default/docker.sock"
```

### Frontend

```bash
cd frontend
npm test
npm run typecheck
npm run build
npm audit --omit=dev
```

## Alcance actual

Implementado en FASE 2:

- Backend Spring Boot modular y frontend React.
- Migraciones Flyway con roles, permisos y datos semilla.
- Login, refresh token rotatorio, logout, logout global.
- Invitaciones y recuperación de contraseña (envío real de email pendiente).
- Perfil, preferencia de idioma y sesión persistente por refresh cookie.
- Multi-tenancy con validación de membresía y permisos en backend.
- Comunidades, resumen y listado de miembros.
- Estructura: edificios, unidades, elementos comunes y validación de coeficientes.
- Auditoría base de operaciones sensibles.
- i18n español/catalán en frontend y mensajes backend.
- Tests unitarios e integración con PostgreSQL real vía Testcontainers.

Implementado en FASE 3.1:

- Edificios, unidades y elementos comunes.
- Resumen y validación de la suma de coeficientes.
- Página de estructura en el frontend.

Implementado en FASE 3.2:

- Titularidad, copropiedad e histórico por unidad.
- Autorización y revocación de inquilinos.
- Detalle de unidad con titulares y alquileres.

Implementado en FASE 3.3:

- Ejercicios fiscales, presupuestos y partidas.
- Cuotas, derramas, cargos y reparto de céntimos.
- Facturas de proveedor, pagos y apuntes contables.
- Resumen financiero por comunidad y por propietario.
- Página de finanzas con formularios y pestañas.

Implementado en FASE 3.4:

- Cuentas bancarias con IBAN enmascarado.
- Importación CSV con mapeo de columnas y detección de duplicados.
- Movimientos bancarios y conciliación con pagos o apuntes.
- Página de banca en el frontend.

Implementado en FASE 3.5:

- Incidencias con prioridad, ubicación, estados y asignación.
- Comentarios públicos y notas internas.
- Fotografías con validación y descarga autenticada.
- Histórico de estados y filtros de visibilidad.

Implementado en FASE 3.6:

- Documentos con categorías, metadatos y visibilidad por rol.
- Versionado sin sobrescritura y descarga de versiones anteriores.
- Almacenamiento local o S3 compatible con URL firmada.
- Auditoría de subidas, descargas, ediciones y archivado.
- Páginas de listado, detalle, edición y versiones.

Implementado en FASE 3.7:

- Convocatorias, orden del día y estados de reunión.
- Asistencia presencial, online, representada o ausente.
- Coeficiente de asistencia y acuerdos con votos.
- Vinculación de actas a documentos.
- Campana de notificaciones, preferencias y contador de no leídas.
- Email transaccional en español/catalán mediante Mailpit en desarrollo.

Implementado en FASE 3.8:

- Dashboard por rol con indicadores y widgets.
- Resumen financiero de comunidad o propietario.
- Evolución mensual de ingresos y gastos.
- Próximas reuniones y últimas incidencias.
- Informe PDF con permisos y auditoría.

Implementado en FASE 3.9:

- Directorio de proveedores y contactos.
- Chat general, temático y privado con mensajes persistidos.
- Votaciones consultivas con elegibilidad, coeficiente, voto único y resultados.
- Aviso legal de votación no vinculante.
- Acceso desde sidebar y dashboard.

Pendiente: push notifications, chat en tiempo real por WebSocket, adjuntos/moderación y
vinculación de proveedores con incidencias/facturas.

Validación FASE 4 realizada:

- E2E Playwright de login, dashboard y navegación; axe sin hallazgos serios/críticos.
- Paridad i18n es/ca con 623 claves.
- `npm audit` sin vulnerabilidades de producción.
- Cabeceras CSP, Permissions-Policy, X-Frame y Referrer.
- Backup y restauración real de PostgreSQL verificados, también cifrados con AES-256.
- Rate limiting de autenticación y perfil de producción endurecido.
- Prueba básica de carga local.

Pendiente para el piloto: legal, HTTPS/secretos de producción, rate limiting, antivirus,
backup programado/cifrado, accesibilidad manual, pentest y usuarios reales.

## Seguridad

- No se incluyen secretos en el repositorio.
- No se almacenan credenciales bancarias.
- No se implementan pagos reales.
- No se publican datos financieros individuales de otros propietarios.
- Las decisiones de seguridad se documentan en `docs/SECURITY.md`.

## Licencia

Pendiente de decisión. No se distribuye licencia hasta que el titular del producto lo determine.
