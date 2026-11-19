# Registro de decisiones arquitectónicas (ADR)

Cada decisión relevante se documenta con contexto, alternativas, decisión y consecuencias.

| ADR | Título | Estado |
| --- | --- | --- |
| [ADR-0001](./ADR-0001-monolito-modular.md) | Monolito modular | Aceptado |
| [ADR-0002](./ADR-0002-multitenancy.md) | Multi-tenancy con esquema compartido | Aceptado |
| [ADR-0003](./ADR-0003-autenticacion-jwt.md) | JWT y refresh tokens rotatorios | Aceptado |
| [ADR-0004](./ADR-0004-modelo-financiero.md) | Modelo financiero y precisión decimal | Aceptado |
| [ADR-0005](./ADR-0005-documentos-s3.md) | Documentos en almacenamiento de objetos | Aceptado |
| [ADR-0006](./ADR-0006-i18n.md) | Internacionalización español/catalán | Aceptado |
| [ADR-0007](./ADR-0007-chat-websocket.md) | Chat con WebSocket/STOMP | Condicionado a MVP+ |

## Cuándo crear un ADR

- Cambio de arquitectura, persistencia, seguridad o integración externa.
- Introducción de una nueva tecnología relevante.
- Decisión que afecte a multi-tenancy, privacidad o datos financieros.
- Cambio de alcance que altere contratos entre módulos.

## Plantilla

```markdown
# ADR-XXXX — Título

- Estado: Propuesto | Aceptado | Rechazado | Sustituido
- Fecha: YYYY-MM-DD
- Decisores:

## Contexto

## Decisión

## Alternativas consideradas

## Consecuencias

## Referencias
```
