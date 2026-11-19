# ADR-0001 — Monolito modular

- **Estado**: Aceptado (pendiente de validación en FASE 1)
- **Fecha**: 2026-11-19
- **Decisores**: Software Architect, Backend Lead, Product Owner

## Contexto

COMUNIDAD 360 debe cubrir múltiples módulos de negocio (comunidades, finanzas, documentos,
incidencias, reuniones, chat) para una comunidad piloto y, después, para múltiples clientes. El equipo es
pequeño y el tiempo de comercialización importa. La seguridad y el aislamiento multi-tenant son críticos.

## Decisión

Implementar un **monolito modular** en Spring Boot con paquetes de negocio bien delimitados, capas
internas y dependencias explícitas. Un único artefacto desplegable en el MVP, con posibilidad de separar
procesos auxiliares (worker de notificaciones) cuando sea necesario.

## Alternativas consideradas

### Microservicios desde el inicio

- **Ventajas**: escalado independiente, límites físicos entre servicios.
- **Inconvenientes**: coste operativo, transacciones distribuidas, duplicación de infraestructura,
  complejidad de observabilidad y despliegue para un equipo pequeño.
- **Motivo de descarte**: sobreingeniería para el alcance y el equipo actual.

### Monolito sin módulos

- **Ventajas**: arranque rápido.
- **Inconvenientes**: acoplamiento, difícil de mantener y de separar en el futuro.
- **Motivo de descarte**: contradice la mantenibilidad y la evolución SaaS.

### Monolito modular

- **Ventajas**: límites lógicos claros, un solo despliegue, transacciones locales, evolución gradual.
- **Inconvenientes**: requiere disciplina y verificación automática de dependencias.
- **Motivo de elección**: mejor equilibrio entre simplicidad, seguridad y evolución.

## Consecuencias

### Positivas

- Menor complejidad operativa y de despliegue.
- Transacciones ACID locales para finanzas.
- Una única base de datos y un único modelo de seguridad.
- Posibilidad de extraer módulos en el futuro si aparecen cuellos de botella.

### Negativas / riesgos

- Riesgo de acoplamiento si no se respetan las reglas de dependencia.
- Escalado conjunto del backend.
- Necesidad de validar dependencias con ArchUnit y revisiones.

## Reglas asociadas

- Sin dependencias circulares.
- Acceso a otros módulos solo mediante interfaces de servicio.
- `common` y `security` no dependen de módulos de negocio.
- No se crean microservicios sin un ADR que lo justifique.
