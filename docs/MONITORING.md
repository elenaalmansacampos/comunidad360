# COMUNIDAD 360 — Monitorización y alertas

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Guía operativa para piloto |
| Fecha | 2026-11-20 |

---

## 1. Objetivo

Detectar caídas, errores y degradación antes de que afecten a la comunidad piloto, y poder
correlacionar una incidencia con una petición concreta.

## 2. Comprobaciones rápidas

Smoke test de producción (frontend, health, CORS y login):

```bash
scripts/smoke-test.sh
# o con otras URLs:
FRONTEND_URL=https://comunidad360-web-q2ln.onrender.com \
API_URL=https://comunidad360-api-ihic.onrender.com \
scripts/smoke-test.sh
```

Health público:

```bash
curl -fsS https://comunidad360-api-ihic.onrender.com/actuator/health
```

## 3. Uptime y alertas externas

El plan gratuito de Render suspende el backend por inactividad y puede tardar 1-4 minutos en
despertar. Para el piloto, configura un monitor externo (por ejemplo UptimeRobot, Better Stack o
Pingdom):

- URL: `https://comunidad360-api-ihic.onrender.com/actuator/health`
- Intervalo: 5 minutos.
- Alerta si el código no es `200` o el cuerpo no contiene `"status":"UP"`.
- Segundo monitor para el frontend: `https://comunidad360-web-q2ln.onrender.com/` (200 y HTML de la app).
- Destinatario: canal de soporte definido en [`PILOT-RUNBOOK.md`](./PILOT-RUNBOOK.md).

Limitación: el cold start del plan free puede generar falsos positivos. Acepta un reintento antes
de escalar y revisa Render → Events/Logs.

## 4. Métricas Prometheus

El endpoint `/actuator/prometheus` existe y aplica seguridad (no es público). Hoy requiere
autenticación, así que no se puede raspar directamente desde un Prometheus externo sin un mecanismo
de token estable.

Para el piloto se usan health + logs + uptime externo. Si se monta Prometheus/Grafana:

1. Desplegar el scraper dentro de la misma red privada que la API.
2. Añadir un token de raspado de larga duración (propuesta POST-MVP) o exponer el endpoint solo en
   una red interna.
3. Alertas mínimas: API caída, tasa de 5xx, latencia p95, errores de base de datos y pool Hikari.

## 5. Trazas y logs

- Cada respuesta incluye `X-Trace-Id`.
- `TraceIdFilter` propaga el identificador a los logs y al cuerpo de error (`traceId`).
- Al reportar una incidencia, adjuntar el `X-Trace-Id` y la hora exacta para localizar la traza.

## 6. Alertas recomendadas

| Alerta | Condición | Acción |
| --- | --- | --- |
| API caída | health no `UP` en 2 comprobaciones | Revisar Events/Logs y reiniciar |
| Errores 5xx | aumento sostenido | Revisar logs por `traceId` |
| Cold start excesivo | arranque > 5 min | Revisar memoria y tamaño del build |
| Backup ausente | no hay backup diario | Ver [`BACKUP.md`](./BACKUP.md) |
| Certificado/HTTPS | error TLS o dominio | Revisar dominio y Render |
| Base de datos | errores de conexión/Hikari | Revisar estado de Render PostgreSQL |

## 7. Limitaciones del piloto en plan free

- Sin WAF ni alertas de seguridad gestionadas.
- Métricas Prometheus no raspables sin token interno.
- Sin trazas centralizadas (solo correlación por `X-Trace-Id` en logs).
- Para producción real: plan de pago, monitorización gestionada y backups offsite.

---

*Fin de docs/MONITORING.md — v0.1.0.*
