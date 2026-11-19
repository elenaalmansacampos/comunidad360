# COMUNIDAD 360 — Especificación funcional

| Campo | Valor |
| --- | --- |
| Producto | COMUNIDAD 360 |
| Nombre de trabajo del repositorio | ComunidadCat 360 |
| Versión | 0.1.0 |
| Estado | Borrador para validación (FASE 0) |
| Fecha | 2026-11-19 |
| Metodología | Spec Driven Development (SDD) |
| Arquitectura | Monolito modular: React + Spring Boot + PostgreSQL |
| Idiomas MVP | Español (`es`) y catalán (`ca`) |

> Este documento es la fuente de verdad funcional. Cualquier cambio de alcance debe reflejarse aquí
> antes de implementarse. Los detalles técnicos se desarrollan en `ARCHITECTURE.md`, `DATABASE.md`,
> `API.md`, `SECURITY.md` y `DESIGN-SYSTEM.md`.

---

## 1. Resumen ejecutivo

COMUNIDAD 360 es una plataforma SaaS multi-comunidad para la gestión de comunidades de propietarios.
Permite administrar la estructura física del inmueble, las personas y sus roles, la comunicación, las
finanzas, la documentación, las incidencias, las reuniones y las votaciones, con aislamiento estricto de
datos entre comunidades.

El primer despliegue se hará para una comunidad piloto real en Barcelona. El producto debe poder
comercializarse posteriormente a comunidades y administradores de fincas.

El MVP se centra en operar una comunidad real con seguridad, trazabilidad y bilingüe español/catalán.
La inteligencia artificial, la integración bancaria automática y los pagos quedan fuera del MVP.

## 2. Objetivos

1. Digitalizar la operación básica de una comunidad de propietarios.
2. Dar transparencia controlada sobre cuentas, cuotas, gastos, incidencias y documentos.
3. Facilitar la comunicación entre propietarios, inquilinos y órganos de gobierno.
4. Garantizar aislamiento multi-tenant y protección de datos personales y financieros.
5. Ofrecer una experiencia mobile-first comprensible para personas con baja alfabetización digital.
6. Permitir la incorporación de nuevas comunidades y nuevos idiomas sin rediseñar la arquitectura.
7. Preparar una evolución comercial SaaS sin introducir microservicios prematuros.

## 3. Actores y roles

### 3.1. Roles

| Rol | Ámbito | Descripción |
| --- | --- | --- |
| `SUPER_ADMIN` | Plataforma | Personal interno de COMUNIDAD 360. Acceso transversal auditado. No participa en la vida comunitaria salvo soporte. |
| `COMMUNITY_ADMIN` | Comunidad | Administrador/a de la comunidad o administrador/a de fincas. Configura la comunidad y sus permisos. |
| `PRESIDENT` | Comunidad | Presidencia de la comunidad. Acceso amplio a información y órganos de gobierno. |
| `SECRETARY` | Comunidad | Secretaría. Gestión documental, convocatorias, actas y votaciones. |
| `TREASURER` | Comunidad | Tesorería. Gestión financiera y conciliación. |
| `OWNER` | Unidad | Propietario/a de una o varias unidades. |
| `TENANT` | Unidad | Inquilino/a autorizado/a de una unidad. |
| `PROVIDER` | Comunidad | Proveedor externo. Acceso limitado a sus incidencias, documentos y facturas. |

Un usuario puede pertenecer a varias comunidades con roles distintos. Un usuario puede ser `OWNER` y
`TENANT` en unidades diferentes de la misma comunidad.

### 3.2. Modelo de permisos

- Los roles son códigos estables; los permisos son un catálogo granular.
- La asignación rol → permiso es configurable por comunidad.
- Los permisos de una membresía son acumulables (unión de roles y concesiones directas).
- Las denegaciones explícitas prevalecen sobre las concesiones.
- `SUPER_ADMIN` queda fuera de la configuración por comunidad y se audita siempre.
- El backend es la única autoridad de autorización. El frontend solo oculta acciones no permitidas.

Catálogo inicial de permisos:

```text
COMMUNITY_VIEW, COMMUNITY_MANAGE
MEMBER_VIEW, MEMBER_MANAGE, MEMBER_INVITE
UNIT_VIEW, UNIT_MANAGE, OWNERSHIP_MANAGE
FINANCE_VIEW, FINANCE_MANAGE, INVOICE_MANAGE, REPORT_VIEW
BANK_ACCOUNT_VIEW, BANK_IMPORT, BANK_RECONCILE
INCIDENT_CREATE, INCIDENT_VIEW_ALL, INCIDENT_VIEW_OWN, INCIDENT_MANAGE
DOCUMENT_VIEW, DOCUMENT_UPLOAD, DOCUMENT_MANAGE
MEETING_VIEW, MEETING_MANAGE
VOTE_VIEW, VOTE_MANAGE, VOTE_CAST
CHAT_VIEW, CHAT_MODERATE
PROVIDER_VIEW, PROVIDER_MANAGE
NOTIFICATION_MANAGE
AUDIT_VIEW
```

La matriz detallada rol → permiso se mantendrá en `SECURITY.md` y será validada con pruebas de
autorización.

## 4. Alcance

### 4.1. Alcance del MVP (comunidad piloto)

| Capacidad | MVP | Notas |
| --- | --- | --- |
| Autenticación, invitaciones, recuperación de contraseña | Sí | Registro solo por invitación |
| Multi-tenancy y roles por comunidad | Sí | Aislamiento probado en backend |
| Comunidades, edificios, unidades y elementos comunes | Sí | Viviendas, locales, garajes, trasteros |
| Propietarios, inquilinos, coeficientes e histórico | Sí | Titularidad compartida |
| Finanzas: cuentas, ejercicio, presupuesto, ingresos, gastos, cuotas, derramas, facturas, pagos, saldo | Sí | Núcleo del MVP |
| Importación bancaria CSV y conciliación básica | Sí | Solo lectura |
| Incidencias con fotos, estados y comentarios | Sí | |
| Documentos con permisos, versiones y auditoría | Sí | |
| Reuniones: convocatoria, orden del día, asistencia y acta | Sí | |
| Notificaciones internas y email | Sí | Idioma del destinatario |
| Dashboard por rol | Sí | Datos básicos |
| Chat general, temático y privado | MVP+ | Se implementa si el piloto lo confirma |
| Votaciones consultivas | MVP+ | Sujeto a validación legal; no vinculantes |
| Proveedores: directorio y facturas | MVP+ | |
| Push notifications | Post-MVP | |
| Sincronización bancaria automática (PSD2) | Post-MVP | |
| Pagos o transferencias desde la plataforma | Fuera de alcance | |
| Inteligencia artificial | Post-MVP | |
| Votaciones con validez jurídica | Fuera de alcance | Requiere validación legal específica |
| Estadísticas avanzadas y BI | Post-MVP | |

### 4.2. Fuera de alcance explícito

- Realizar pagos, transferencias o cobros reales.
- Almacenar credenciales bancarias.
- Sustituir la contabilidad oficial o la firma de actas.
- Ofrecer asesoramiento jurídico.
- Gestionar datos especialmente sensibles (salud, ideología, etc.) más allá de lo imprescindible.
- Publicar listas de morosidad.
- Aplicaciones móviles nativas en el MVP.
- Microservicios y despliegue multi-región en el MVP.

## 5. Requisitos funcionales

Las etiquetas de alcance son: `[MVP]`, `[MVP+]` (si el piloto lo valida) y `[POST]`.

### 5.1. Autenticación y sesiones

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-AUTH-001 | Login con email y contraseña. | MVP |
| RF-AUTH-002 | El alta de usuarios se realiza mediante invitación a una comunidad. | MVP |
| RF-AUTH-003 | Verificación de email antes de operar. | MVP |
| RF-AUTH-004 | Recuperación de contraseña mediante token de un solo uso con caducidad. | MVP |
| RF-AUTH-005 | JWT de acceso de corta duración y refresh token rotatorio con revocación. | MVP |
| RF-AUTH-006 | Cierre de sesión local y cierre de todas las sesiones. | MVP |
| RF-AUTH-007 | Bloqueo temporal y limitación de intentos ante fuerza bruta. | MVP |
| RF-AUTH-008 | Registro de accesos y eventos de seguridad relevantes. | MVP |
| RF-AUTH-009 | Segundo factor (TOTP) opcional. | POST |

Reglas:

- No se permitirá autoregistro abierto en el MVP.
- Una invitación caduca y solo puede aceptarse una vez.
- La invitación define comunidad y rol inicial, nunca permisos de plataforma.
- El refresh token no se almacena en claro.
- El cambio de contraseña revoca las sesiones existentes.

### 5.2. Multi-tenancy

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-TEN-001 | Un usuario puede pertenecer a varias comunidades con roles distintos. | MVP |
| RF-TEN-002 | Toda operación de negocio se ejecuta en el contexto de una comunidad autorizada. | MVP |
| RF-TEN-003 | El contexto de comunidad se deriva del usuario autenticado y de la ruta; nunca se confía solo en un identificador enviado por el cliente. | MVP |
| RF-TEN-004 | Ninguna consulta puede devolver datos de otra comunidad. | MVP |
| RF-TEN-005 | El acceso transversal de `SUPER_ADMIN` queda auditado con actor, motivo y recurso. | MVP |
| RF-TEN-006 | Existen pruebas automatizadas de aislamiento para cada módulo con datos. | MVP |

### 5.3. Comunidades y estructura física

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-COM-001 | Crear y configurar una comunidad (nombre, CIF, dirección, localidad, provincia, código postal, zona horaria, idioma por defecto, moneda). | MVP |
| RF-COM-002 | Gestionar edificios de una comunidad. | MVP |
| RF-COM-003 | Gestionar unidades: vivienda, local, garaje, trastero y elemento común. | MVP |
| RF-COM-004 | Cada unidad tendrá código, tipo, planta, puerta, superficie y coeficiente de participación. | MVP |
| RF-COM-005 | Validar que la suma de coeficientes de las unidades que participan en la comunidad sea 100 %. | MVP |
| RF-COM-006 | Gestionar elementos comunes independientes de las unidades. | MVP |
| RF-COM-007 | Archivar entidades sin borrar historial financiero ni documental. | MVP |

### 5.4. Propietarios, inquilinos y coeficientes

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-OWN-001 | Asociar propietarios a unidades con tipo de titularidad y porcentaje. | MVP |
| RF-OWN-002 | Soportar titularidad compartida; la suma por unidad no puede superar el 100 %. | MVP |
| RF-OWN-003 | Autorizar inquilinos con vigencia temporal y vínculo a una unidad. | MVP |
| RF-OWN-004 | Mantener histórico temporal de titularidad y alquiler. | MVP |
| RF-OWN-005 | Guardar una instantánea del coeficiente al generar cargos financieros. | MVP |
| RF-OWN-006 | Un propietario puede consultar sus cuotas y pagos, pero no la información financiera individual de otros propietarios. | MVP |
| RF-OWN-007 | Los datos de contacto solo son visibles según permiso y finalidad. | MVP |

### 5.5. Chat y comunicación

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-CHAT-001 | Canal general de la comunidad. | MVP+ |
| RF-CHAT-002 | Canales temáticos creados por usuarios autorizados. | MVP+ |
| RF-CHAT-003 | Conversaciones privadas entre miembros autorizados. | MVP+ |
| RF-CHAT-004 | Mensajes en tiempo real mediante WebSocket. | MVP+ |
| RF-CHAT-005 | Adjuntos con límites de tamaño y tipo, almacenados de forma segura. | MVP+ |
| RF-CHAT-006 | Notificación a los destinatarios según sus preferencias e idioma. | MVP+ |
| RF-CHAT-007 | Moderación: ocultar mensajes y expulsar de canales con registro de auditoría. | MVP+ |
| RF-CHAT-008 | Historial de mensajes accesible solo a miembros del canal y moderadores. | MVP+ |

### 5.6. Finanzas

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-FIN-001 | Registrar cuentas bancarias de la comunidad mostrando IBAN enmascarado. | MVP |
| RF-FIN-002 | Abrir y cerrar ejercicios fiscales por comunidad. | MVP |
| RF-FIN-003 | Crear presupuestos con partidas de ingreso y gasto. | MVP |
| RF-FIN-004 | Registrar ingresos y gastos con fecha, categoría, importe y descripción. | MVP |
| RF-FIN-005 | Generar cuotas ordinarias y derramas extraordinarias por periodo. | MVP |
| RF-FIN-006 | Repartir cuotas por coeficiente, a partes iguales o con reparto manual justificado. | MVP |
| RF-FIN-007 | Generar cargos por unidad y controlar su estado de cobro. | MVP |
| RF-FIN-008 | Registrar facturas de proveedor con base, impuestos, total y documento adjunto. | MVP |
| RF-FIN-009 | Registrar pagos y vincularlos a cargos y/o facturas. | MVP |
| RF-FIN-010 | Calcular saldo comunitario y estado de cuotas pendientes. | MVP |
| RF-FIN-011 | Separar movimientos bancarios, apuntes contables, facturas y pagos. | MVP |
| RF-FIN-012 | Toda operación financiera genera registro de auditoría inmutable. | MVP |
| RF-FIN-013 | Los apuntes contabilizados no se editan: se corrigen mediante reverso. | MVP |
| RF-FIN-014 | Generar informes básicos por ejercicio y categoría. | MVP |
| RF-FIN-015 | Usar `BigDecimal` en Java y `DECIMAL` en PostgreSQL; nunca `float`/`double`. | MVP |
| RF-FIN-016 | Aplicar redondeo y distribución de céntimos con regla documentada y probada. | MVP |
| RF-FIN-017 | Soportar operaciones financieras idempotentes. | MVP |

### 5.7. Banca

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-BANK-001 | Importar movimientos bancarios desde CSV con mapeo configurable por comunidad. | MVP |
| RF-BANK-002 | Detectar duplicados mediante huella del movimiento. | MVP |
| RF-BANK-003 | Conciliar movimientos con pagos o apuntes contables. | MVP |
| RF-BANK-004 | Registrar importaciones con usuario, fecha, fichero y resultado. | MVP |
| RF-BANK-005 | La integración bancaria es de solo lectura. | MVP |
| RF-BANK-006 | Nunca almacenar credenciales bancarias. | MVP |
| RF-BANK-007 | Sincronización automática mediante proveedor autorizado (PSD2). | POST |
| RF-BANK-008 | No ejecutar transferencias ni pagos. | Fuera de alcance |

### 5.8. Incidencias

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-INC-001 | Crear incidencias con título, descripción, categoría y ubicación. | MVP |
| RF-INC-002 | Adjuntar fotografías. | MVP |
| RF-INC-003 | Asignar prioridad, responsable y proveedor. | MVP |
| RF-INC-004 | Gestionar estados: abierta, en curso, pendiente de proveedor, resuelta, cerrada, cancelada. | MVP |
| RF-INC-005 | Añadir comentarios y seguimiento con autor y fecha. | MVP |
| RF-INC-006 | Mantener histórico de cambios de estado y asignación. | MVP |
| RF-INC-007 | Un propietario solo ve sus incidencias o las de su unidad salvo permiso global. | MVP |

### 5.9. Documentación

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-DOC-001 | Subir documentos con categoría: acta, estatuto, contrato, factura, presupuesto, informe, certificado u otro. | MVP |
| RF-DOC-002 | Controlar visibilidad por comunidad, órgano de gobierno, propietarios, rol o usuario. | MVP |
| RF-DOC-003 | Versionar documentos sin sobrescribir versiones anteriores. | MVP |
| RF-DOC-004 | Registrar auditoría de subida, descarga, modificación y borrado lógico. | MVP |
| RF-DOC-005 | Almacenar documentos en almacenamiento de objetos, no en base de datos. | MVP |
| RF-DOC-006 | Validar tipo y tamaño; analizar archivos subidos antes de publicarlos. | MVP |
| RF-DOC-007 | Aplicar retención y borrado conforme a obligaciones legales. | MVP |

### 5.10. Reuniones

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-MEET-001 | Crear convocatorias con fecha, hora, lugar, tipo y orden del día. | MVP |
| RF-MEET-002 | Adjuntar documentación de la convocatoria. | MVP |
| RF-MEET-003 | Registrar asistencia, ausencia y representación. | MVP |
| RF-MEET-004 | Registrar acuerdos por punto del orden del día con resultado de votación. | MVP |
| RF-MEET-005 | Adjuntar el acta y vincularla a la reunión. | MVP |
| RF-MEET-006 | Notificar la convocatoria según idioma y preferencias. | MVP |

### 5.11. Votaciones

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-VOTE-001 | Crear propuestas con opciones y periodo de votación. | MVP+ |
| RF-VOTE-002 | Determinar participantes según unidades, coeficientes y representaciones. | MVP+ |
| RF-VOTE-003 | Registrar un voto por participante elegible y evitar duplicados. | MVP+ |
| RF-VOTE-004 | Calcular quórum y resultado por número de votos y por coeficiente. | MVP+ |
| RF-VOTE-005 | Registrar representaciones y su vigencia. | MVP+ |
| RF-VOTE-006 | Mostrar siempre el aviso de que una votación digital no es automáticamente un acuerdo jurídicamente válido. | MVP+ |
| RF-VOTE-007 | Soportar votación secreta sin asociar el sentido del voto a la identidad en los resultados públicos. | POST |

### 5.12. Proveedores

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-PROV-001 | Registrar proveedores con datos fiscales y de contacto. | MVP+ |
| RF-PROV-002 | Clasificar proveedores por categoría. | MVP+ |
| RF-PROV-003 | Vincular incidencias y facturas a proveedores. | MVP+ |
| RF-PROV-004 | Mantener histórico de contratos y presupuestos. | POST |
| RF-PROV-005 | El acceso del proveedor se limita a los recursos que tenga asignados. | MVP+ |

### 5.13. Notificaciones

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-NOTIF-001 | Notificaciones internas en la aplicación. | MVP |
| RF-NOTIF-002 | Notificaciones por email. | MVP |
| RF-NOTIF-003 | Preferencias de notificación por usuario y categoría. | MVP |
| RF-NOTIF-004 | El contenido se genera en el idioma del destinatario. | MVP |
| RF-NOTIF-005 | Notificaciones push. | POST |
| RF-NOTIF-006 | Reintentos controlados y registro de envíos fallidos. | MVP |
| RF-NOTIF-007 | No incluir datos financieros sensibles en el asunto o cuerpo del email. | MVP |

### 5.14. Inteligencia artificial

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-AI-001 | Asistente documental sobre documentos autorizados. | POST |
| RF-AI-002 | Resúmenes de reuniones y actas. | POST |
| RF-AI-003 | Análisis y clasificación de gastos e incidencias. | POST |
| RF-AI-004 | La IA no puede ejecutar operaciones financieras, aprobar acuerdos ni modificar datos sin confirmación humana. | POST |
| RF-AI-005 | La IA respeta permisos, idioma y minimización de datos. | POST |

### 5.15. Auditoría y trazabilidad

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-AUD-001 | Registrar operaciones sensibles con actor, comunidad, entidad, acción, fecha y resultado. | MVP |
| RF-AUD-002 | Los registros de auditoría son inmutables y consultables por personal autorizado. | MVP |
| RF-AUD-003 | Registrar accesos y descargas de documentación sensible. | MVP |
| RF-AUD-004 | Registrar cambios de roles y permisos. | MVP |

## 6. Internacionalización (i18n)

### 6.1. Requisitos

| ID | Requisito | Alcance |
| --- | --- | --- |
| RF-I18N-001 | La aplicación deberá estar disponible en castellano y catalán desde el MVP. | MVP |
| RF-I18N-002 | Cada usuario podrá seleccionar su idioma preferido. | MVP |
| RF-I18N-003 | Las notificaciones respetarán el idioma del destinatario. | MVP |
| RF-I18N-004 | Los textos de interfaz no estarán codificados directamente en los componentes. | MVP |
| RF-I18N-005 | La arquitectura permitirá incorporar nuevos idiomas. | MVP |
| RF-I18N-006 | Se adaptarán fechas, números y formatos regionales. | MVP |
| RF-I18N-007 | La preferencia de idioma persiste entre sesiones y dispositivos. | MVP |
| RF-I18N-008 | Los emails, plantillas y mensajes del sistema se traducen en backend. | MVP |
| RF-I18N-009 | Los mensajes escritos por usuarios y los documentos oficiales conservan su idioma original. | MVP |
| RF-I18N-010 | El catalán utiliza terminología administrativa natural de Cataluña, no traducción literal. | MVP |
| RF-I18N-011 | Existe un selector de idioma accesible desde la interfaz y desde el perfil. | MVP |
| RF-I18N-012 | El idioma por defecto de una comunidad se aplica a nuevos miembros sin preferencia. | MVP |

### 6.2. Reglas

- Frontend: `i18next` + `react-i18next`, archivos `frontend/src/locales/es/translation.json` y
  `frontend/src/locales/ca/translation.json`.
- Claves semánticas y estables, organizadas por módulo: `auth.login.title`, `finance.balance.label`.
- Prohibida la concatenación de frases traducidas; usar interpolación y plurales ICU.
- Fechas, números e importes con `Intl.DateTimeFormat` / `Intl.NumberFormat`, locales `es-ES` y `ca-ES`.
- Moneda `EUR`, zona horaria `Europe/Madrid` por defecto, configurables por comunidad.
- Backend: mensajes de error mediante claves (`messageKey`) y parámetros, no textos incrustados.
- Emails y notificaciones se renderizan con el locale del destinatario, con fallback a `es`.
- Añadir un idioma no debe requerir cambios en componentes, solo configuración y archivos de traducción.
- Las traducciones se revisan por una persona competente en catalán antes de publicar.

## 7. Reglas de negocio

1. Una unidad pertenece a una única comunidad y, opcionalmente, a un edificio.
2. La suma de coeficientes de participación de las unidades debe ser 100 %; se admite una tolerancia
   máxima de 0,0001 configurable y validada antes de activar la comunidad.
3. La suma de porcentajes de titularidad de una unidad no puede superar el 100 %.
4. Un cargo financiero usa el coeficiente vigente en el momento de su generación.
5. Los apuntes contabilizados son inmutables; las correcciones se hacen con un apunte de reverso.
6. Las cuotas y derramas se generan por periodo y deben poder auditarse línea a línea.
7. No se eliminan físicamente registros financieros, documentos con retención legal ni registros de
   auditoría.
8. Un usuario solo accede a recursos de comunidades donde tiene una membresía activa.
9. Un proveedor solo accede a incidencias, documentos y facturas donde esté asignado.
10. Las notificaciones respetan las preferencias del usuario, salvo comunicaciones legales obligatorias.
11. Las votaciones del MVP son consultivas y se muestran con advertencia legal.
12. Los importes se almacenan con precisión decimal; los cálculos y redondeos se realizan en servidor.

## 8. Requisitos no funcionales

Los valores marcados como *propuestos* requieren validación en FASE 1.

### RNF-01. Seguridad

- Autorización en backend en cada operación.
- Hash de contraseñas con Argon2id o BCrypt de coste adecuado.
- JWT firmados, rotación de refresh tokens y revocación.
- Validación de entradas y salidas; prevención de XSS, CSRF, inyección SQL y path traversal.
- Protección de archivos con URLs firmadas de corta duración.
- Limitación de tasa en autenticación y operaciones sensibles.
- Secretos fuera del repositorio y gestionados por entorno.
- Auditoría de operaciones sensibles.
- Análisis de dependencias y contenedores en CI.

### RNF-02. Privacidad y cumplimiento

- RGPD y LOPDGDD desde el diseño.
- Minimización de datos y finalidad explícita.
- Derechos de acceso, rectificación, supresión y portabilidad, con las excepciones legales de
  conservación contable.
- Registro de actividades de tratamiento y base jurídica documentada.
- No publicar datos de morosidad ni información financiera individual de otros propietarios.
- Validación legal de la Ley de Propiedad Horizontal para convocatorias, actas y votaciones.

### RNF-03. Rendimiento

- Respuestas de lectura habituales: p95 < 500 ms en entorno de referencia. *Propuesto.*
- Operaciones de escritura habituales: p95 < 800 ms. *Propuesto.*
- Dashboard con datos de una comunidad piloto: < 2 s. *Propuesto.*
- Paginación obligatoria en listados.
- Carga de documentos desacoplada de la petición cuando sea pesada.

### RNF-04. Disponibilidad y recuperación

- Disponibilidad objetivo del 99,5 % mensual en el piloto. *Propuesto.*
- Copias de seguridad diarias de PostgreSQL y almacenamiento de objetos.
- RPO ≤ 24 h y RTO ≤ 8 h en el MVP. *Propuesto.*
- Prueba de restauración documentada antes de la puesta en producción.

### RNF-05. Accesibilidad y usabilidad

- WCAG 2.1 AA como objetivo.
- Navegación por teclado, foco visible y etiquetas accesibles.
- Contraste mínimo 4,5:1 en texto normal.
- Diseño mobile-first y responsive.
- Lenguaje claro, orientado a personas con baja experiencia digital.

### RNF-06. Mantenibilidad y calidad

- Monolito modular con dependencias explícitas y sin ciclos.
- Capas separadas: controller, service, repository, entity, dto, mapper, exception, validation.
- Código y nombres técnicos en inglés; interfaz y documentación funcional en español/catalán.
- Pruebas unitarias, de integración, de seguridad y E2E en rutas críticas.
- Migraciones Flyway versionadas y reproducibles.
- Cobertura prioritaria de lógica financiera, autorización y aislamiento multi-tenant.
- Documentación sincronizada con el código.

### RNF-07. Escalabilidad y operación

- Escalado horizontal del backend sin estado de sesión en memoria.
- Redis opcional para sesiones, rate limiting o caché; no obligatorio en el MVP.
- WebSocket preparado para escalar con broker externo si es necesario.
- Procesamiento asíncrono para emails, notificaciones y tareas pesadas.
- Observabilidad: logs estructurados, métricas y trazas con identificador de correlación.

### RNF-08. Compatibilidad

- Navegadores actuales: Chrome, Edge, Firefox y Safari.
- Móvil y escritorio.
- API versionada (`/api/v1`).

## 9. Requisitos pendientes de validación

| ID | Pregunta / decisión | Impacto | Bloquea |
| --- | --- | --- | --- |
| Q-01 | ¿El alta es exclusivamente por invitación o se prevé solicitud pública de acceso? | Modelo de registro y antiabuso | FASE 2 |
| Q-02 | ¿`COMMUNITY_ADMIN` representa al administrador de fincas y puede gestionar varias comunidades? | Modelo de roles y permisos | FASE 2 |
| Q-03 | ¿Qué grado de validez legal se exige a las votaciones del piloto? | Diseño del módulo de votaciones | FASE 3 |
| Q-04 | ¿Qué proveedor bancario/PSD2 se usará y con qué alcance? | Integración futura | POST |
| Q-05 | ¿Qué proveedor de email y de almacenamiento S3 se utilizará? | Infraestructura | FASE 2 |
| Q-06 | ¿Se usarán subdominios por comunidad, dominio único con selector o ambos? | Enrutamiento y cookies | FASE 2 |
| Q-07 | ¿Qué plan contable y categorías de gasto usa la comunidad piloto? | Modelo financiero | FASE 3 |
| Q-08 | ¿Qué regla exacta de redondeo y reparto de céntimos se aprueba? | Cálculos financieros | FASE 3 |
| Q-09 | ¿Qué plazos de conservación documental y contable se aplican? | Retención y borrado | FASE 3 |
| Q-10 | ¿Puede un propietario ver la identidad de otros propietarios? ¿Con qué finalidad? | Privacidad y LPH | FASE 3 |
| Q-11 | ¿Se requiere chat en el MVP o puede pasar a MVP+? | Alcance | FASE 3 |
| Q-12 | ¿Se requiere push en el MVP? | Alcance y proveedor | POST |
| Q-13 | ¿Qué límites de tamaño y tipos de archivo se aceptan? | Almacenamiento y seguridad | FASE 2 |
| Q-14 | ¿Se permite representación de voto y cómo se acredita? | Votaciones y reuniones | FASE 3 |
| Q-15 | ¿Qué datos de proveedores son visibles para propietarios? | Privacidad | FASE 3 |
| Q-16 | ¿La comunidad piloto exige facturación electrónica o integración con gestoría? | Finanzas e integraciones | POST |
| Q-17 | ¿Qué idioma por defecto prevalece si el usuario no ha elegido? | i18n | FASE 2 |
| Q-18 | ¿Qué política de retención de logs y auditoría se aplica? | Operación y RGPD | FASE 2 |

### 9.1. Contradicciones y ambigüedades detectadas

| ID | Observación | Tratamiento propuesto | Estado |
| --- | --- | --- | --- |
| C-01 | El workspace y el SPEC inicial usan "ComunidadCat 360"; el prompt usa "COMUNIDAD 360". | "COMUNIDAD 360" como nombre de producto; "ComunidadCat 360" como nombre del repositorio. | Pendiente de confirmar |
| C-02 | El SPEC inicial solo contenía requisitos i18n; el prompt define el producto completo. | SPEC ampliado conservando RF-I18N-001 a 006. | Resuelto |
| C-03 | "Registro mediante invitación" frente a la posibilidad de registro abierto. | MVP solo por invitación; Q-01 decide si habrá solicitud pública. | Pendiente (Q-01) |
| C-04 | La visión menciona administradores de fincas, pero los roles no incluyen un rol específico. | Interpretar `COMMUNITY_ADMIN` como administrador/a con acceso a varias comunidades. | Pendiente (Q-02) |
| C-05 | Se pide módulo de votaciones, pero se advierte de que no todo voto digital es un acuerdo válido. | MVP+ solo votaciones consultivas con aviso legal; formales post-MVP tras revisión legal. | Resuelto provisional |
| C-06 | La visión menciona estadísticas, pero no hay módulo de estadísticas en la lista funcional. | Cubrir con dashboard e informes básicos; analítica avanzada post-MVP. | Resuelto provisional |
| C-07 | Se mencionan notificaciones push, pero no hay proveedor ni requisito de plataforma. | Push post-MVP; email e in-app en MVP. | Pendiente (Q-12) |
| C-08 | El prompt no exige explícitamente informes PDF para COMUNIDAD 360. | Mantener informe exportable como P1 sujeto a validación; no bloquear MVP. | Pendiente |
| C-09 | Redis aparece como "cuando sea necesario" y podría complicar el MVP. | Redis opcional, fuera del camino crítico. | Resuelto |
| C-10 | El chat se describe como módulo, pero su coste y valor en el piloto no están confirmados. | Chat como MVP+ condicionado a Q-11. | Pendiente (Q-11) |
| C-11 | Se prevé sincronización bancaria con proveedor autorizado, pero no hay proveedor elegido. | CSV de solo lectura en MVP; PSD2 post-MVP. | Resuelto provisional |
| C-12 | Se exige invitación para el alta y también recuperación de contraseña. | Compatibles: la invitación crea la cuenta; la recuperación gestiona el acceso. | Resuelto |
| C-13 | Se exige i18n total, pero los documentos oficiales deben conservar su idioma. | Traducir interfaz, mensajes y metadatos; no traducir contenido de usuario ni documentos. | Resuelto |
| C-14 | Transparencia financiera frente a protección de datos y morosidad. | Mostrar solo datos individuales al titular y agregados a roles autorizados; sin listas de morosos. | Resuelto provisional |
| C-15 | Multi-comunidad frente a moneda y zona horaria fijas. | `EUR` y `Europe/Madrid` por defecto, configurables por comunidad. | Resuelto provisional |

## 10. Criterios de éxito del MVP

El MVP se considerará operativo para el piloto cuando:

1. La comunidad piloto esté dada de alta con edificios, unidades y coeficientes validados al 100 %.
2. Los propietarios e inquilinos puedan activar su cuenta por invitación y acceder según su rol.
3. Un usuario de otra comunidad no pueda acceder a ningún dato ajeno, con pruebas automatizadas.
4. Se puedan registrar ingresos, gastos, cuotas y pagos, y el saldo cuadre con el extracto importado.
5. Se pueda crear, asignar, seguir y cerrar una incidencia con fotografía y comentarios.
6. Se puedan subir y descargar documentos respetando permisos y dejando auditoría.
7. Se pueda convocar una reunión, registrar asistencia y adjuntar el acta.
8. Las notificaciones internas y por email lleguen en el idioma del destinatario.
9. Toda la interfaz esté disponible en español y catalán, sin textos incrustados.
10. No existan vulnerabilidades críticas o altas abiertas en la revisión de seguridad.
11. Exista una copia de seguridad y una restauración verificada.
12. La documentación esté actualizada y el equipo pueda desplegar con un procedimiento reproducible.

## 11. Glosario

| Término | Definición |
| --- | --- |
| Comunidad | Conjunto de propietarios y unidades sujeto a la Ley de Propiedad Horizontal. |
| Unidad | Vivienda, local, garaje, trastero o elemento común con coeficiente. |
| Coeficiente | Porcentaje de participación de una unidad en la comunidad. |
| Cuota ordinaria | Importe periódico para financiar gastos generales. |
| Derrama | Cuota extraordinaria para un gasto específico. |
| Cargo | Deuda generada a una unidad por una cuota o derrama. |
| Conciliación | Vinculación entre un movimiento bancario y un pago o apunte. |
| Membresía | Vínculo entre un usuario y una comunidad con roles y permisos. |
| Tenant | Comunidad aislada dentro de la plataforma multi-comunidad. |
| Acta | Documento que recoge acuerdos y deliberaciones de una reunión. |

## 12. Trazabilidad documental

- Arquitectura: [`ARCHITECTURE.md`](./ARCHITECTURE.md)
- Base de datos: [`DATABASE.md`](./DATABASE.md)
- API: [`API.md`](./API.md)
- Seguridad: [`SECURITY.md`](./SECURITY.md)
- Diseño: [`DESIGN-SYSTEM.md`](./DESIGN-SYSTEM.md)
- Roadmap: [`ROADMAP.md`](./ROADMAP.md)
- Backlog: [`BACKLOG.md`](./BACKLOG.md)
- Decisiones: [`ADR/`](./ADR/)

---

*Fin de docs/SPEC.md — COMUNIDAD 360 v0.1.0 (FASE 0).*
