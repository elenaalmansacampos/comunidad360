# COMUNIDAD 360 — Roadmap

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | FASE 4 en curso; piloto pendiente |
| Fecha | 2026-11-19 |
| Metodología | Spec Driven Development, entregas incrementales |

> El roadmap prioriza una implementación funcional y verificable. No se avanza de fase si existen
> errores críticos abiertos o requisitos bloqueantes sin validar.

---

## 1. Resumen de fases

| Fase | Objetivo | Estado |
| --- | --- | --- |
| FASE 0 | Discovery, especificación y arquitectura | Documentada |
| FASE 1 | Diseño detallado y validación funcional | Documentada |
| FASE 2 | Fundaciones técnicas y multi-tenancy | Implementada |
| FASE 3 | MVP por módulos priorizados | Implementada; MVP+ base incluido |
| FASE 4 | Validación, seguridad y piloto | Validación técnica en curso; piloto pendiente |
| POST-MVP | Evolución SaaS e IA | Pendiente |

## 2. FASE 0 — Discovery y especificación

### Objetivos

- Analizar requisitos y detectar ambigüedades.
- Definir arquitectura, modelo de datos y seguridad.
- Diseñar el alcance realista del MVP.
- Crear backlog inicial y criterios de aceptación.
- Documentar decisiones y riesgos.

### Entregables

- `docs/SPEC.md`
- `docs/ARCHITECTURE.md`
- `docs/DATABASE.md`
- `docs/API.md`
- `docs/SECURITY.md`
- `docs/DESIGN-SYSTEM.md`
- `docs/ROADMAP.md`
- `docs/BACKLOG.md`
- `docs/ADR/`
- `README.md`, `CONTRIBUTING.md`, `.env.example`

### Criterio de salida

- Documentación coherente y revisada.
- Requisitos pendientes identificados.
- Decisiones arquitectónicas registradas.
- MVP delimitado.
- No se ha implementado código de producción.

## 3. FASE 1 — Diseño detallado

### Objetivos

- Resolver preguntas bloqueantes de `SPEC.md`.
- Validar el modelo de datos con la comunidad piloto.
- Fijar contratos API y Design System.
- Diseñar wireframes y navegación.
- Convertir historias en tareas técnicas.

### Actividades

1. Validar alcance MVP y MVP+ con Product Owner.
2. Resolver Q-01 a Q-18 o marcar explícitamente las que pasan a fases posteriores.
3. Revisar el modelo ER con Database Architect y asesoría contable.
4. Validar reglas de coeficientes, titularidad y redondeo financiero.
5. Definir plan de categorías contables y de incidencias.
6. Crear wireframes mobile-first de flujos críticos:
   - login e invitación;
   - alta de comunidad y unidades;
   - dashboard por rol;
   - cuotas, cargos y pagos;
   - incidencias;
   - documentos;
   - reunión y acta.
7. Definir contratos OpenAPI iniciales.
8. Validar accesibilidad y contraste.
9. Revisar legalmente RGPD/LPH y votaciones.
10. Actualizar backlog con estimaciones relativas y dependencias.

### Entregables

- Decisiones pendientes resueltas o planificadas.
- Wireframes y flujos.
- Contratos API v0.
- Design tokens y componentes base.
- Backlog refinado y priorizado.
- ADRs adicionales si hay cambios.

Estado actual de los entregables:

| Entregable | Ubicación | Estado |
| --- | --- | --- |
| Propuestas de decisión | [`FASE1-DESIGN.md`](./FASE1-DESIGN.md) | Generadas; pendientes de validación |
| Wireframes y navegación | [`WIREFRAMES.md`](./WIREFRAMES.md) | Generados; pendientes de validación |
| Contrato OpenAPI v0 | [`openapi.yaml`](./openapi.yaml) | Validado con Redocly; pendiente de revisión funcional |

### Criterio de salida

- No hay contradicciones funcionales abiertas que impidan FASE 2.
- El modelo de datos está validado para el piloto.
- Los flujos críticos tienen diseño aprobado.
- Los requisitos legales están identificados y planificados.

## 4. FASE 2 — Fundaciones

### Objetivos

- Crear la base técnica segura y reproducible.
- Implementar autenticación, autorización y multi-tenancy.
- Preparar entornos y CI/CD.
- Establecer la base bilingüe.

### Actividades

1. Crear repositorio y estructura de carpetas.
2. Configurar backend Spring Boot con Java 21.
3. Configurar frontend React + TypeScript + Vite.
4. Configurar PostgreSQL local y migraciones Flyway.
5. Configurar Docker Compose de desarrollo:
   - PostgreSQL;
   - almacenamiento S3 compatible (S3Mock o equivalente);
   - servicio de email de pruebas (Mailpit o equivalente);
   - Redis opcional.
6. Implementar `app_user`, autenticación y sesiones.
7. Implementar invitaciones y recuperación.
8. Implementar comunidad, membresía, roles y permisos.
9. Implementar TenantGuard y pruebas de aislamiento base.
10. Implementar auditoría mínima.
11. Implementar i18n base en frontend y backend.
12. Configurar CI: build, tests, lint, SCA, SAST.
13. Configurar entornos local, test y staging.
14. Documentar procedimiento de arranque y despliegue.

### Entregables

- Proyecto compilable y ejecutable.
- Login e invitación funcionales.
- Multi-tenancy base con pruebas.
- Migraciones iniciales.
- CI verde.
- Documentación de entorno actualizada.

### Criterio de salida

- Un usuario puede aceptar una invitación, iniciar sesión y acceder a su comunidad.
- Las pruebas de aislamiento base pasan.
- El proyecto se levanta con un procedimiento reproducible.
- No hay secretos en el repositorio.

### Estado actual de FASE 2

| Entregable | Estado |
| --- | --- |
| Backend Spring Boot modular | Implementado |
| Frontend React + Vite + i18n | Implementado |
| PostgreSQL + Flyway + seeds | Implementado |
| Autenticación, refresh, logout | Implementado |
| Invitaciones y recuperación | Backend implementado; email real pendiente |
| Multi-tenancy y permisos | Implementado con pruebas |
| Auditoría base | Implementado |
| Frontend de comunidades y miembros | Implementado |
| Docker Compose y Dockerfiles | Implementado y validado (build + smoke test) |
| CI build + tests | Configurado; pendiente primera ejecución en GitHub |
| SCA/SAST | Parcial: `npm audit`; pendiente SAST |
| Staging | Pendiente |

Detalle completo: [`FASE2-IMPLEMENTATION.md`](./FASE2-IMPLEMENTATION.md).

## 5. FASE 3 — MVP

### Estrategia

Implementar por incrementos verticales, cada uno con backend, frontend, pruebas, i18n y documentación.
No se considera terminado un módulo sin sus pruebas y sin revisión de seguridad.

### Incremento 3.1 — Comunidad y estructura

- Alta y configuración de comunidad.
- Edificios.
- Unidades y tipos.
- Elementos comunes.
- Validación de coeficientes.
- Importación inicial de unidades (si el piloto lo necesita; evaluar CSV).
- Dashboard base de comunidad.

**Estado:** implementado edificios, unidades, elementos comunes y resumen de coeficientes.
Pendiente: importación inicial y UI de edición/archivado. Ver
[`FASE3.1-IMPLEMENTATION.md`](./FASE3.1-IMPLEMENTATION.md).

### Incremento 3.2 — Propietarios e inquilinos

- Titularidad y copropiedad.
- Coeficientes e instantáneas.
- Autorización de inquilinos.
- Histórico temporal.
- Visibilidad de datos según rol.

**Estado:** implementado titularidad, copropiedad, inquilinos, histórico y detalle de unidad.
Pendiente: edición avanzada, documentos justificativos y snapshot de coeficiente en cargos.
Ver [`FASE3.2-IMPLEMENTATION.md`](./FASE3.2-IMPLEMENTATION.md).

### Incremento 3.3 — Finanzas núcleo

- Ejercicios fiscales.
- Presupuesto y partidas.
- Ingresos y gastos.
- Cuotas ordinarias.
- Derramas.
- Cargos por unidad.
- Facturas de proveedor.
- Pagos.
- Saldo y estado de cuotas.
- Informes básicos.
- Auditoría financiera.
- Reglas de redondeo e idempotencia.

**Estado:** implementado el núcleo financiero, cargos, pagos, facturas, apuntes, presupuesto y resumen.
Pendiente: conciliación bancaria (FASE 3.4), UI del informe por categorías y reparto manual.
Ver [`FASE3.3-IMPLEMENTATION.md`](./FASE3.3-IMPLEMENTATION.md).

### Incremento 3.4 — Banca CSV

- Cuentas bancarias enmascaradas.
- Importación CSV.
- Detección de duplicados.
- Movimientos.
- Conciliación con pagos/apuntes.
- Histórico de importaciones.

**Estado:** implementado cuentas, importación CSV, movimientos, conciliación y deshacer conciliación.
Pendiente: PSD2, conciliación parcial y sugerencias automáticas.
Ver [`FASE3.4-IMPLEMENTATION.md`](./FASE3.4-IMPLEMENTATION.md).

### Incremento 3.5 — Incidencias

- Alta con categoría, prioridad y ubicación.
- Fotografías.
- Asignación a responsable/proveedor.
- Estados y transiciones.
- Comentarios.
- Histórico.
- Visibilidad por rol.

**Estado:** implementadas incidencias, estados, asignación a miembros, comentarios, histórico y fotografías.
Las fotografías usan `StorageService`, con almacenamiento local o S3 según configuración.
Pendiente: proveedores y notificaciones.
Ver [`FASE3.5-IMPLEMENTATION.md`](./FASE3.5-IMPLEMENTATION.md).

### Incremento 3.6 — Documentos

- Categorías y metadatos.
- Subida y versionado.
- Permisos y visibilidad.
- Descarga con URL firmada.
- Auditoría de accesos.
- Cuarentena y análisis de archivos.

**Estado:** implementados categorías, metadatos, visibilidad, versionado, S3, URL firmada y auditoría.
Pendiente: antivirus/cuarentena real, gestión visual de ACL, `document_link`, búsqueda y previsualización.
Ver [`FASE3.6-IMPLEMENTATION.md`](./FASE3.6-IMPLEMENTATION.md).

### Incremento 3.7 — Reuniones y notificaciones

- Convocatorias y orden del día.
- Asistencia y representación.
- Acuerdos y acta.
- Notificaciones internas y email.
- Preferencias de notificación.
- Idioma del destinatario.

**Estado:** implementados convocatorias, agenda, asistencia con coeficiente, acuerdos, acta,
notificaciones internas, preferencias y email con Mailpit.
Pendiente: outbox/reintentos, push, votaciones formales, acta PDF y registro masivo.
Ver [`FASE3.7-IMPLEMENTATION.md`](./FASE3.7-IMPLEMENTATION.md).

### Incremento 3.8 — Dashboard e informes

- Dashboard por rol.
- Indicadores financieros.
- Incidencias y reuniones próximas.
- Evolución básica.
- Informe PDF básico (si se confirma en alcance).
- Exportaciones con control de acceso.

**Estado:** implementados dashboard por rol, indicadores, widgets, evolución mensual e informe PDF
con `REPORT_VIEW` y auditoría.
Pendiente: exportaciones CSV/Excel, comparativas, informes programados y gráficos en PDF.
Ver [`FASE3.8-IMPLEMENTATION.md`](./FASE3.8-IMPLEMENTATION.md).

### Incremento 3.9 — MVP+ (condicionado)

- Chat general, temático y privado.
- Proveedores.
- Votaciones consultivas con aviso legal.
- Push notifications (solo si se valida).

**Estado:** implementados proveedores/contactos, chat persistente con polling y votaciones consultivas.
Pendiente: WebSocket en tiempo real, adjuntos/moderación, vinculación de proveedores y push.
Ver [`FASE3.9-IMPLEMENTATION.md`](./FASE3.9-IMPLEMENTATION.md).

### Criterio de salida del MVP

- Se cumplen los criterios de éxito de `SPEC.md`.
- Suite de aislamiento multi-tenant en verde.
- Pruebas financieras y de redondeo en verde.
- Validación bilingüe completa.
- Revisión de seguridad sin hallazgos críticos/altos abiertos.
- Backup y restauración verificados.
- Documentación sincronizada.
- Aprobación del Product Owner y de la comunidad piloto.

## 6. FASE 4 — Validación y piloto

**Estado:** validación técnica automatizada completada; piloto real pendiente de legal, seguridad
de producción y usuarios. Ver [`FASE4-VALIDATION.md`](./FASE4-VALIDATION.md).

### Actividades

1. Pruebas E2E de flujos críticos.
2. Pruebas de autorización y aislamiento con datos realistas.
3. Pruebas de carga básicas.
4. Pruebas de accesibilidad.
5. Revisión de seguridad y dependencias.
6. Validación legal de RGPD/LPH.
7. Formación básica a usuarios del piloto.
8. Carga inicial de la comunidad piloto.
9. Periodo de acompañamiento y corrección de incidencias.
10. Verificación de backup/restore.
11. Revisión post-piloto y priorización.

### Criterio de salida

- Piloto operativo con usuarios reales.
- Incidencias críticas resueltas.
- Feedback recogido y priorizado.
- Decisión go/no-go para comercialización.

## 7. Post-MVP

Orden propuesto, sujeto a valor y validación:

1. **Sincronización bancaria PSD2** con proveedor autorizado.
2. **Push notifications** — implementado en v0.1.0 con Web Push/VAPID; ver
   [`PUSH-NOTIFICATIONS.md`](./PUSH-NOTIFICATIONS.md). Queda activar las claves en producción.
3. **Proveedores ampliado**: contratos, presupuestos, historial — implementado en v0.1.0
   (contratos y presupuestos con estado, notas e historial unificado).
4. **Votaciones formales** con validación legal.
5. **Inteligencia artificial**:
   - asistente documental;
   - resúmenes de reuniones;
   - análisis de gastos;
   - clasificación de incidencias;
   - búsqueda inteligente.
6. **Estadísticas avanzadas y BI** — implementado en v0.1.0: comparativa mensual, exportaciones
   CSV (evolución, miembros, incidencias, proveedores), gráfico mensual en el PDF localizado
   (es/ca/en) e informes programados por email (diario/semanal/mensual).
7. **Facturación electrónica e integración con gestoría**.
8. **Aplicación móvil nativa** si se justifica.
9. **Multi-idioma ampliado (inglés)** — implementado en v0.1.0: `messages_en` en backend y catálogo
   `en` en frontend (707 claves), selector ES/CA/EN y validación de idioma soportado.
10. **Organizaciones y múltiples administradores de fincas** — implementado en v0.1.0:
    organizaciones, vinculación de comunidades, administradores de organización y propagación
    automática de acceso como `COMMUNITY_ADMIN`.
11. **Planes de suscripción y facturación SaaS**.

Cada evolución requiere ADR, actualización de SPEC y análisis de seguridad/privacidad.

## 8. Dependencias críticas

| Dependencia | Fase | Impacto |
| --- | --- | --- |
| Validación del alcance MVP | FASE 1 | Bloquea diseño y planificación |
| Reglas financieras y plan contable | FASE 1 | Bloquea finanzas |
| Validación legal LPH/RGPD | FASE 1/4 | Bloquea votaciones y retención |
| Proveedor S3 y email | FASE 2 | Bloquea documentos y notificaciones |
| Dominio y cookies | FASE 2 | Bloquea autenticación definitiva |
| Regla de redondeo | FASE 3.3 | Bloquea cargos |
| Formato CSV bancario | FASE 3.4 | Bloquea importación |
| Carga inicial de datos | FASE 4 | Bloquea piloto |

## 9. Riesgos del roadmap

| Riesgo | Mitigación |
| --- | --- |
| Alcance MVP excesivo | MoSCoW, MVP+ separado, incrementos verticales |
| Finanzas más complejas de lo previsto | Validación temprana, prototipo de cálculo, asesoría |
| Requisitos legales tardíos | Revisión en FASE 1 |
| Deuda de seguridad | Controles desde FASE 2, pruebas continuas |
| Dependencia de la comunidad piloto | Planificar disponibilidad y validaciones |
| Cambios de alcance | SDD: actualizar SPEC antes de implementar |
| Falta de datos iniciales | Definir plantilla de carga y responsable |
| Bilingüismo incompleto | Claves desde el inicio, pruebas de traducciones |

## 10. Definition of Ready

Una historia está lista cuando:

- Tiene objetivo y valor.
- Tiene criterios de aceptación verificables.
- Dependencias identificadas.
- Diseño o contrato API disponible si aplica.
- Requisitos de seguridad e i18n identificados.
- Datos de prueba definidos.
- Estimación relativa acordada.

## 11. Definition of Done

Una tarea está terminada cuando:

- Cumple criterios de aceptación.
- Código implementado y revisado.
- Pruebas unitarias, integración y E2E aplicables en verde.
- Pruebas de autorización y aislamiento en verde.
- Textos en `es` y `ca`, sin literales incrustados.
- Documentación actualizada.
- Sin errores críticos ni vulnerabilidades altas sin mitigar.
- Cambios versionados en Git con commit descriptivo.
- Demostración o validación funcional realizada.

---

*Fin de docs/ROADMAP.md — v0.1.0 (FASE 0).*
