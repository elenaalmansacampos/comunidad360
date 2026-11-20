# COMUNIDAD 360 — Seguridad y privacidad

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Marco inicial (FASE 0) |
| Fecha | 2026-11-19 |
| Alcance | Aplicación, API, base de datos, almacenamiento, infraestructura y procesos |

> Este documento no sustituye una auditoría legal ni una revisión de seguridad independiente. Las
> decisiones marcadas como pendientes deben validarse antes de producción.

---

## 1. Principios

1. **Mínimo privilegio**: cada usuario, servicio y proceso accede solo a lo necesario.
2. **Defensa en profundidad**: autenticación, autorización, aislamiento, validación, auditoría y
   monitorización.
3. **Seguridad en backend**: el frontend nunca autoriza.
4. **Privacidad desde el diseño**: minimización, finalidad y retención.
5. **Trazabilidad**: operaciones sensibles auditadas.
6. **No secretos en Git**.
7. **Fallo seguro**: ante duda, denegar y registrar.
8. **Datos financieros y personales como activos críticos**.

## 2. Activos protegidos

- Credenciales y sesiones.
- Datos personales de propietarios, inquilinos y proveedores.
- Datos financieros de la comunidad y de cada unidad.
- Documentos oficiales (actas, estatutos, contratos, facturas).
- Fotografías y adjuntos de incidencias y chat.
- Movimientos bancarios importados.
- Configuración de permisos y roles.
- Registros de auditoría.
- Secretos de infraestructura y claves de firma.
- Copias de seguridad.

## 3. Modelo de amenazas

| Amenaza | Vector | Impacto | Controles |
| --- | --- | --- | --- |
| Acceso a otra comunidad (IDOR) | Manipulación de IDs y rutas | Crítico | Tenant guard, consultas filtradas, FKs compuestas, pruebas de aislamiento |
| Escalada de privilegios | Roles/permisos mal validados | Crítico | Autorización por caso de uso, pruebas negativas, revisión de cambios de rol |
| Robo de sesión | XSS, malware, tokens expuestos | Alto | Access token en memoria, refresh HttpOnly, CSP, rotación, revocación |
| Reutilización de refresh token | Robo de cookie | Alto | Rotación, detección de reutilización, revocación en cascada |
| Fuerza bruta | Login y recuperación | Alto | Rate limiting, bloqueo temporal, CAPTCHA si necesario, alertas |
| Enumeración de cuentas | Mensajes distintos | Medio | Respuestas genéricas, tiempos homogéneos |
| Inyección SQL | Entradas no validadas | Crítico | JPA parametrizado, validación, revisión de consultas nativas |
| XSS en chat/documentos | HTML/JS malicioso | Alto | Renderizado seguro, sanitización, CSP, sin `dangerouslySetInnerHTML` |
| CSRF | Cookies de refresco | Alto | SameSite, validación de Origin, tokens CSRF si aplica |
| Subida de archivo malicioso | Documentos/adjuntos | Alto | Lista blanca, validación MIME real, antivirus, cuarentena, descarga forzada |
| Path traversal | Nombres de archivo | Alto | Claves opacas, saneado, sin rutas del usuario |
| Fuga por URL firmada | Enlace compartido | Medio | Expiración corta, permisos previos, registro de descargas |
| Inyección CSV | Exportaciones | Medio | Escape de fórmulas, prefijo seguro, validación |
| Inyección de email | Campos de usuario en plantillas | Medio | Plantillas controladas, escape, no HTML de usuario |
| SSRF | Futuras integraciones | Alto | Sin URLs arbitrarias; allowlist y validación estricta |
| Denegación de servicio | Carga masiva, WebSocket | Medio | Rate limiting, límites de tamaño, cuotas, timeouts |
| Fuga en logs | Tokens/datos en logs | Alto | Redacción, niveles, revisión, no logging de payloads sensibles |
| Manipulación de auditoría | Acceso a base de datos | Crítico | Append-only, permisos restringidos, backups, monitorización |
| Fuga de backup | Almacenamiento mal protegido | Crítico | Cifrado, acceso restringido, pruebas de restauración |
| Dependencia comprometida | Librería maliciosa | Alto | SCA, lockfiles, revisión, builds reproducibles |
| Error de configuración cloud | Bucket público, puertos abiertos | Crítico | IaC revisada, escaneo, mínimos privilegios, auditoría |

## 4. Autenticación

- Login con email y contraseña; registro solo por invitación.
- Verificación de email antes de operar.
- Contraseñas con Argon2id (preferido) o BCrypt con coste adecuado.
- Política mínima de contraseña alineada con buenas prácticas; no imponer reglas obsoletas que reduzcan
  entropía.
- Bloqueo temporal tras intentos fallidos y rate limiting por IP y cuenta.
- Respuestas genéricas ante credenciales incorrectas.
- Recuperación con token de un solo uso, caducidad corta y revocación de sesiones.
- Refresh token rotatorio con detección de reutilización.
- Cierre de sesión local y global.
- MFA opcional post-MVP; obligatorio para `SUPER_ADMIN` cuando se implemente.
- Auditoría de login, logout, refresco, fallos y cambios de contraseña.

## 5. Autorización y roles

### 5.1. Reglas

1. Cada endpoint declara permiso y ámbito.
2. Cada caso de uso valida pertenencia al recurso.
3. El rol no basta: se comprueba el permiso y el tenant.
4. Las denegaciones explícitas tienen prioridad.
5. Los permisos se acumulan entre roles y concesiones.
6. `SUPER_ADMIN` accede a soporte con justificación y auditoría.
7. Los cambios de roles y permisos requieren permiso y quedan auditados.
8. Los proveedores solo ven recursos asignados.

### 5.2. Matriz inicial de permisos por rol

Leyenda: ✔ concedido por defecto, ◐ limitado a recursos propios o asignados, — no concedido.

| Permiso | SUPER_ADMIN | COMMUNITY_ADMIN | PRESIDENT | SECRETARY | TREASURER | OWNER | TENANT | PROVIDER |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `COMMUNITY_VIEW` | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `COMMUNITY_MANAGE` | ✔ | ✔ | — | — | — | — | — | — |
| `MEMBER_VIEW` | ✔ | ✔ | ✔ | ✔ | ◐ | ◐ | — | — |
| `MEMBER_MANAGE` | ✔ | ✔ | — | — | — | — | — | — |
| `MEMBER_INVITE` | ✔ | ✔ | — | ✔ | — | — | — | — |
| `UNIT_VIEW` | ✔ | ✔ | ✔ | ✔ | ✔ | ◐ | ◐ | ◐ |
| `UNIT_MANAGE` | ✔ | ✔ | — | — | — | — | — | — |
| `OWNERSHIP_MANAGE` | ✔ | ✔ | — | ◐ | — | — | — | — |
| `FINANCE_VIEW` | ✔ | ✔ | ✔ | ✔ | ✔ | ◐ | — | — |
| `FINANCE_MANAGE` | ✔ | ✔ | — | — | ✔ | — | — | — |
| `INVOICE_MANAGE` | ✔ | ✔ | — | — | ✔ | — | — | ◐ |
| `REPORT_VIEW` | ✔ | ✔ | ✔ | ✔ | ✔ | ◐ | — | — |
| `BANK_ACCOUNT_VIEW` | ✔ | ✔ | ◐ | — | ✔ | — | — | — |
| `BANK_IMPORT` | ✔ | ✔ | — | — | ✔ | — | — | — |
| `BANK_RECONCILE` | ✔ | ✔ | — | — | ✔ | — | — | — |
| `INCIDENT_CREATE` | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | — |
| `INCIDENT_VIEW_ALL` | ✔ | ✔ | ✔ | ✔ | ✔ | — | — | — |
| `INCIDENT_VIEW_OWN` | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ◐ |
| `INCIDENT_MANAGE` | ✔ | ✔ | ◐ | ◐ | — | — | — | ◐ |
| `DOCUMENT_VIEW` | ✔ | ✔ | ✔ | ✔ | ✔ | ◐ | ◐ | ◐ |
| `DOCUMENT_UPLOAD` | ✔ | ✔ | ✔ | ✔ | ✔ | ◐ | — | ◐ |
| `DOCUMENT_MANAGE` | ✔ | ✔ | — | ✔ | — | — | — | — |
| `MEETING_VIEW` | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | — |
| `MEETING_MANAGE` | ✔ | ✔ | ◐ | ✔ | — | — | — | — |
| `VOTE_VIEW` | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ◐ | — |
| `VOTE_MANAGE` | ✔ | ✔ | — | ✔ | — | — | — | — |
| `VOTE_CAST` | — | ✔ | ✔ | ✔ | ✔ | ✔ | ◐ | — |
| `CHAT_VIEW` | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ◐ |
| `CHAT_MODERATE` | ✔ | ✔ | ◐ | ◐ | — | — | — | — |
| `PROVIDER_VIEW` | ✔ | ✔ | ✔ | ✔ | ✔ | — | — | ◐ |
| `PROVIDER_MANAGE` | ✔ | ✔ | — | — | ✔ | — | — | — |
| `NOTIFICATION_MANAGE` | ✔ | ✔ | — | — | — | — | — | — |
| `AUDIT_VIEW` | ✔ | ✔ | ◐ | ◐ | — | — | — | — |

La matriz es una propuesta inicial. Debe validarse con la comunidad piloto y la Ley de Propiedad
Horizontal antes de producción. Las pruebas de autorización son obligatorias para cada celda relevante.

### 5.3. Aislamiento multi-tenant

- `community_id` obligatorio en entidades de negocio.
- Contexto de tenant validado en cada petición.
- FKs compuestas para impedir referencias cruzadas.
- Consultas de repositorio filtradas por comunidad.
- Pruebas de acceso cruzado por módulo.
- RLS opcional como defensa adicional.
- Respuestas `403`/`404` sin revelar existencia de recursos ajenos.

## 6. Datos personales y RGPD/LOPDGDD

### 6.1. Bases y finalidades

| Tratamiento | Finalidad | Base jurídica propuesta | Datos |
| --- | --- | --- | --- |
| Gestión de usuarios | Acceso y seguridad | Ejecución de contrato / interés legítimo | Identidad, email, idioma, sesiones |
| Gestión de comunidades | Operación de la comunidad | Obligación legal / interés legítimo | Unidades, coeficientes, titularidad |
| Finanzas | Cobros, pagos y contabilidad | Obligación legal / ejecución de contrato | Cargos, pagos, facturas, IBAN |
| Incidencias | Resolución de problemas | Interés legítimo / contrato | Descripción, fotos, comentarios |
| Documentos | Gestión documental | Obligación legal / interés legítimo | Actas, contratos, certificados |
| Comunicaciones | Información y participación | Interés legítimo / consentimiento cuando aplique | Mensajes, notificaciones |
| Proveedores | Gestión de servicios | Ejecución de contrato | Contacto, facturación |

La base jurídica definitiva debe validarse con asesoría legal.

### 6.2. Obligaciones

- Registro de actividades de tratamiento.
- Información por capas y política de privacidad.
- Minimización y exactitud de datos.
- Derechos de acceso, rectificación, supresión, limitación, portabilidad y oposición.
- Procedimiento de atención de derechos con plazos legales.
- Evaluación de impacto (DPIA) si el tratamiento lo requiere.
- Contratos de encargado de tratamiento con proveedores.
- Evaluación de transferencias internacionales (especialmente IA post-MVP).
- Política de cookies y consentimiento para analítica no esencial.
- Notificación de brechas a la autoridad y a las personas afectadas cuando proceda.
- Retención y supresión documentadas (Q-09).
- Control de acceso a datos personales y registro de auditoría.

### 6.3. Datos especialmente sensibles

- Evitar categorías especiales de datos.
- Las fotografías de incidencias pueden revelar información personal o del interior de viviendas: limitar
  acceso, informar y permitir eliminación cuando sea legal.
- No usar datos de salud, ideología, religión o similares salvo necesidad imperiosa y base jurídica
  específica.

## 7. Ley de Propiedad Horizontal y votaciones

- La plataforma no sustituye la convocatoria, celebración y documentación formal de reuniones.
- Las votaciones del MVP son consultivas y muestran una advertencia visible.
- No se afirmará que una votación digital es jurídicamente vinculante sin validación legal.
- Las actas y acuerdos deben poder exportarse y conservarse.
- La información financiera individual no se expone a otros propietarios.
- No se publican listas de morosidad.
- Se respeta la finalidad y la base jurídica de cada comunicación.

**Pendiente**: revisión legal específica antes de habilitar votaciones formales (Q-03) y antes de definir
la retención documental (Q-09).

## 8. Datos financieros

- Los importes se tratan como datos sensibles.
- Solo los roles autorizados ven información financiera.
- Un propietario ve sus propios cargos y pagos, no los de otras unidades.
- Los resúmenes agregados no deben permitir reidentificar a una unidad en comunidades pequeñas.
- El IBAN se almacena cifrado o enmascarado; nunca se muestran credenciales.
- Los logs no incluyen importes individuales completos ni identificadores bancarios.
- Las exportaciones se generan bajo demanda, con permisos y auditoría.
- Los ficheros CSV de importación bancaria se tratan como datos personales/financieros.
- Los apuntes contables no se borran; se corrigen mediante reverso.

## 9. Documentos y archivos

- Almacenamiento de objetos privado.
- Cifrado en tránsito y en reposo.
- Validación de extensión, MIME real, tamaño y hash.
- Lista blanca de tipos por categoría.
- Análisis antivirus y cuarentena.
- URLs firmadas de corta duración.
- Verificación de permisos antes de firmar.
- Registro de visualización y descarga.
- `Content-Disposition: attachment` para tipos no seguros.
- Prohibido servir HTML/SVG activo desde el dominio de la aplicación.
- Claves opacas; no se usa el nombre original como ruta.
- Límites de tamaño y cuota por comunidad (Q-13).
- Borrado lógico y retención legal.
- Copia de seguridad coordinada con la base de datos.

## 10. Banca

- Integración de solo lectura.
- En el MVP, importación CSV iniciada por usuario autorizado.
- Nunca almacenar credenciales bancarias.
- Nunca ejecutar transferencias ni pagos.
- Validar y normalizar el CSV; detectar duplicados por hash.
- Registrar importaciones, usuario, fecha y resultado.
- Tratar los movimientos como datos financieros sensibles.
- Para PSD2 futuro: proveedor autorizado, contrato, alcance mínimo, evaluación de seguridad y
  consentimiento del usuario.
- No confiar en el nombre del fichero ni en su extensión.

## 11. Seguridad de la aplicación

### 11.1. Entradas y salidas

- Validación en servidor con Bean Validation y reglas de dominio.
- Parámetros de consulta acotados y tipados.
- JPA parametrizado; prohibidas concatenaciones SQL.
- Serialización controlada de DTOs; no exponer entidades.
- Escape de salida en frontend.
- Sanitización de HTML si se permite contenido enriquecido; preferible texto plano en chat.
- Prevención de CSV injection en exportaciones.
- Validación de redirecciones y URLs externas.
- Deshabilitar entidades externas en XML.

### 11.2. Cabeceras y navegador

- `Content-Security-Policy` restrictiva.
- `X-Content-Type-Options: nosniff`.
- `Referrer-Policy: strict-origin-when-cross-origin`.
- `Permissions-Policy` mínima.
- HSTS en producción.
- CORS con lista blanca de orígenes.
- Cookies `Secure`, `HttpOnly`, `SameSite`.
- Sin `localStorage` para access tokens.
- Validación de `Origin` en operaciones con cookies.

### 11.3. Rate limiting

- Login, recuperación, refresco y verificación.
- Creación de incidencias y mensajes.
- Subida de archivos.
- Endpoints costosos (informes, importaciones).
- WebSocket: límite de conexiones y mensajes por usuario.

### 11.4. Errores y logs

- Mensajes genéricos al cliente.
- `traceId` para soporte.
- Logs estructurados sin secretos.
- Redacción de tokens, contraseñas, IBAN, cookies y datos personales innecesarios.
- No registrar cuerpos completos de peticiones sensibles.
- Retención de logs definida (Q-18).

## 12. Auditoría

Eventos mínimos:

- Login, logout, fallo de login, bloqueo, refresco y reutilización de token.
- Cambio de contraseña, email e idioma.
- Invitaciones: creación, aceptación, revocación.
- Cambios de membresía, rol y permisos.
- Acceso de `SUPER_ADMIN`.
- Creación, edición, emisión y anulación de cuotas, cargos, pagos, facturas y apuntes.
- Importación bancaria y conciliación.
- Subida, descarga, cambio de visibilidad y borrado de documentos.
- Moderación de chat.
- Convocatorias, asistencia, acuerdos y votaciones.
- Exportaciones e informes.
- Cambios de configuración de la comunidad.

Propiedades:

- Inmutables desde la aplicación.
- Con actor, tenant, entidad, acción, fecha y resultado.
- Sin secretos ni datos excesivos.
- Consultables por personal autorizado.
- Retención definida.

## 13. Secretos y configuración

- Secretos fuera del repositorio.
- `.env.example` sin valores reales.
- Gestión por variables de entorno o gestor de secretos.
- Claves JWT robustas y rotables.
- Claves de S3 con mínimo privilegio.
- Credenciales de email restringidas.
- Rotación documentada.
- Prohibido registrar secretos.
- Prohibido incluirlos en imágenes Docker.
- Acceso a producción restringido y auditado.

## 14. Copias de seguridad y continuidad

- Backup diario de PostgreSQL y almacenamiento de objetos.
- Cifrado de backups.
- Retención definida.
- Prueba de restauración antes de producción y periódicamente.
- RPO ≤ 24 h y RTO ≤ 8 h propuestos para el MVP.
- Procedimiento documentado de recuperación.
- Acceso a backups restringido.
- Verificación de integridad.

## 15. Seguridad de la cadena de suministro y CI/CD

- Dependencias con versiones fijadas y lockfiles.
- Análisis de vulnerabilidades (SCA).
- Análisis estático (SAST).
- Escaneo de imágenes de contenedor.
- Revisión de PR con foco en autorización y datos.
- Builds reproducibles.
- Firma o verificación de imágenes cuando sea posible.
- No desplegar con vulnerabilidades críticas abiertas.
- Registro de artefactos y trazabilidad de versiones.

## 16. Respuesta a incidentes

1. Detectar y clasificar.
2. Contener el acceso o la fuga.
3. Preservar evidencias y logs.
4. Evaluar impacto en datos personales.
5. Notificar según RGPD si procede.
6. Corregir la causa.
7. Revisar y documentar lecciones aprendidas.
8. Actualizar controles y pruebas.

Canales y responsables deben definirse antes de producción.

## 17. Pruebas de seguridad obligatorias

- Aislamiento entre comunidades en todos los módulos con datos.
- Autorización positiva y negativa por rol y recurso.
- Acceso a documentos con y sin permiso.
- URLs firmadas caducadas o manipuladas.
- Validación de archivos y tipos no permitidos.
- Rate limiting y bloqueo de cuenta.
- Reutilización de refresh token.
- CSRF en endpoints con cookie.
- XSS en chat, incidencias y documentos.
- Inyección SQL en filtros y ordenación.
- Importación CSV maliciosa o duplicada.
- Exportaciones con fórmulas.
- Enumeración de usuarios.
- Acceso de proveedor a recursos no asignados.
- Ausencia de secretos en logs y repositorio.

## 18. Criterios de aceptación de seguridad

Una funcionalidad no se considera terminada si:

1. No valida autorización en backend.
2. No tiene pruebas negativas de acceso.
3. Expone datos de otra comunidad.
4. Registra secretos o datos sensibles en logs.
5. Permite subir o descargar archivos sin control.
6. No audita una operación sensible.
7. Introduce una vulnerabilidad crítica o alta sin mitigación aprobada.

## 19. Decisiones pendientes

| ID | Decisión | Impacto |
| --- | --- | --- |
| Q-03 | Validez legal de votaciones | Diseño y avisos |
| Q-05 | Proveedor de email y S3 | Controles y contratos |
| Q-06 | Dominio y cookies | CSRF, CORS, SameSite |
| Q-09 | Retención documental y contable | Borrado y conservación |
| Q-13 | Límites y tipos de archivo | Superficie de ataque |
| Q-18 | Retención de logs y auditoría | Operación y RGPD |
| — | RLS en producción | Defensa en profundidad |
| — | MFA para roles críticos | Autenticación |
| — | Antivirus y proveedor | Archivos |
| — | Gestor de secretos | Infraestructura |
| — | Revisión legal RGPD/LPH | Cumplimiento |

---

*Fin de docs/SECURITY.md — v0.1.0 (FASE 0).*


## CSRF

La API es stateless y las operaciones de negocio usan `Authorization: Bearer`. La única cookie es
el refresh token, `HttpOnly` y `SameSite=Lax`; no autoriza operaciones de negocio. Por ello Spring
Security desactiva CSRF para la API. Esta decisión está revisada y se mantiene bajo vigilancia de
CodeQL; si se añaden operaciones autenticadas por cookie, deberá activarse CSRF o un patrón
equivalente.


## CodeQL

El workflow de seguridad ejecuta CodeQL para Java/Kotlin y JavaScript/TypeScript. Las alertas se
revisan y clasifican. Estado actual: 0 alertas abiertas.

Clasificaciones aceptadas:

- CSRF desactivado en API stateless con Bearer y refresh cookie `HttpOnly`/`SameSite`.
- Log de token de reset solo con `expose-dev-tokens=true` en desarrollo.
- Avisos de código de test y firmas de `ExceptionHandler` clasificados como no explotables.
