# COMUNIDAD 360 — Runbook de piloto

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Plantilla operativa |
| Fecha | 2026-11-19 |
| Objetivo | Preparar, operar y cerrar una comunidad piloto |

---

## 1. Antes de empezar

- [ ] Entorno de producción o preproducción estable.
- [ ] Dominio con HTTPS.
- [ ] SMTP verificado.
- [ ] S3 privado y backups cifrados.
- [ ] Admin de comunidad creado sin contraseña de demo.
- [ ] Contacto de soporte y canal de incidencias definidos.
- [ ] Textos legales y base jurídica revisados.
- [ ] Criterios de éxito y go/no-go acordados.

## 2. Carga inicial

1. Crear la comunidad.
2. Crear edificios y unidades.
3. Revisar coeficientes y suma.
4. Invitar a la junta y administración.
5. Cargar propietarios e inquilinos.
6. Revisar titularidades y porcentajes.
7. Crear el ejercicio fiscal actual.
8. Cargar presupuesto y cuotas iniciales.
9. Subir estatutos, actas y contratos.
10. Crear la primera reunión o aviso.

## 3. Usuarios

- Enviar invitaciones individuales; no compartir contraseñas.
- Verificar que cada usuario acepta y accede.
- Revisar roles: administración, presidencia, secretaría, tesorería, propietario, inquilino.
- Retirar accesos de personas que no participen en el piloto.

## 4. Operación diaria

- Revisar salud del sistema y errores 5xx.
- Revisar backups y espacio.
- Atender incidencias con prioridad y trazabilidad.
- Revisar notificaciones fallidas.
- No cambiar coeficientes ni cargos sin validación funcional.
- No restaurar bases sin autorización.

## 5. Incidencias

| Prioridad | Ejemplo | Respuesta |
| --- | --- | --- |
| Crítica | Caída, pérdida de datos, acceso cruzado | Inmediata; detener cambios y escalar |
| Alta | No se puede pagar, invitar o subir documento | Mismo día |
| Media | Error visual, email no recibido | 1–2 días |
| Baja | Mejora de texto o formato | Backlog |

Registrar: fecha, usuario afectado, comunidad, pasos, resultado esperado, resultado real, evidencia
y traza.

## 6. Feedback

Recoger al menos:

- Facilidad de alta y acceso.
- Claridad de cuotas y deuda.
- Utilidad de incidencias.
- Documentos y actas.
- Reuniones y notificaciones.
- Problemas de idioma, accesibilidad o móvil.

## 7. Cierre del piloto

- [ ] Métricas de uso recogidas.
- [ ] Incidencias críticas cerradas.
- [ ] Feedback clasificado.
- [ ] Backup final verificado.
- [ ] Revisión legal completada.
- [ ] Riesgos residuales aceptados.
- [ ] Decisión go/no-go firmada.

## 8. Rollback

1. Comunicar la ventana.
2. Detener escrituras.
3. Volver a la imagen anterior.
4. Restaurar backup solo si hay corrupción.
5. Verificar login, datos y documentos.
6. Registrar causa e impacto.

---

*Fin de docs/PILOT-RUNBOOK.md — v0.1.0.*
