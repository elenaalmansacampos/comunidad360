# Contribuir a COMUNIDAD 360

## 1. Principios

- Spec Driven Development: primero especificación, después implementación.
- Cambios pequeños, revisables y verificables.
- Seguridad y aislamiento multi-tenant no son opcionales.
- Documentación sincronizada con el código.
- Nunca incluir secretos, credenciales ni datos personales reales.

## 2. Flujo de trabajo

1. Leer el requisito en `docs/SPEC.md` y la historia en `docs/BACKLOG.md`.
2. Verificar que la historia cumple Definition of Ready.
3. Crear una rama descriptiva.
4. Implementar en incrementos pequeños.
5. Añadir o actualizar pruebas.
6. Actualizar documentación y traducciones.
7. Ejecutar la suite local.
8. Abrir Pull Request con contexto, riesgos y pruebas.
9. Revisar y corregir.
10. No marcar como terminado sin cumplir Definition of Done.

### Ramas

```text
feature/<id>-<descripcion-corta>
fix/<id>-<descripcion-corta>
docs/<descripcion-corta>
chore/<descripcion-corta>
```

Ejemplos:

```text
feature/US-043-cuotas-ordinarias
fix/US-051-redondeo-centimos
docs/adr-multitenancy
```

### Commits

Usar Conventional Commits:

```text
feat(finance): generar cargos por cuota
fix(auth): revocar refresh tokens al cambiar contraseña
docs(adr): registrar decisión de multi-tenancy
test(security): añadir aislamiento de incidencias
```

Reglas:

- Un commit por cambio lógico.
- Mensaje en imperativo y descriptivo.
- No incluir secretos.
- No mezclar refactorizaciones grandes con funcionalidades.

## 3. Convenciones de código

### Backend

- Java 21.
- Paquete raíz `com.comunidad360`.
- Monolito modular por módulos de negocio.
- Capas: controller, service, repository, entity, dto, mapper, exception, validation.
- Sin dependencias circulares.
- Entidades JPA no se exponen en la API.
- DTOs y mappers en la frontera.
- `BigDecimal` para dinero; nunca `double`.
- Validación en servidor.
- Autorización explícita por caso de uso.
- `community_id` en toda entidad de negocio.
- Migraciones Flyway versionadas.
- Logs estructurados sin datos sensibles.

### Frontend

- TypeScript estricto.
- React funcional y hooks.
- Componentes sin textos de interfaz incrustados.
- TanStack Query para estado de servidor.
- Zustand solo para estado global de cliente.
- Sass con tokens del Design System.
- Accesibilidad WCAG 2.1 AA.
- Mobile-first.
- Formularios con validación en cliente y servidor.
- No usar `dangerouslySetInnerHTML` con contenido de usuario.

### Base de datos

- Tablas y columnas en `snake_case`.
- Importes `NUMERIC(19,4)`.
- Fechas `TIMESTAMPTZ` en UTC.
- Claves foráneas compuestas por tenant cuando aplique.
- Índices justificados.
- Sin borrado físico de datos financieros o de auditoría.
- Migraciones inmutables una vez aplicadas en entornos compartidos.

## 4. Internacionalización

- Todos los textos visibles pasan por `i18next`.
- Claves semánticas: `<module>.<section>.<element>`.
- Archivos:
  - `frontend/src/locales/es/translation.json`
  - `frontend/src/locales/ca/translation.json`
- No concatenar fragmentos traducidos.
- Usar plurales ICU.
- Fechas y números con `Intl`.
- Emails y errores de backend con claves y locale del destinatario.
- El catalán debe ser natural y administrativo; revisión humana obligatoria.
- No traducir contenido escrito por usuarios ni documentos oficiales.

## 5. Pruebas

Antes de abrir un Pull Request:

```bash
# Backend (requiere Docker para Testcontainers)
cd backend
mvn test

# Frontend
cd frontend
npm ci
npm test
npm run typecheck
npm run build
npm audit --omit=dev

# E2E (pendiente de configurar)
# npm run test:e2e
```

Pruebas mínimas por cambio:

- Unitaria para lógica nueva.
- Integración para persistencia y API.
- Autorización positiva y negativa.
- Aislamiento multi-tenant si hay datos.
- Cálculos financieros y redondeos.
- Renderizado bilingüe si hay interfaz.
- E2E para flujos críticos modificados.

No se declaran pruebas satisfactorias sin ejecutarlas y adjuntar evidencia.

## 6. Seguridad

- No incluir secretos en Git.
- No registrar tokens, contraseñas, IBAN ni datos personales innecesarios.
- Validar entradas y autorizar en backend.
- Revisar accesos a documentos y archivos.
- No confiar en el frontend para permisos.
- No introducir URLs externas sin validación.
- No añadir dependencias sin revisar licencia y vulnerabilidades.
- Reportar vulnerabilidades de forma privada al responsable del proyecto.

## 7. Documentación

Actualizar cuando aplique:

- `docs/SPEC.md` si cambia el alcance.
- `docs/ARCHITECTURE.md` si cambia la arquitectura.
- `docs/DATABASE.md` si cambia el modelo.
- `docs/API.md` si cambia un contrato.
- `docs/SECURITY.md` si cambia un control.
- `docs/DESIGN-SYSTEM.md` si cambia la interfaz.
- `docs/ROADMAP.md` y `docs/BACKLOG.md` si cambia el plan.
- `docs/ADR/` si hay una decisión arquitectónica relevante.

Un cambio de alcance no se implementa sin actualizar la especificación.

## 8. Pull Requests

Incluir:

- Qué se cambia y por qué.
- Requisito o historia relacionada.
- Archivos principales afectados.
- Pruebas ejecutadas y resultado.
- Impacto en seguridad, privacidad, multi-tenancy e i18n.
- Capturas o evidencia si hay UI.
- Riesgos y trabajo pendiente.

Checklist:

- [ ] Cumple criterios de aceptación.
- [ ] Tests en verde.
- [ ] Autorización y aislamiento probados.
- [ ] Textos en `es` y `ca`.
- [ ] Documentación actualizada.
- [ ] Sin secretos.
- [ ] Sin dependencias innecesarias.
- [ ] Sin errores críticos.

## 9. Definition of Done

1. Criterios de aceptación cumplidos.
2. Código implementado y revisado.
3. Pruebas unitarias, integración y E2E aplicables en verde.
4. Seguridad y multi-tenancy verificados.
5. i18n completo.
6. Documentación actualizada.
7. Sin vulnerabilidades críticas/altas sin mitigar.
8. Cambios versionados en Git.
9. Demostración o validación realizada.

## 10. Datos y privacidad

- Usar datos sintéticos en desarrollo y pruebas.
- No copiar datos reales de producción a entornos inferiores sin anonimizar.
- No incluir datos personales en issues, capturas o logs.
- Respetar la retención y el borrado definidos.
