# ADR-0007 — Chat en tiempo real con WebSocket/STOMP

- **Estado**: Aceptado condicionado al alcance MVP+
- **Fecha**: 2026-11-19
- **Decisores**: Software Architect, Backend Lead, Frontend Lead

## Contexto

El módulo de chat incluye canal general, canales temáticos y conversaciones privadas con mensajes en
tiempo real, adjuntos, notificaciones y moderación. El MVP puede posponerlo si el piloto no lo confirma
(Q-11).

## Decisión

- Usar **Spring WebSocket con STOMP** sobre `/ws`.
- Autenticar la conexión y validar membresía y permiso al suscribirse a cada canal.
- Persistir todos los mensajes en PostgreSQL.
- Almacenar adjuntos en S3 con las mismas reglas que los documentos.
- Notificar de forma asíncrona a los destinatarios.
- Moderación con registro de auditoría.
- En el MVP, sin broker externo. Si se escala horizontalmente, introducir broker compatible o Redis
  pub/sub, documentado en un nuevo ADR.

## Alternativas consideradas

### Polling HTTP

- **Ventajas**: simplicidad.
- **Inconvenientes**: latencia, carga, mala experiencia de chat.
- **Motivo de descarte**: no cumple "tiempo real" con calidad.

### Server-Sent Events (SSE) + REST

- **Ventajas**: simple, unidireccional.
- **Inconvenientes**: necesita otra vía para enviar; menos natural para chat bidireccional.
- **Motivo de descarte**: WebSocket encaja mejor.

### WebSocket/STOMP

- **Ventajas**: bidireccional, integración con Spring, tópicos por canal.
- **Inconvenientes**: estado de conexión, escalado, reconexión.
- **Motivo de elección**: buena integración con el stack y requisitos.

## Consecuencias

### Positivas

- Mensajes en tiempo real.
- Integración natural con Spring Security.
- Separación por tópicos de comunidad/canal.

### Negativas / riesgos

- Escalado horizontal requiere broker.
- Reconexión y entrega de mensajes deben probarse.
- Moderación y permisos son críticos para evitar fugas.

## Reglas de seguridad

- Validar token al conectar y al suscribirse.
- No confiar en el `communityId` o `channelId` enviados sin comprobar membresía.
- Limitar tamaño y tipo de mensajes y adjuntos.
- Prevenir XSS en el renderizado de mensajes.
- Registrar acciones de moderación.
- No incluir datos sensibles en notificaciones de chat.
