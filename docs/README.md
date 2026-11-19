# Documentación de COMUNIDAD 360

Estado: **FASE 0 — Discovery y especificación**. No existe código de producción.

## Índice

| Documento | Contenido |
| --- | --- |
| [`SPEC.md`](./SPEC.md) | Visión, alcance, requisitos funcionales, i18n, reglas de negocio, MVP y criterios de éxito |
| [`ARCHITECTURE.md`](./ARCHITECTURE.md) | Monolito modular, multi-tenancy, módulos, frontend, despliegue y pruebas |
| [`DATABASE.md`](./DATABASE.md) | Modelo ER, diccionario de datos, integridad multi-tenant e índices |
| [`API.md`](./API.md) | Convenciones, errores, paginación, catálogo inicial y WebSocket |
| [`SECURITY.md`](./SECURITY.md) | Amenazas, autenticación, autorización, RGPD/LOPDGDD, LPH y archivos |
| [`DESIGN-SYSTEM.md`](./DESIGN-SYSTEM.md) | Identidad visual, tokens, componentes, accesibilidad e i18n |
| [`ROADMAP.md`](./ROADMAP.md) | Fases, entregables, dependencias y criterios de salida |
| [`BACKLOG.md`](./BACKLOG.md) | Épicas, historias de usuario y criterios de aceptación |
| [`FASE1-DESIGN.md`](./FASE1-DESIGN.md) | Propuestas de decisión y criterios de salida de FASE 1 |
| [`WIREFRAMES.md`](./WIREFRAMES.md) | Navegación y wireframes mobile-first de flujos críticos |
| [`openapi.yaml`](./openapi.yaml) | Contrato OpenAPI v0 del núcleo del MVP |
| [`FASE2-IMPLEMENTATION.md`](./FASE2-IMPLEMENTATION.md) | Fundaciones implementadas, pruebas y limitaciones |
| [`FASE3.1-IMPLEMENTATION.md`](./FASE3.1-IMPLEMENTATION.md) | Estructura: edificios, unidades, elementos comunes y coeficientes |
| [`FASE3.2-IMPLEMENTATION.md`](./FASE3.2-IMPLEMENTATION.md) | Titularidad, copropiedad, inquilinos e histórico |
| [`FASE3.3-IMPLEMENTATION.md`](./FASE3.3-IMPLEMENTATION.md) | Finanzas: ejercicios, cuotas, pagos, facturas y apuntes |
| [`FASE3.4-IMPLEMENTATION.md`](./FASE3.4-IMPLEMENTATION.md) | Banca: cuentas, CSV, movimientos y conciliación |
| [`FASE3.5-IMPLEMENTATION.md`](./FASE3.5-IMPLEMENTATION.md) | Incidencias, seguimiento, comentarios y fotografías |
| [`ADR/`](./ADR/) | Decisiones arquitectónicas registradas |

## Orden de lectura recomendado

1. `SPEC.md`
2. `ROADMAP.md`
3. `ARCHITECTURE.md`
4. `DATABASE.md`
5. `SECURITY.md`
6. `API.md`
7. `DESIGN-SYSTEM.md`
8. `WIREFRAMES.md`
9. `FASE1-DESIGN.md`
10. `openapi.yaml`
11. `BACKLOG.md`
12. `FASE2-IMPLEMENTATION.md`
13. `FASE3.1-IMPLEMENTATION.md`
14. `FASE3.2-IMPLEMENTATION.md`
15. `FASE3.3-IMPLEMENTATION.md`
16. `FASE3.4-IMPLEMENTATION.md`
17. `FASE3.5-IMPLEMENTATION.md`
18. `ADR/`

## Reglas de mantenimiento

- La especificación es la fuente de verdad funcional.
- Un cambio de alcance se documenta antes de implementarse.
- Las decisiones arquitectónicas relevantes se registran como ADR.
- La documentación debe estar sincronizada con el código.
- No se incluyen secretos ni datos personales reales.
