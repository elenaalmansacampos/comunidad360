# COMUNIDAD 360 — Backlog inicial

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Backlog inicial (FASE 0) |
| Fecha | 2026-11-19 |
| Prioridades | P0 = MVP obligatorio · P1 = MVP+ condicionado · P2 = Post-MVP |
| Estado de las historias | Todas pendientes de refinamiento y validación |

---

## 1. Criterios de priorización

1. **Valor para el piloto**: resuelve una necesidad operativa real.
2. **Riesgo**: seguridad, multi-tenancy y finanzas primero.
3. **Dependencia**: no se empieza lo que bloquea a otro módulo crítico.
4. **Complejidad**: incrementos verticales pequeños y verificables.
5. **Cumplimiento**: privacidad, LPH y auditoría no son opcionales.

## 2. Resumen de épicas

| Épica | Nombre | Prioridad | Fase |
| --- | --- | --- | --- |
| EP-01 | Plataforma, acceso y sesiones | P0 | 2 |
| EP-02 | Multi-tenancy, roles y auditoría | P0 | 2 |
| EP-03 | Comunidad y estructura física | P0 | 3.1 |
| EP-04 | Propietarios, inquilinos y coeficientes | P0 | 3.2 |
| EP-05 | Finanzas núcleo | P0 | 3.3 |
| EP-06 | Banca CSV y conciliación | P0 | 3.4 |
| EP-07 | Incidencias | P0 | 3.5 |
| EP-08 | Documentos | P0 | 3.6 |
| EP-09 | Reuniones | P0 | 3.7 |
| EP-10 | Notificaciones | P0 | 3.7 |
| EP-11 | Dashboard e informes | P0 | 3.8 |
| EP-12 | Internacionalización | P0 | 2–3 |
| EP-13 | Chat | P1 | 3.9 |
| EP-14 | Proveedores | P1 | 3.9 |
| EP-15 | Votaciones consultivas | P1 | 3.9 |
| EP-16 | Calidad, seguridad y operación | P0 | 2–4 |

---

## 3. EP-01 — Plataforma, acceso y sesiones

**US-001 — Bootstrap técnico del repositorio** · P0 · FASE 2 · Depende: —
Criterios: estructura backend/frontend; compilación reproducible; README con arranque; `.gitignore` sin secretos; CI ejecuta build y tests.
Pruebas: build backend y frontend; pipeline verde.

**US-002 — Entorno Docker de desarrollo** · P0 · FASE 2 · Depende: US-001
Criterios: PostgreSQL, S3 compatible y email de pruebas levantan con un comando; datos persistentes; configuración por entorno; sin credenciales reales.
Pruebas: arranque limpio y reinicio sin pérdida de datos.

**US-003 — Migraciones y datos semilla** · P0 · FASE 2 · Depende: US-002
Criterios: Flyway aplica desde cero; roles, permisos, idiomas y categorías iniciales sembrados; `ddl-auto=validate`; migración reproducible en CI.
Pruebas: Testcontainers aplica migraciones; esquema validado.

**US-004 — Login con email y contraseña** · P0 · FASE 2 · Depende: US-003
Criterios: credenciales válidas devuelven access token y cookie de refresh; credenciales inválidas devuelven error genérico; rate limiting; auditoría de intento.
Pruebas: unitarias, integración y E2E de login.

**US-005 — Aceptación de invitación** · P0 · FASE 2 · Depende: US-004
Criterios: token válido crea membresía y permite establecer contraseña; token caducado o usado falla; invitación asociada a comunidad y rol; auditoría.
Pruebas: invitación válida, caducada, reutilizada y de otra comunidad.

**US-006 — Verificación de email** · P0 · FASE 2 · Depende: US-005
Criterios: usuario no verificado no opera; token de un solo uso; reenvío limitado; email en idioma del destinatario.
Pruebas: verificación correcta y token inválido.

**US-007 — Recuperación de contraseña** · P0 · FASE 2 · Depende: US-004
Criterios: solicitud con respuesta genérica; token de un solo uso y caducidad; cambio de contraseña revoca sesiones; rate limiting.
Pruebas: flujo completo y abuso de tokens.

**US-008 — Sesiones, refresco y cierre global** · P0 · FASE 2 · Depende: US-004
Criterios: refresh rotatorio; reutilización detectada revoca la familia; logout local y global; access token corto; sesiones listadas en perfil si se aprueba.
Pruebas: rotación, reutilización, logout y expiración.

**US-009 — Perfil y preferencia de idioma** · P0 · FASE 2 · Depende: US-004
Criterios: ver y editar nombre; seleccionar `es` o `ca`; preferencia persiste entre sesiones; cambio aplicado en interfaz y notificaciones.
Pruebas: persistencia y cambio de locale.

## 4. EP-02 — Multi-tenancy, roles y auditoría

**US-010 — Comunidad y membresía base** · P0 · FASE 2 · Depende: US-003
Criterios: comunidad creada por `SUPER_ADMIN`; membresía activa une usuario y comunidad; un usuario puede tener varias membresías; roles por comunidad.
Pruebas: creación, múltiples membresías y estados.

**US-011 — TenantGuard y aislamiento** · P0 · FASE 2 · Depende: US-010
Criterios: toda ruta de tenant valida membresía y permiso; consultas filtran por `community_id`; accesos cruzados devuelven `403`/`404`; pruebas para cada módulo con datos.
Pruebas: suite de aislamiento multi-tenant.

**US-012 — Roles y permisos configurables** · P0 · FASE 2 · Depende: US-010
Criterios: catálogo de permisos; asignación por rol; ajustes por comunidad; permisos acumulables; denegación explícita prevalece; cambios auditados.
Pruebas: matriz positiva/negativa y conflictos.

**US-013 — Auditoría base** · P0 · FASE 2 · Depende: US-010
Criterios: eventos sensibles registrados con actor, tenant, acción y fecha; sin secretos; solo inserción; consulta restringida por `AUDIT_VIEW`.
Pruebas: eventos de login, rol y acceso de soporte.

**US-014 — Acceso de soporte auditado** · P0 · FASE 2 · Depende: US-011, US-013
Criterios: `SUPER_ADMIN` accede a comunidad con motivo; cada acceso genera auditoría; no omite validaciones; interfaz advierte del modo soporte.
Pruebas: acceso autorizado, motivo obligatorio y auditoría.

## 5. EP-03 — Comunidad y estructura física

**US-020 — Alta y configuración de comunidad** · P0 · FASE 3.1 · Depende: US-010
Criterios: nombre, CIF, dirección, localidad, provincia, código postal, zona horaria, idioma y moneda; validación; estado; auditoría.
Pruebas: alta válida, datos inválidos y permisos.

**US-021 — Gestión de edificios** · P0 · FASE 3.1 · Depende: US-020
Criterios: crear, editar, listar y archivar; pertenencia a comunidad; sin borrado físico si hay historial.
Pruebas: CRUD, aislamiento y archivado.

**US-022 — Gestión de unidades** · P0 · FASE 3.1 · Depende: US-021
Criterios: tipos vivienda/local/garaje/trastero/elemento común; código, planta, puerta, superficie y coeficiente; código único por comunidad; archivar.
Pruebas: validaciones, unicidad y aislamiento.

**US-023 — Elementos comunes** · P0 · FASE 3.1 · Depende: US-020
Criterios: crear, editar y archivar; vinculables a edificio o comunidad; visibles según permisos.
Pruebas: CRUD y aislamiento.

**US-024 — Validación de coeficientes** · P0 · FASE 3.1 · Depende: US-022
Criterios: suma de coeficientes de unidades participantes = 100 % con tolerancia configurable; error bloqueante con detalle; no se activa la comunidad si no cuadra.
Pruebas: suma exacta, tolerancia y suma incorrecta.

**US-025 — Carga inicial de unidades** · P1 · FASE 3.1 · Depende: US-024
Criterios: plantilla validada; importación con previsualización; errores por fila; idempotencia; auditoría.
Pruebas: fichero válido, inválido y duplicado.

## 6. EP-04 — Propietarios, inquilinos y coeficientes

**US-030 — Alta de titularidad** · P0 · FASE 3.2 · Depende: US-022
Criterios: asociar usuario y unidad; tipo de titularidad; porcentaje; vigencia; validación de suma ≤ 100 %; auditoría.
Pruebas: alta, solapamiento y suma superior.

**US-031 — Copropiedad** · P0 · FASE 3.2 · Depende: US-030
Criterios: varios titulares por unidad; porcentajes válidos; histórico; visualización clara; sin exponer datos innecesarios.
Pruebas: suma 100 %, suma parcial y errores.

**US-032 — Autorización de inquilinos** · P0 · FASE 3.2 · Depende: US-030
Criterios: vincular inquilino a unidad con vigencia; estado; autorizador; revocación; permisos limitados.
Pruebas: alta, caducidad, revocación y acceso.

**US-033 — Histórico de titularidad y alquiler** · P0 · FASE 3.2 · Depende: US-030, US-032
Criterios: vigencias pasadas consultables; no se sobrescribe historial; cargos usan coeficiente vigente; auditoría.
Pruebas: cambio de titular y consulta histórica.

**US-034 — Visibilidad de datos de propietarios** · P0 · FASE 3.2 · Depende: US-011, US-033
Criterios: propietario ve sus datos y cargos; no ve finanzas individuales de otros; contactos solo con permiso y finalidad; proveedor no accede.
Pruebas: matriz de visibilidad y accesos cruzados.

## 7. EP-05 — Finanzas núcleo

**US-040 — Ejercicios fiscales** · P0 · FASE 3.3 · Depende: US-020
Criterios: abrir ejercicio por año; fechas; estado abierto/cerrado; no duplicar año por comunidad; cierre bloquea cambios.
Pruebas: apertura, duplicado y cierre.

**US-041 — Presupuesto y partidas** · P0 · FASE 3.3 · Depende: US-040
Criterios: crear presupuesto; partidas de ingreso/gasto; importes decimales; totales calculados; estados; auditoría.
Pruebas: totales, validación y permisos.

**US-042 — Ingresos y gastos** · P0 · FASE 3.3 · Depende: US-040
Criterios: registrar apunte con fecha, categoría, descripción e importe; estado borrador/contabilizado; no editar contabilizado; reverso.
Pruebas: alta, contabilización, inmutabilidad y reverso.

**US-043 — Cuotas ordinarias** · P0 · FASE 3.3 · Depende: US-040, US-024
Criterios: crear cuota por periodo; importe total; reparto por coeficiente; instantánea de coeficiente; emisión; cargos cuadran.
Pruebas: reparto, cuadre, redondeo y aislamiento.

**US-044 — Derramas extraordinarias** · P0 · FASE 3.3 · Depende: US-043
Criterios: tipo extraordinario; concepto; periodo; reparto; trazabilidad; auditoría.
Pruebas: derrama por coeficiente y por reparto aprobado.

**US-045 — Cargos por unidad y estado de cobro** · P0 · FASE 3.3 · Depende: US-043
Criterios: cargo único por cuota/unidad; estados pendiente/parcial/pagado/cancelado; actualización transaccional con pagos.
Pruebas: pago total, parcial, exceso y anulación.

**US-046 — Facturas de proveedor** · P0 · FASE 3.3 · Depende: US-040
Criterios: número, fechas, base, impuestos, total; proveedor opcional; documento adjunto; estado; sin duplicados por proveedor/número.
Pruebas: alta, duplicado y adjunto.

**US-047 — Pagos** · P0 · FASE 3.3 · Depende: US-045, US-046
Criterios: registrar pago; vincular a cargo o factura; método; referencia; importe > 0; anulación con trazabilidad.
Pruebas: pago válido, exceso, anulación y aislamiento.

**US-048 — Saldo y cuotas pendientes** · P0 · FASE 3.3 · Depende: US-042, US-045, US-047
Criterios: saldo calculado desde apuntes contabilizados y pagos; cuotas pendientes por unidad; sin datos de otras unidades para propietario.
Pruebas: cuadre con escenario conocido y permisos.

**US-049 — Informes básicos** · P0 · FASE 3.3 · Depende: US-048
Criterios: informe por ejercicio y categoría; totales; exportación; permisos; auditoría de generación.
Pruebas: cifras correctas y acceso restringido.

**US-050 — Auditoría e inmutabilidad financiera** · P0 · FASE 3.3 · Depende: US-042, US-047
Criterios: toda operación financiera auditada; apuntes contabilizados inmutables; correcciones por reverso; sin borrado físico.
Pruebas: intento de edición/borrado denegado y reverso correcto.

**US-051 — Redondeo e idempotencia** · P0 · FASE 3.3 · Depende: US-043, US-047
Criterios: regla de redondeo documentada y probada; suma de cargos = total; `Idempotency-Key` evita duplicados; payload distinto devuelve conflicto.
Pruebas: casos límite de céntimos y reintentos.

## 8. EP-06 — Banca CSV y conciliación

**US-060 — Cuentas bancarias enmascaradas** · P0 · FASE 3.4 · Depende: US-020
Criterios: registrar cuenta y banco; IBAN enmascarado; sin credenciales; estado; permisos.
Pruebas: alta, visualización y aislamiento.

**US-061 — Importación CSV** · P0 · FASE 3.4 · Depende: US-060
Criterios: mapeo configurable; previsualización; validación de filas; errores por fila; registro de lote; auditoría.
Pruebas: CSV válido, columnas incorrectas y filas inválidas.

**US-062 — Detección de duplicados** · P0 · FASE 3.4 · Depende: US-061
Criterios: huella por movimiento; reimportación no duplica; informe de omitidos.
Pruebas: importación repetida y movimientos similares.

**US-063 — Conciliación** · P0 · FASE 3.4 · Depende: US-047, US-062
Criterios: vincular movimiento con pago o apunte; importe coincidente; deshacer conciliación; estados; auditoría.
Pruebas: conciliación correcta, descuadre y deshacer.

## 9. EP-07 — Incidencias

**US-070 — Crear incidencia** · P0 · FASE 3.5 · Depende: US-022, US-032
Criterios: título, descripción, categoría, ubicación; estado inicial abierto; autor; auditoría.
Pruebas: alta válida, validación y permisos.

**US-071 — Fotografías** · P0 · FASE 3.5 · Depende: US-070, US-080
Criterios: adjuntar imágenes; límites y tipos; análisis; visibilidad según incidencia; descarga firmada.
Pruebas: subida válida, tipo no permitido y acceso.

**US-072 — Asignación, prioridad y estados** · P0 · FASE 3.5 · Depende: US-070
Criterios: asignar responsable/proveedor; prioridad; transiciones válidas; histórico; notificación.
Pruebas: transiciones, permisos e histórico.

**US-073 — Comentarios y seguimiento** · P0 · FASE 3.5 · Depende: US-070
Criterios: comentar; autor y fecha; comentario interno solo para gestores; sin edición silenciosa.
Pruebas: visibilidad interna/externa y auditoría.

**US-074 — Visibilidad de incidencias** · P0 · FASE 3.5 · Depende: US-034, US-070
Criterios: propietario/inquilino ven las suyas; gestores ven todas; proveedor solo asignadas; sin fugas.
Pruebas: matriz de acceso y aislamiento.

## 10. EP-08 — Documentos

**US-080 — Subida y metadatos** · P0 · FASE 3.6 · Depende: US-020
Criterios: categoría, título, descripción; archivo validado; hash; almacenamiento en S3; metadatos en base; auditoría.
Pruebas: subida válida, tamaño excedido y fallo de almacenamiento.

**US-081 — Permisos y visibilidad** · P0 · FASE 3.6 · Depende: US-080, US-011
Criterios: visibilidad comunidad/órgano/propietarios/rol/privado; comprobación backend; propietario no ve documentos restringidos.
Pruebas: matriz de visibilidad y accesos cruzados.

**US-082 — Versionado** · P0 · FASE 3.6 · Depende: US-080
Criterios: nuevas versiones sin sobrescribir; versión actual; histórico; descarga de versión autorizada.
Pruebas: dos versiones y permisos.

**US-083 — Descarga firmada y auditoría** · P0 · FASE 3.6 · Depende: US-080, US-081
Criterios: URL firmada de corta duración; permiso previo; registro de descarga; caducidad verificada.
Pruebas: URL válida, caducada y sin permiso.

**US-084 — Análisis y cuarentena** · P0 · FASE 3.6 · Depende: US-080
Criterios: archivo no disponible hasta análisis limpio; infectado bloqueado; error registrado; reintento controlado.
Pruebas: archivo limpio, infectado simulado y error.

## 11. EP-09 — Reuniones

**US-090 — Convocatoria y orden del día** · P0 · FASE 3.7 · Depende: US-020, US-080
Criterios: título, tipo, fecha, hora, lugar; puntos ordenados; estado; documento adjunto; auditoría.
Pruebas: creación, edición y permisos.

**US-091 — Asistencia y representación** · P0 · FASE 3.7 · Depende: US-090
Criterios: registrar presente/online/representado/ausente; representante válido; coeficiente; sin duplicados.
Pruebas: asistencia, representación y duplicado.

**US-092 — Acuerdos** · P0 · FASE 3.7 · Depende: US-091
Criterios: registrar acuerdo por punto; votos y coeficientes; estado adoptado; trazabilidad.
Pruebas: cálculo y validación.

**US-093 — Acta y notificación** · P0 · FASE 3.7 · Depende: US-090, US-092, US-101
Criterios: adjuntar acta; vincular a reunión; notificar; visible según permisos; auditoría.
Pruebas: acta visible, notificación e idioma.

## 12. EP-10 — Notificaciones

**US-100 — Notificaciones internas** · P0 · FASE 3.7 · Depende: US-004
Criterios: campana; listado; no leídas; marcar leída; permisos; sin datos sensibles innecesarios.
Pruebas: creación, lectura y aislamiento.

**US-101 — Notificaciones por email** · P0 · FASE 3.7 · Depende: US-100
Criterios: envío asíncrono; reintentos; registro de estado; plantilla; sin datos financieros sensibles en asunto.
Pruebas: envío correcto, fallo y reintento.

**US-102 — Preferencias de notificación** · P0 · FASE 3.7 · Depende: US-100
Criterios: activar/desactivar por categoría y canal; respetar preferencias salvo comunicaciones obligatorias; persistencia.
Pruebas: preferencia respetada y excepción legal.

**US-103 — Idioma del destinatario** · P0 · FASE 3.7 · Depende: US-009, US-101
Criterios: email y notificación en `es` o `ca`; fallback a `es`; parámetros traducidos; no traducir contenido de usuario.
Pruebas: ambos idiomas y fallback.

## 13. EP-11 — Dashboard e informes

**US-110 — Dashboard por rol** · P0 · FASE 3.8 · Depende: US-011, US-048
Criterios: cada rol ve widgets permitidos; sin datos de otras comunidades; estados de carga y vacío; responsive.
Pruebas: matriz de roles y aislamiento.

**US-111 — Indicadores financieros** · P0 · FASE 3.8 · Depende: US-048
Criterios: saldo, ingresos, gastos, cuotas pendientes; cifras correctas; formatos locales; propietario solo sus datos.
Pruebas: cuadre y permisos.

**US-112 — Widgets de incidencias y reuniones** · P0 · FASE 3.8 · Depende: US-070, US-090
Criterios: últimas incidencias; próximas reuniones; acceso al detalle; visibilidad por rol.
Pruebas: datos correctos y permisos.

**US-113 — Informe PDF básico** · P1 · FASE 3.8 · Depende: US-049, US-110
Criterios: fecha, dominio/comunidad, resultados por módulo, incidencias y recomendaciones; permisos; marca de agua/versión; auditoría.
Pruebas: contenido, acceso y generación.

## 14. EP-12 — Internacionalización

**US-120 — Configuración i18n español/catalán** · P0 · FASE 2 · Depende: US-001
Criterios: i18next configurado; archivos `es` y `ca`; claves semánticas; sin literales en componentes base.
Pruebas: cambio de idioma y claves existentes.

**US-121 — Selector y persistencia de idioma** · P0 · FASE 2 · Depende: US-009, US-120
Criterios: selector accesible; cambio inmediato; persistencia en usuario; fallback; `lang` del documento actualizado.
Pruebas: persistencia entre sesiones y dispositivos.

**US-122 — Formatos regionales** · P0 · FASE 3 · Depende: US-120
Criterios: fechas, números, porcentajes e importes con `Intl`; `es-ES`/`ca-ES`; EUR; sin hardcode.
Pruebas: snapshots por locale y casos límite.

**US-123 — i18n backend y emails** · P0 · FASE 3.7 · Depende: US-101, US-103
Criterios: errores con `messageKey`; plantillas de email por locale; fallback; parámetros.
Pruebas: error en ambos idiomas y email en catalán.

**US-124 — Calidad de traducciones** · P0 · FASE 3–4 · Depende: US-120
Criterios: revisión humana del catalán administrativo; prueba que detecta claves faltantes o huérfanas; glosario aplicado.
Pruebas: CI de claves y revisión documentada.

## 15. EP-13 — Chat (MVP+)

**US-130 — Canales** · P1 · FASE 3.9 · Depende: US-011, US-012
Criterios: canal general único; canales temáticos; privados; membresía; archivado; permisos.
Pruebas: creación, acceso y aislamiento.

**US-131 — Mensajes en tiempo real** · P1 · FASE 3.9 · Depende: US-130
Criterios: WebSocket autenticado; mensajes persistidos; historial paginado; reconexión; sin XSS.
Pruebas: envío, recepción, reconexión y seguridad.

**US-132 — Conversaciones privadas** · P1 · FASE 3.9 · Depende: US-131
Criterios: solo participantes; creación controlada; sin fuga de historial; notificación.
Pruebas: tercero no accede y participantes sí.

**US-133 — Adjuntos y moderación** · P1 · FASE 3.9 · Depende: US-131, US-084
Criterios: adjuntos validados; descarga firmada; ocultar mensaje; expulsar; auditoría; notificación.
Pruebas: adjunto válido, moderación y permisos.

## 16. EP-14 — Proveedores (MVP+)

**US-140 — Directorio de proveedores** · P1 · FASE 3.9 · Depende: US-020
Criterios: nombre, CIF, categoría, contacto, estado; alta/edición; permisos; auditoría.
Pruebas: CRUD y aislamiento.

**US-141 — Contactos** · P1 · FASE 3.9 · Depende: US-140
Criterios: múltiples contactos; principal; datos de contacto; visibilidad limitada.
Pruebas: alta y permisos.

**US-142 — Vinculación con incidencias y facturas** · P1 · FASE 3.9 · Depende: US-140, US-072, US-046
Criterios: asignar proveedor a incidencia; asociar factura; proveedor solo ve lo asignado.
Pruebas: visibilidad del proveedor y aislamiento.

## 17. EP-15 — Votaciones consultivas (MVP+)

**US-150 — Propuesta y opciones** · P1 · FASE 3.9 · Depende: US-090, US-012
Criterios: título, descripción, opciones, periodo; estado; aviso de votación consultiva; permisos.
Pruebas: creación, validación y aviso visible.

**US-151 — Elegibilidad y coeficientes** · P1 · FASE 3.9 · Depende: US-150, US-030
Criterios: participantes según unidad/titularidad; coeficiente; representación; sin duplicados.
Pruebas: elegibilidad y representación.

**US-152 — Emisión de voto** · P1 · FASE 3.9 · Depende: US-151
Criterios: un voto por elegible; solo durante periodo; opción válida; registro; no se puede cambiar salvo regla definida.
Pruebas: voto válido, duplicado, fuera de plazo y sin permiso.

**US-153 — Quórum, resultados y aviso legal** · P1 · FASE 3.9 · Depende: US-152
Criterios: resultados por voto y coeficiente; quórum; estado; aviso de no vinculación jurídica; exportación.
Pruebas: cálculo, quórum y visualización del aviso.

## 18. EP-16 — Calidad, seguridad y operación

**US-160 — Suite de autorización** · P0 · FASE 2–4 · Depende: US-012
Criterios: pruebas positivas y negativas por rol, recurso y tenant; cobertura de endpoints críticos; CI bloqueante.
Pruebas: ejecución continua y revisión.

**US-161 — Suite de aislamiento multi-tenant** · P0 · FASE 3–4 · Depende: US-011
Criterios: dos comunidades; intentos de acceso cruzado en cada módulo; listados filtrados; FKs compuestas verificadas.
Pruebas: suite completa en CI.

**US-162 — E2E de flujos críticos** · P0 · FASE 4 · Depende: US-004, US-048, US-070, US-083, US-090
Criterios: login, invitación, alta de unidad, cuota/pago, incidencia, documento y reunión; ambos idiomas; móvil y escritorio.
Pruebas: Playwright en staging.

**US-163 — Backup y restauración** · P0 · FASE 4 · Depende: US-002
Criterios: backup diario; cifrado; restauración documentada; prueba real; RPO/RTO medidos.
Pruebas: simulacro de restauración.

**US-164 — CI/CD y seguridad de cadena de suministro** · P0 · FASE 2–4 · Depende: US-001
Criterios: build, tests, lint, SCA, SAST, escaneo de imagen; bloqueo ante críticos; artefactos versionados.
Pruebas: pipeline completo y fallo controlado.

**US-165 — Rendimiento y carga básica** · P1 · FASE 4 · Depende: US-048, US-070, US-083
Criterios: objetivos de latencia medidos; listados paginados; sin N+1 evidente; informe de resultados.
Pruebas: escenario de comunidad piloto.

**US-166 — Auditoría de accesibilidad** · P0 · FASE 4 · Depende: US-110
Criterios: revisión WCAG 2.1 AA; teclado; contraste; lectores de pantalla; correcciones críticas.
Pruebas: auditoría manual y automatizada.

**US-167 — Revisión legal RGPD/LPH** · P0 · FASE 1/4 · Depende: Q-03, Q-09
Criterios: bases jurídicas; información; retención; votaciones; contratos de encargado; conclusiones documentadas.
Pruebas: informe legal y checklist.

**US-168 — Onboarding y carga del piloto** · P0 · FASE 4 · Depende: US-025, US-163
Criterios: datos de comunidad cargados; usuarios invitados; formación básica; checklist de salida; incidencias registradas.
Pruebas: validación con la comunidad piloto.

---

## 19. Fuera del backlog actual

Estas capacidades no se descomponen todavía y requieren SPEC/ADR propios:

- IA documental, resúmenes y clasificación.
- Sincronización bancaria PSD2.
- Pagos y transferencias.
- Push notifications.
- Votaciones formales.
- Facturación electrónica e integración con gestoría.
- Analítica avanzada y BI.
- Aplicación móvil nativa.
- Suscripciones y facturación SaaS.
- Inglés y otros idiomas.

## 20. Próximo refinamiento

1. Validar Q-01 a Q-18.
2. Estimar historias P0 con el equipo.
3. Convertir US-001 a US-011 en tareas técnicas de FASE 2.
4. Añadir criterios de aceptación detallados (Given/When/Then) para las historias que se implementen.
5. Revisar dependencias y orden de los incrementos.
6. Confirmar el alcance MVP+ (chat, proveedores, votaciones consultivas, PDF).
7. Definir datos de prueba y escenarios del piloto.

---

*Fin de docs/BACKLOG.md — v0.1.0 (FASE 0).*
