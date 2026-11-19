# ADR-0005 — Documentos en almacenamiento de objetos

- **Estado**: Aceptado (pendiente de validación en FASE 1)
- **Fecha**: 2026-11-19
- **Decisores**: Software Architect, Security Lead, DevSecOps

## Contexto

La plataforma gestiona actas, estatutos, contratos, facturas, presupuestos, informes técnicos,
certificados, fotografías de incidencias y adjuntos de chat. Los archivos pueden ser grandes, sensibles y
estar sujetos a retención legal.

## Decisión

- Almacenar el contenido de los archivos en **almacenamiento de objetos compatible con S3**.
- Guardar en PostgreSQL solo metadatos: propietario, comunidad, categoría, visibilidad, versión, nombre,
  MIME, tamaño, hash y estado de análisis.
- Usar claves opacas generadas por la plataforma; nunca rutas basadas en el nombre original.
- Servir descargas mediante **URLs firmadas de corta duración** tras validar permisos.
- Versionar documentos sin sobrescribir versiones anteriores.
- Analizar los archivos antes de publicarlos (antivirus/malware) y mantener cuarentena si el análisis
  falla.
- Aplicar cifrado en reposo y en tránsito.
- Registrar auditoría de subida, descarga, modificación y borrado lógico.

## Alternativas consideradas

### Guardar archivos en PostgreSQL (`bytea`)

- **Ventajas**: una sola fuente de datos, backup conjunto.
- **Inconvenientes**: base de datos pesada, backups lentos, mal rendimiento y escalado.
- **Motivo de descarte**: no apto para documentos y adjuntos.

### Guardar archivos en el sistema de ficheros local

- **Ventajas**: simplicidad en un solo servidor.
- **Inconvenientes**: no escala, backups y permisos manuales, riesgo en contenedores.
- **Motivo de descarte**: no cumple la evolución SaaS.

### Almacenamiento de objetos con URLs firmadas

- **Ventajas**: escalable, económico, permisos centralizados, separación de datos.
- **Inconvenientes**: dependencia de proveedor, necesidad de gestionar claves y cuarentena.
- **Motivo de elección**: mejor equilibrio para el producto.

## Consecuencias

### Positivas

- Base de datos ligera y backups rápidos.
- Escalado sencillo.
- Control de acceso centralizado.

### Negativas / riesgos

- Dependencia de un proveedor S3 (Q-05).
- Necesidad de política de ciclo de vida y retención.
- Riesgo de URLs filtradas: deben ser cortas y de un solo uso cuando sea posible.
- Necesidad de antivirus y validación de MIME real.

## Reglas de seguridad

- Ningún bucket público.
- Cifrado en reposo.
- URLs firmadas con expiración corta.
- Validación de permisos antes de firmar.
- Nombres saneados y claves opacas.
- Límites de tamaño y tipos permitidos (Q-13).
- Registro de accesos y descargas de documentos sensibles.
