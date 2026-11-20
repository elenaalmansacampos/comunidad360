# COMUNIDAD 360 — Criterios de go/no-go del piloto

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Plantilla de decisión |
| Fecha | 2026-11-20 |

---

## 1. Cómo se usa

Rellenar antes de abrir el piloto a usuarios reales. Cada criterio debe tener evidencia
(enlace, captura, log o test). La decisión final se firma en la sección 4.

## 2. Criterios técnicos

- [ ] Smoke test de producción en verde (`scripts/smoke-test.sh`).
- [ ] `/actuator/health` = `UP` de forma estable.
- [ ] Frontend y API accesibles por HTTPS con las URLs reales de Render.
- [ ] CORS limitado al frontend real y a los orígenes aprobados.
- [ ] Backup diario configurado y una restauración verificada.
- [ ] Copia offsite o almacenamiento externo operativo.
- [ ] Rate limiting de autenticación y global activos.
- [ ] Secretos de producción distintos de los de desarrollo (`ProdSecurityGuard` en verde).
- [ ] `APP_BOOTSTRAP_ENABLED=false` y `APP_BOOTSTRAP_RESET_PASSWORD=false` tras crear el admin.
- [ ] Monitor de uptime externo con alertas a un canal atendido.
- [ ] Logs revisados sin errores críticos recurrentes.

## 3. Criterios de producto y cumplimiento

- [ ] Flujos críticos validados con usuarios: login, alta, cuotas, incidencias, documentos, reuniones.
- [ ] Textos legales LPH/RGPD revisados y publicados.
- [ ] Base jurídica y política de retención definidas.
- [ ] Auditoría manual de accesibilidad (teclado, foco, lector de pantalla) realizada.
- [ ] Datos de la comunidad piloto cargados y revisados (coeficientes y cuotas).
- [ ] Plan de soporte y formación entregado ([`PILOT-TRAINING.md`](./PILOT-TRAINING.md)).
- [ ] Criterios de éxito medibles acordados con el Product Owner.

## 4. Decisión

| Rol | Nombre | Decisión | Fecha |
| --- | --- | --- | --- |
| Product Owner |  |  |  |
| Responsable técnico |  |  |  |
| Administración de la comunidad |  |  |  |

Resultado (marcar uno):

- [ ] **GO**: todos los criterios bloqueantes cumplidos.
- [ ] **GO condicionado**: se permite el piloto con mitigaciones y fecha límite para los pendientes.
- [ ] **NO-GO**: hay un riesgo alto abierto; no se abre el piloto.

Condiciones o riesgos residuales aceptados:

```text

```

## 5. Después del piloto

- Recoger uso e incidencias (ver [`PILOT-RUNBOOK.md`](./PILOT-RUNBOOK.md)).
- Verificar backup final.
- Clasificar feedback y priorizar correcciones.
- Repetir esta plantilla para la decisión de comercialización.

---

*Fin de docs/PILOT-GO-NO-GO.md — v0.1.0.*
