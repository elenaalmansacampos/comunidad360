# ADR-0002 — Multi-tenancy con esquema compartido

- **Estado**: Aceptado (pendiente de validación en FASE 1)
- **Fecha**: 2026-11-19
- **Decisores**: Software Architect, Database Architect, DevSecOps, Security Lead

## Contexto

La plataforma debe gestionar múltiples comunidades con datos totalmente aislados. Un usuario puede
pertenecer a varias comunidades con roles distintos. El MVP debe ser económico y operable por un equipo
pequeño, sin renunciar a seguridad.

## Decisión

Usar **una base de datos PostgreSQL compartida, un esquema compartido y un discriminador
`community_id`** en todas las tablas de negocio, con defensa en profundidad:

1. Contexto de tenant derivado de la autenticación y la ruta.
2. Guard de autorización que valida membresía y permiso.
3. Repositorios que filtran siempre por `community_id`.
4. Claves foráneas compuestas `(id, community_id)` para integridad cruzada.
5. Pruebas automatizadas de aislamiento.
6. Evaluación de Row Level Security (RLS) como capa adicional en FASE 2.

## Alternativas consideradas

### Base de datos por comunidad

- **Ventajas**: aislamiento físico fuerte, backup/restauración por cliente.
- **Inconvenientes**: muchas bases, migraciones múltiples, coste y complejidad operativa.
- **Motivo de descarte**: innecesario para el tamaño del piloto y el equipo.

### Esquema por comunidad

- **Ventajas**: aislamiento medio, separación lógica.
- **Inconvenientes**: migraciones por esquema, gestión de conexiones, herramientas.
- **Motivo de descarte**: complejidad sin beneficio claro en el MVP.

### Esquema compartido con `community_id`

- **Ventajas**: una migración, consultas y operación simples, coste bajo.
- **Inconvenientes**: un error de filtrado puede causar fuga de datos.
- **Motivo de elección**: equilibrio adecuado con controles y pruebas estrictas.

## Consecuencias

### Positivas

- Operación y despliegue sencillos.
- Migraciones únicas.
- Facilidad para incorporar nuevas comunidades.
- Coste de infraestructura contenido.

### Negativas / riesgos

- El aislamiento depende de la disciplina del código.
- Un fallo en un repositorio puede exponer datos.
- Las consultas deben revisarse con especial cuidado.

## Controles obligatorios

- `community_id NOT NULL` en entidades de negocio.
- FKs compuestas y restricciones de integridad.
- Tenant guard en cada caso de uso.
- Pruebas de acceso cruzado en cada módulo.
- Revisión de código específica para consultas de negocio.
- Auditoría de accesos de `SUPER_ADMIN`.
- RLS opcional como segunda barrera, nunca como única.
