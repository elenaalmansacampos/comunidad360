# COMUNIDAD 360 — FASE 3.8: Dashboard e informes

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Implementada; validación funcional completada |
| Fecha | 2026-11-19 |
| Alcance | Dashboard por rol, indicadores, widgets, evolución e informe PDF |

---

## 1. Resumen

Se ha implementado el octavo incremento vertical de FASE 3:

- Dashboard enriquecido y adaptado al rol.
- Indicadores financieros de comunidad o propietario.
- Incidencias abiertas y últimas incidencias visibles.
- Próximas reuniones convocadas.
- Evolución mensual de ingresos y gastos.
- Informe PDF básico con permisos y auditoría.
- Acceso directo a los módulos desde el dashboard.

## 2. Backend

### 2.1. Dashboard

Endpoint: `GET /api/v1/communities/{communityId}/dashboard`.

Respuesta:

- `community`, `roles`, `activeMembers`.
- `openIncidents`: incidencias abiertas, en curso o pendientes de proveedor.
- `recentIncidents`: últimas cinco incidencias visibles para el usuario.
- `upcomingMeetings`: próximas cinco reuniones `CONVENED`.
- `finance`: resumen del ejercicio actual si el usuario tiene `FINANCE_VIEW`.
- `monthlyFinance`: 12 meses de ingresos/gastos para staff.
- `canViewReports`: indica si el usuario puede descargar el informe.

### 2.2. Alcance por rol

- Staff con `FINANCE_VIEW`: ve ingresos, gastos, saldo, cuotas pendientes y facturas pendientes de la comunidad.
- Propietario/inquilino: ve únicamente sus cuotas pendientes (`scope = OWN`).
- Incidencias: se reutiliza la visibilidad del módulo de incidencias.
- Reuniones: solo se muestran a usuarios con `MEETING_VIEW`.
- Informe PDF: requiere `REPORT_VIEW`.
- Todas las lecturas se resuelven dentro de la comunidad activa.

### 2.3. Evolución mensual

`ledger_entry` se agrupa por mes para el ejercicio actual:

- Solo apuntes `POSTED`.
- Ingresos y gastos separados.
- Siempre se devuelven 12 meses, con ceros si no hay movimientos.
- Los importes del dashboard se normalizan a dos decimales.

### 2.4. Informe PDF

Endpoint: `GET /api/v1/communities/{communityId}/reports/summary`.

- Requiere `REPORT_VIEW`.
- Genera un PDF con PDFBox.
- Incluye comunidad, fecha, versión, miembros, incidencias, finanzas, próximas reuniones y últimas incidencias.
- Descarga como `informe-comunidad.pdf`.
- Registra auditoría `REPORT_EXPORTED`.
- No se genera ni almacena en S3.

## 3. Frontend

- Dashboard responsive con tarjetas de resumen.
- Widget de incidencias abiertas y acceso al listado.
- Widget financiero adaptado a comunidad o propietario.
- Gráfico CSS de evolución mensual de ingresos/gastos.
- Lista de próximas reuniones con enlace al detalle.
- Lista de últimas incidencias con estado y prioridad.
- Botón de descarga del informe PDF para perfiles autorizados.
- Enlaces directos a finanzas, incidencias, documentos y reuniones.
- Textos en español y catalán.

## 4. Pruebas ejecutadas

| Validación | Resultado |
| --- | --- |
| Backend `mvn test` | 46 tests, 0 fallos |
| `DashboardIntegrationTest` | 3 tests, 0 fallos |
| Dashboard de staff con finanzas y widgets | Correcto |
| Dashboard de propietario con datos propios | Correcto |
| Dashboard sin ejercicio fiscal | Correcto, sin error |
| Aislamiento entre comunidades | Correcto (`403`) |
| Informe PDF | Correcto (`%PDF`, `200`) |
| Informe sin `REPORT_VIEW` | Correcto (`403`) |
| Frontend `npm test` | 26 tests, 0 fallos |
| Frontend `npm run typecheck` | Correcto |
| Frontend `npm run build` | Correcto |
| Smoke test | Dashboard con finanzas, incidencias, reunión y PDF correctos |

## 5. Limitaciones conocidas

- El informe PDF es de una página y sin gráficos.
- No hay exportación CSV/Excel adicional.
- No hay comparación con ejercicios anteriores.
- No hay widgets configurables ni orden persistente.
- No hay informes programados ni envío por email.
- No hay dashboard de plataforma para `SUPER_ADMIN`.
- No hay pruebas E2E.
- La sección de dashboard e informes del OpenAPI v0 no está sincronizada por completo.

## 6. Trazabilidad

| Historia | Estado |
| --- | --- |
| US-110 Dashboard por rol | Hecho |
| US-111 Indicadores financieros | Hecho |
| US-112 Widgets de incidencias y reuniones | Hecho |
| US-113 Informe PDF básico | Hecho |

## 7. Próximos pasos

1. Implementar FASE 3.9: MVP+ condicionado (chat, proveedores, votaciones, push).
2. Añadir exportaciones CSV/Excel y comparativas.
3. Incorporar gráficos al PDF y programación de informes.
4. Sincronizar OpenAPI y añadir pruebas E2E.

---

*Fin de docs/FASE3.8-IMPLEMENTATION.md — v0.1.0.*
