# COMUNIDAD 360 — FASE 1: Diseño detallado

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Diseño generado; pendiente de validación |
| Fecha | 2026-11-19 |
| Fase | FASE 1 — Diseño |
| Código de producción | No incluido |

---

## 1. Objetivo

Convertir la especificación de FASE 0 en decisiones de diseño verificables antes de iniciar la
implementación:

- Resolver o acotar las preguntas `Q-01` a `Q-18`.
- Fijar la navegación y los wireframes de los flujos críticos.
- Definir el contrato OpenAPI inicial del núcleo del MVP.
- Validar el modelo de datos y las reglas financieras con el piloto.
- Confirmar el alcance del MVP y del MVP+.

Este documento contiene **propuestas**, no decisiones definitivas. Las propuestas marcadas como
"requiere validación" no deben implementarse como definitivas sin aprobación del Product Owner y, cuando
corresponda, de asesoría legal o contable.

## 2. Entregables de esta iteración

| Entregable | Estado |
| --- | --- |
| Propuestas de decisión `Q-01` a `Q-18` | Generadas; pendientes de validación |
| Navegación y wireframes | [`WIREFRAMES.md`](./WIREFRAMES.md) |
| Contrato OpenAPI v0 | [`openapi.yaml`](./openapi.yaml) |
| Modelo de datos inicial | [`DATABASE.md`](./DATABASE.md) |
| Design System inicial | [`DESIGN-SYSTEM.md`](./DESIGN-SYSTEM.md) |
| Backlog inicial | [`BACKLOG.md`](./BACKLOG.md) |
| Revisión legal | Pendiente |

## 3. Propuestas de decisión

### Q-01 — Alta de usuarios

**Propuesta:** MVP exclusivamente por invitación. No habrá autoregistro abierto.

- La invitación identifica comunidad y rol inicial.
- La aceptación crea el usuario o vincula uno existente.
- Si más adelante se habilita solicitud pública, se añadirá un flujo separado de `access_request` con
  verificación y aprobación.
- Motivo: control de acceso, prevención de abuso y coherencia con comunidades cerradas.

**Validación:** Product Owner y comunidad piloto.
**Impacto si cambia:** modelo de registro, antiabuso, emails y pantallas de alta.

### Q-02 — Rol del administrador de fincas

**Propuesta:** `COMMUNITY_ADMIN` representa al administrador/a de la comunidad o de fincas y puede tener
ese rol en varias comunidades. `SUPER_ADMIN` es personal interno de la plataforma.

- No se crea un rol nuevo en el MVP.
- Un usuario con `COMMUNITY_ADMIN` en varias comunidades cambia de comunidad desde la interfaz.
- Los permisos siguen siendo por comunidad.

**Validación:** Product Owner.
**Impacto si cambia:** modelo de roles, navegación y matriz de permisos.

### Q-03 — Validez legal de votaciones

**Propuesta:** En MVP+ solo votaciones **consultivas**, con aviso visible de que no constituyen
automáticamente un acuerdo jurídicamente válido. Las votaciones formales quedan post-MVP y requieren
revisión legal específica.

**Validación:** Asesoría legal y comunidad piloto.
**Impacto si cambia:** diseño de votaciones, actas, quórums y responsabilidad.

### Q-04 — Integración bancaria

**Propuesta:** MVP con importación CSV de solo lectura. La sincronización PSD2 queda post-MVP. No se
almacenan credenciales bancarias ni se ejecutan pagos.

**Validación:** Product Owner y comunidad piloto.
**Impacto si cambia:** esfuerzo de integración, seguridad y contratos con proveedor.

### Q-05 — Proveedores de infraestructura

**Propuesta:** usar interfaces genéricas y elegir proveedor concreto en FASE 2:

- Email: SMTP estándar; en desarrollo Mailpit; en producción un proveedor transaccional.
- Almacenamiento: S3 compatible; en desarrollo S3Mock; en producción un proveedor con cifrado y región
  adecuada.
- Evitar dependencias propietarias en el código de dominio.

**Validación:** DevSecOps y Product Owner.
**Impacto si cambia:** configuración, costes, contratos de encargado de tratamiento y despliegue.

### Q-06 — Dominio y cookies

**Propuesta:** MVP en un dominio único con selector de comunidad. Subdominios por comunidad quedan para
post-MVP.

- Cookie de refresh `HttpOnly`, `Secure` en producción y `SameSite=Lax`.
- Validación de `Origin` en operaciones con cookie.
- CORS con lista blanca.
- Si se adoptan subdominios, revisar `SameSite` y el ámbito de cookies.

**Validación:** DevSecOps, Security Lead y Product Owner.
**Impacto si cambia:** autenticación, CORS, CSRF y enlaces.

### Q-07 — Plan de categorías financieras

**Propuesta inicial** para validar con la comunidad piloto y asesoría contable:

**Ingresos**

- Cuotas ordinarias.
- Derramas.
- Ingresos financieros.
- Otros ingresos.

**Gastos**

- Administración.
- Limpieza.
- Jardinería.
- Mantenimiento general.
- Ascensor.
- Seguros.
- Suministros.
- Reparaciones y obras.
- Servicios profesionales.
- Otros gastos.

Las categorías serán configurables por comunidad y no se codificarán como enums inmutables.

**Validación:** Comunidad piloto y asesoría contable.
**Impacto si cambia:** informes, presupuestos y conciliación.

### Q-08 — Redondeo y reparto de céntimos

**Propuesta:**

- Coeficientes con 6 decimales.
- Cargos calculados con precisión intermedia y redondeados a 2 decimales.
- Método de **resto mayor** para distribuir los céntimos sobrantes.
- La suma de cargos debe ser exactamente igual al total de la cuota o derrama.
- La regla se implementará en servidor y se cubrirá con pruebas de casos límite.

**Validación:** Asesoría contable y comunidad piloto.
**Impacto si cambia:** cálculo de cargos, cuadres y conciliación.

### Q-09 — Retención documental y contable

**Propuesta sujeta a revisión legal:**

- Documentación contable y financiera: mínimo 6 años.
- Documentos oficiales de la comunidad: según estatutos y normativa aplicable.
- Datos personales: supresión o anonimización cuando desaparezca la finalidad, con excepciones legales.
- No borrar apuntes contabilizados ni auditoría financiera.

**Validación:** Asesoría legal.
**Impacto si cambia:** borrado, anonimización, backups y almacenamiento.

### Q-10 — Visibilidad entre propietarios

**Propuesta:** el directorio de la comunidad muestra nombre, unidad, coeficiente y rol. No muestra
teléfono, email ni datos financieros de otras unidades. El contacto se realiza mediante la plataforma o
los canales habilitados por la comunidad.

**Validación:** Asesoría legal y comunidad piloto.
**Impacto si cambia:** privacidad, pantallas de directorio y permisos.

### Q-11 — Chat en el MVP

**Propuesta:** chat como MVP+ (P1). El MVP obligatorio prioriza finanzas, incidencias, documentos,
reuniones y notificaciones.

**Validación:** Product Owner y comunidad piloto.
**Impacto si cambia:** alcance, esfuerzo y fecha del piloto.

### Q-12 — Notificaciones push

**Propuesta:** post-MVP. MVP con notificaciones internas y email.

**Validación:** Product Owner.
**Impacto si cambia:** proveedor push, permisos del navegador y esfuerzo.

### Q-13 — Límites y tipos de archivo

**Propuesta inicial:**

- Documentos: máximo 25 MB.
- Imágenes de incidencias: máximo 10 MB.
- Adjuntos de chat: máximo 10 MB.
- Tipos permitidos: PDF, JPEG, PNG, WebP, DOCX, XLSX, TXT.
- Prohibidos: ejecutables, HTML, SVG activo y archivos comprimidos no inspeccionados.
- Límites configurables por comunidad.

**Validación:** DevSecOps y Product Owner.
**Impacto si cambia:** almacenamiento, antivirus y experiencia de usuario.

### Q-14 — Representación en reuniones y votaciones

**Propuesta:** permitir representación con autorización documentada adjunta, validada por secretaría. El
sistema registra representante, representado, vigencia y documento. No se presume validez legal sin
revisión.

**Validación:** Asesoría legal.
**Impacto si cambia:** reuniones, votaciones, actas y documentos.

### Q-15 — Visibilidad de proveedores

**Propuesta:** los propietarios ven nombre, categoría y datos de contacto del proveedor solo cuando está
asignado a una incidencia visible para ellos. No ven contratos, presupuestos ni facturas.

**Validación:** Product Owner y asesoría legal.
**Impacto si cambia:** permisos y pantallas de proveedores.

### Q-16 — Facturación electrónica y gestoría

**Propuesta:** fuera del MVP. Exportación CSV/PDF y registro interno de facturas. La integración con
gestoría o facturación electrónica queda post-MVP.

**Validación:** Comunidad piloto y asesoría contable.
**Impacto si cambia:** alcance financiero e integraciones.

### Q-17 — Idioma por defecto

**Propuesta:** prioridad `preferencia del usuario` → `idioma por defecto de la comunidad` → `es`.

**Validación:** Product Owner.
**Impacto si cambia:** i18n, onboarding y notificaciones.

### Q-18 — Retención de logs y auditoría

**Propuesta inicial:**

- Logs técnicos: 90 días.
- Auditoría de operaciones financieras y de permisos: 6 años.
- Resto de auditoría: 2 años.
- Sujeto a revisión legal y a la política de privacidad.

**Validación:** Asesoría legal y DevSecOps.
**Impacto si cambia:** almacenamiento, costes y borrado.

## 4. Contradicciones y estado

| ID | Estado propuesto |
| --- | --- |
| C-01 Nombre del producto | Usar "COMUNIDAD 360" como producto y "ComunidadCat 360" como repositorio; confirmar. |
| C-02 SPEC inicial solo i18n | Resuelto: SPEC ampliado. |
| C-03 Registro por invitación | Alineado con Q-01. |
| C-04 Administrador de fincas | Alineado con Q-02. |
| C-05 Votaciones | Consultivas en MVP+; formales post-MVP. |
| C-06 Estadísticas | Dashboard e informes básicos; BI post-MVP. |
| C-07 Push | Post-MVP. |
| C-08 Informes PDF | P1 sujeto a validación; no bloquea MVP. |
| C-09 Redis | Opcional. |
| C-10 Chat | MVP+ condicionado a Q-11. |
| C-11 Banca | CSV en MVP; PSD2 post-MVP. |
| C-12 Invitación y recuperación | Compatibles. |
| C-13 i18n y documentos | No se traduce contenido de usuario ni documentos oficiales. |
| C-14 Transparencia y morosidad | Sin listas de morosos; solo datos individuales al titular. |
| C-15 Moneda y zona horaria | EUR y Europe/Madrid por defecto, configurables. |

## 5. Flujos críticos para wireframes

1. Aceptar invitación y crear cuenta.
2. Iniciar sesión y seleccionar comunidad.
3. Configurar comunidad, edificios y unidades.
4. Dar de alta propietarios e inquilinos.
5. Consultar dashboard según rol.
6. Crear cuota, emitir cargos y registrar pago.
7. Importar CSV bancario y conciliar.
8. Crear y seguir una incidencia.
9. Subir documentos y controlar acceso.
10. Convocar reunión, registrar asistencia y adjuntar acta.
11. Consultar notificaciones y cambiar idioma.
12. Crear y votar una consulta (MVP+).
13. Usar el chat (MVP+).

Ver [`WIREFRAMES.md`](./WIREFRAMES.md).

## 6. Contrato API v0

El contrato [`openapi.yaml`](./openapi.yaml) cubre el núcleo del MVP:

- Autenticación, sesiones y perfil.
- Comunidades, edificios, unidades y membresías.
- Finanzas: resumen, cuotas, cargos y pagos.
- Incidencias.
- Documentos y URL de descarga.
- Reuniones.
- Notificaciones y preferencias.
- Votaciones como propuesta MVP+.

Quedan fuera de esta v0:

- Banca CSV detallada.
- Chat/WebSocket.
- Proveedores.
- Auditoría.
- IA.
- Administración de plataforma.

El contrato es una propuesta de diseño; los endpoints definitivos se validarán antes de implementar.

Validación técnica realizada:

```text
npx @redocly/cli lint docs/openapi.yaml
→ OpenAPI válido
→ 1 warning: servidor localhost (esperado en desarrollo)
```

## 7. Validaciones de modelo de datos

Antes de FASE 2/3:

1. Confirmar la suma de coeficientes y su tolerancia.
2. Confirmar la regla de titularidad compartida y vigencias.
3. Confirmar categorías financieras y regla de redondeo.
4. Revisar la referencia circular `document` ↔ `document_version`.
5. Validar la política de retención y anonimización.
6. Confirmar el uso de `community_id` y FKs compuestas en todos los módulos.
7. Revisar índices con datos representativos del piloto.
8. Decidir si se usará RLS en producción.

## 8. Validaciones de Design System

- Validar contraste de la paleta con herramienta automática.
- Elegir tipografía definitiva y licencia.
- Elegir set de iconos autoalojado.
- Aprobar wireframes mobile-first.
- Validar navegación inferior en móvil y sidebar en escritorio.
- Revisar accesibilidad de formularios, tablas y gráficos.
- Confirmar el glosario catalán con revisión humana.

## 9. Criterios de salida de FASE 1

FASE 1 se considerará cerrada cuando:

1. `Q-01` a `Q-18` estén validadas, acotadas o explícitamente pospuestas.
2. Las contradicciones `C-01` a `C-15` tengan tratamiento aprobado.
3. Los wireframes de los flujos críticos estén revisados.
4. El contrato OpenAPI v0 esté validado y sin contradicciones con `SPEC.md`.
5. El modelo de datos esté validado para el piloto.
6. Las reglas financieras estén confirmadas por la comunidad y asesoría contable.
7. La revisión legal inicial esté planificada.
8. El backlog P0 esté refinado y estimado.
9. No existan decisiones bloqueantes para FASE 2.

## 10. Decisiones que bloquean FASE 2

| Decisión | Motivo |
| --- | --- |
| Q-01 Alta por invitación | Flujo de usuarios y emails |
| Q-02 Rol del administrador | Permisos y navegación |
| Q-05 Email y S3 | Infraestructura y configuración |
| Q-06 Dominio y cookies | Autenticación, CORS y CSRF |
| Q-13 Límites de archivo | Configuración de documentos |
| Q-17 Idioma por defecto | i18n y onboarding |
| Q-18 Retención de logs | Operación y privacidad |

Las decisiones `Q-07`, `Q-08`, `Q-09`, `Q-14` y `Q-15` bloquean módulos concretos de FASE 3, no el
esqueleto de FASE 2.

## 11. Próximos pasos

1. Revisar y aprobar las propuestas de este documento.
2. Validar wireframes con Product Owner y comunidad piloto.
3. Revisar `openapi.yaml` con Backend y Frontend.
4. Encargar revisión legal de `Q-03`, `Q-09`, `Q-10`, `Q-14`, `Q-15` y `Q-18`.
5. Validar `Q-07` y `Q-08` con asesoría contable.
6. Refinar las historias P0 con criterios Given/When/Then.
7. Cerrar FASE 1 y preparar FASE 2 (scaffolding, Docker, Flyway, autenticación y multi-tenancy).

---

*Fin de docs/FASE1-DESIGN.md — v0.1.0.*
