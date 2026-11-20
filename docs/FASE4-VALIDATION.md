# COMUNIDAD 360 — FASE 4: Validación y piloto

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Validación técnica en curso; piloto real pendiente |
| Fecha | 2026-11-19 |
| Alcance | E2E, accesibilidad, seguridad, carga, i18n y backup/restore |

---

## 1. Resultado ejecutivo

La validación técnica automatizable está en verde:

| Área | Resultado |
| --- | --- |
| Backend | 55 tests, 0 fallos |
| Frontend | 28 tests, 0 fallos |
| E2E Playwright | 3 tests, 0 fallos (desktop y móvil) |
| Accesibilidad automatizada | 0 violaciones serias/críticas en login y dashboard |
| i18n | 623 claves en `es` y `ca`, sin faltas |
| Dependencias frontend | 0 vulnerabilidades de producción |
| Backup/restore | Backup y restauración real verificados |
| Carga local | 0 peticiones fallidas en health y dashboard |
| Cabeceras de seguridad | CSP, Permissions-Policy, X-Frame-Options y Referrer-Policy activas |
| Rate limiting de autenticación | Activo y probado |
| Rate limiting perimetral | Nginx: 30 r/s por IP con burst |
| Métricas | Endpoint Prometheus disponible |
| Antimalware | ClamAV opcional; subida bloqueada si infecta o falla el análisis |

No se declara todavía un go/no-go de piloto: faltan validación legal, prueba con usuarios reales,
revisión manual de accesibilidad, endurecimiento de producción y backup programado/cifrado.

## 2. E2E y accesibilidad

Comando:

```bash
cd frontend
npm run test:e2e
```

Cubre:

- Login con usuario demo.
- Llegada al dashboard.
- Navegación por estructura, finanzas, banca, incidencias, documentos, reuniones, chat y votaciones.
- Regreso al dashboard.
- Flujo móvil con navegación inferior y escaneo axe.
- Escaneo axe en login y dashboard con etiquetas WCAG 2 A/AA.
- Cero violaciones `serious` o `critical`.

Se corrigió un problema real de contraste:

- `--color-gray-500` pasó de `#64748b` a `#475569`.
- Afectaba a subtítulos y badges; ahora cumple contraste AA en los elementos analizados.

Limitación: axe no sustituye una auditoría manual con lector de pantalla ni una revisión completa
de teclado, foco y textos alternativos.

## 3. Internacionalización

Comando:

```bash
cd frontend
npm run check:i18n
```

Resultado:

```text
[es] 623 keys OK
[ca] 623 keys OK
```

El script detecta claves faltantes, sobrantes y valores vacíos. Se corrigieron claves de dashboard
que faltaban en catalán.

## 4. Seguridad

### Controles verificados

- CORS limitado a orígenes de desarrollo configurados.
- Autorización backend por rol, permiso y tenant.
- Aislamiento multi-tenant con pruebas cruzadas en todos los módulos principales.
- Contraseñas con BCrypt.
- Cookies de refresh `HttpOnly` y `SameSite=Lax`.
- Bloqueo temporal por intentos fallidos.
- Validación de tipo y tamaño de archivos.
- URLs S3 firmadas con caducidad corta.
- Auditoría de operaciones sensibles.
- Cabeceras de seguridad en Nginx:
  - `Content-Security-Policy`
  - `Permissions-Policy`
  - `X-Frame-Options: DENY`
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `X-Content-Type-Options: nosniff`
- `npm audit --omit=dev`: 0 vulnerabilidades.
- Búsqueda de secretos en el repositorio: sin hallazgos; no hay `.env` versionado.
- Rate limiting de endpoints de autenticación por IP y minuto, con respuesta `429` y `Retry-After`.
- Perfil `prod` con secretos obligatorios, cookies `Secure`, seed desactivado y S3 real.
- Backups cifrados con AES-256/OpenSSL y retención configurable.
- Integración opcional con ClamAV (`zINSTREAM`) y política fail-closed para documentos y fotos.

### Riesgos abiertos

| Riesgo | Nivel | Acción |
| --- | --- | --- |
| Sin rate limiting global (solo auth) | Medio | Añadir límites generales en reverse proxy/API |
| Rate limit en memoria por instancia | Medio | Rate limiting distribuido si hay varias réplicas |
| Sin MFA | Medio | Evaluar para administradores |
| Secreto JWT de desarrollo | Alto en producción | Sustituir por secreto gestionado y rotación |
| Antivirus opcional, no activo por defecto | Medio | Activar ClamAV y monitorizar fallos antes de producción |
| Backups sin programación automática | Alto | Programar, externalizar y restringir acceso; cifrado ya disponible |
| Sin WAF ni monitorización de seguridad | Medio | Definir infraestructura de producción |
| Sin prueba de penetración | Alto | Contratar antes de comercializar |
| Chat sin moderación/adjuntos | Medio | Completar si se activa en piloto |

## 5. Carga básica

Entorno: Mac local con Colima, backend Docker y PostgreSQL local del stack. No es un benchmark de
producción.

| Prueba | Peticiones | Concurrencia | Fallos | Throughput | Latencia media |
| --- | --- | --- | --- | --- | --- |
| `GET /actuator/health` | 300 | 10 | 0 | ≈481 req/s | ≈20,8 ms |
| `GET /dashboard` autenticado | 200 | 5 | 0 | ≈87 req/s | ≈57 ms |

Comando reproducible:

```bash
scripts/load-test.sh
```

También se validó el rate limiting perimetral de Nginx: 30 r/s por IP con burst 60 y respuesta
`429 RATE_LIMITED`. Las métricas Prometheus están en `/actuator/prometheus` y requieren autenticación.

Pendiente: prueba con datos realistas, concurrencia de escritura, límites de conexión y observabilidad.

## 6. Backup y restauración

Scripts:

```bash
scripts/backup.sh [salida.dump]
scripts/restore.sh <backup.dump> [base_destino]
```

Prueba ejecutada:

- Backup custom-format de PostgreSQL: 188 KB.
- Restauración en `comunidad360_restore_test`.
- Verificación: 2 usuarios y 1 comunidad restaurados.
- Base temporal eliminada después.
- Backup cifrado con AES-256/OpenSSL y restauración cifrada verificada.
- Scripts: `backup.sh`, `restore.sh`, `backup-encrypted.sh`, `restore-encrypted.sh`.
- Timer systemd diario incluido en `deploy/systemd/` y documentado en `docs/BACKUP.md`.

Salvaguardas:

- `restore.sh` bloquea la restauración sobre `comunidad360` salvo `ALLOW_PROD_RESTORE=true`.
- El backup se valida con `pg_restore --list`.
- `backups/` está excluido del repositorio.

Pendiente: programación diaria, cifrado, retención, almacenamiento externo, RPO/RTO formales y
prueba de restauración en infraestructura de producción.

## 7. CI

El workflow añade:

- Backend: `mvn verify`.
- Frontend: `check:i18n`, typecheck, tests, build y audit.
- Compose: validación de configuración.
- E2E: arranque del stack, espera de salud, Chromium, Playwright y logs en fallo.
- Seguridad: CodeQL, dependency review, Dependabot y Gitleaks.

## 8. Checklist de piloto

### Listo técnicamente

- [x] Tests backend/frontend en verde.
- [x] E2E de login y navegación crítica.
- [x] Accesibilidad automatizada sin hallazgos serios/críticos.
- [x] i18n es/ca con paridad de claves.
- [x] Backup/restore probado.
- [x] Cabeceras de seguridad básicas.
- [x] Rate limiting de autenticación.
- [x] Backup cifrado probado.
- [x] CI con E2E.

### Pendiente antes del piloto real

- [ ] Validación legal LPH/RGPD y textos informativos.
- [ ] Sustituir secretos de desarrollo y configurar cookies/HTTPS de producción.
- [ ] Rate limiting general y protección perimetral.
- [ ] Validar timer systemd y copia offsite en el host real; plantilla y cifrado disponibles.
- [x] Integración antimalware con ClamAV (activar en producción).
- [ ] Auditoría manual de accesibilidad y pruebas con usuarios.
- [ ] Prueba de carga con datos realistas.
- [ ] Monitorización, alertas y trazas centralizadas.
- [ ] Plan de soporte y formación del piloto.
- [ ] Carga inicial de la comunidad piloto.
- [ ] Criterios de go/no-go firmados.

## 9. Trazabilidad

| Historia | Estado |
| --- | --- |
| US-160 Suite de autorización | Cubierta por tests de integración |
| US-161 Suite de aislamiento multi-tenant | Cubierta por tests cruzados |
| US-162 E2E de flujos críticos | Parcial: login, dashboard y navegación; sin creación completa por UI |
| US-163 Backup y restauración | Backup/restore cifrados y timer systemd; falta validar offsite |
| US-164 Revisión de seguridad | Revisión técnica y rate limiting; falta pentest |
| US-165 Rendimiento y carga | Prueba básica local |
| US-166 Accesibilidad | Automatizada en login/dashboard; falta manual |
| US-167 RGPD/LPH | Pendiente de revisión legal |
| US-168 Onboarding y carga del piloto | Pendiente |

---

*Fin de docs/FASE4-VALIDATION.md — v0.1.0.*
