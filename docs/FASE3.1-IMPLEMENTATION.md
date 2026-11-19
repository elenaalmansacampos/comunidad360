# COMUNIDAD 360 — FASE 3.1: Estructura de la comunidad

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Implementada; validación funcional pendiente |
| Fecha | 2026-11-19 |
| Alcance | Edificios, unidades, elementos comunes y coeficientes |

---

## 1. Resumen

Se ha implementado el primer incremento vertical de FASE 3:

- Alta, consulta, edición y archivado de edificios.
- Alta, consulta, edición y archivado de unidades.
- Tipos de unidad: vivienda, local, garaje, trastero y elemento común.
- Alta, consulta, edición y archivado de elementos comunes.
- Validación de coeficientes: suma de unidades activas frente a 100 % con tolerancia configurable.
- Pantalla de estructura en el frontend con resumen, listados y formularios de alta.
- Pruebas de integración con PostgreSQL real y aislamiento multi-tenant.

## 2. Backend

### 2.1. Migración

`backend/src/main/resources/db/migration/V3__structure.sql` crea:

- `building`
- `unit`
- `common_element`

Incluye:

- FK compuesta `(building_id, community_id)` para impedir referencias entre comunidades.
- Código de unidad único por comunidad.
- Coeficiente `NUMERIC(9,6)` con `CHECK` entre 0 y 100.
- Estados de unidad `ACTIVE`/`INACTIVE`.
- Índices por comunidad, edificio y archivado.

### 2.2. Endpoints

| Método | Ruta | Permiso |
| --- | --- | --- |
| GET | `/api/v1/communities/{cid}/buildings` | `UNIT_VIEW` |
| POST | `/api/v1/communities/{cid}/buildings` | `UNIT_MANAGE` |
| GET | `/api/v1/communities/{cid}/buildings/{id}` | `UNIT_VIEW` |
| PATCH | `/api/v1/communities/{cid}/buildings/{id}` | `UNIT_MANAGE` |
| DELETE | `/api/v1/communities/{cid}/buildings/{id}` | `UNIT_MANAGE` |
| GET | `/api/v1/communities/{cid}/units` | `UNIT_VIEW` |
| POST | `/api/v1/communities/{cid}/units` | `UNIT_MANAGE` |
| GET | `/api/v1/communities/{cid}/units/{id}` | `UNIT_VIEW` |
| PATCH | `/api/v1/communities/{cid}/units/{id}` | `UNIT_MANAGE` |
| DELETE | `/api/v1/communities/{cid}/units/{id}` | `UNIT_MANAGE` |
| GET | `/api/v1/communities/{cid}/common-elements` | `UNIT_VIEW` |
| POST | `/api/v1/communities/{cid}/common-elements` | `UNIT_MANAGE` |
| GET | `/api/v1/communities/{cid}/common-elements/{id}` | `UNIT_VIEW` |
| PATCH | `/api/v1/communities/{cid}/common-elements/{id}` | `UNIT_MANAGE` |
| DELETE | `/api/v1/communities/{cid}/common-elements/{id}` | `UNIT_MANAGE` |
| GET | `/api/v1/communities/{cid}/structure/summary` | `UNIT_VIEW` |

### 2.3. Reglas de negocio

- Una unidad pertenece a una comunidad y, opcionalmente, a un edificio de la misma comunidad.
- El código de unidad es único por comunidad.
- El coeficiente debe estar entre 0 y 100 con hasta 6 decimales.
- La suma de coeficientes de unidades activas se compara con 100 %.
- Tolerancia por defecto: `0.0001`, configurable con `APP_STRUCTURE_COEFFICIENT_TOLERANCE`.
- No se puede archivar un edificio con unidades activas.
- Archivar una unidad la marca `INACTIVE` y la excluye de listados y del resumen.
- Todas las operaciones comprueban membresía y permiso en backend.
- Crear, editar y archivar genera auditoría.

### 2.4. Respuesta de resumen

```json
{
  "coefficientSum": "100.000000",
  "tolerance": "0.0001",
  "valid": true,
  "totalUnits": 3,
  "activeUnits": 3,
  "buildings": 1,
  "commonElements": 2
}
```

## 3. Frontend

- Nueva ruta: `/communities/:communityId/structure`.
- Nuevo enlace de navegación `Estructura` en sidebar y navegación móvil.
- Tarjeta de resumen de coeficientes con estado válido/no válido.
- Listado de edificios, unidades y elementos comunes.
- Formularios de alta de edificio, unidad y elemento común.
- Formularios bilingües español/catalán.
- Invalidación automática de consultas tras crear un recurso.
- Mensajes específicos para código de unidad duplicado.
- Diseño responsive: tablas convertidas en tarjetas en móvil.

## 4. Pruebas ejecutadas

| Validación | Resultado |
| --- | --- |
| Backend `mvn test` | 15 tests, 0 fallos |
| `StructureIntegrationTest` | 6 tests, 0 fallos |
| Aislamiento entre comunidades | Correcto (`403`) |
| `OWNER` puede ver estructura y no gestionarla | Correcto (`403` al crear) |
| Código de unidad duplicado | Correcto (`409 UNIT_CODE_EXISTS`) |
| Edificio de otra comunidad | Correcto (`422 INVALID_BUILDING`) |
| Suma de coeficientes y tolerancia | Correcto |
| Archivado de unidad y edificio | Correcto |
| Frontend `npm test` | 6 tests, 0 fallos |
| Frontend `npm run typecheck` | Correcto |
| Frontend `npm run build` | Correcto |
| Smoke test del stack completo | Edificio, unidad, resumen y ruta de estructura correctos |
| OpenAPI con Redocly | Válido; 1 warning esperado por localhost |

## 5. Limitaciones conocidas

- No hay UI de edición ni archivado; el backend ya los soporta.
- No hay importación masiva de unidades (US-025).
- No se bloquea la creación de unidades cuando la suma supera 100; se informa mediante el resumen.
- No hay todavía propietarios, coeficientes por titularidad ni cargos financieros.
- No hay validación de duplicados de nombre de edificio o elemento común.
- No hay paginación en los listados de estructura; adecuado para el tamaño del piloto.
- No hay Drag & drop ni ordenación manual de unidades.
- No hay pruebas E2E de la pantalla de estructura.

## 6. Trazabilidad

| Historia | Estado |
| --- | --- |
| US-020 Alta y configuración de comunidad | Base implementada en FASE 2 |
| US-021 Gestión de edificios | Hecho |
| US-022 Gestión de unidades | Hecho |
| US-023 Elementos comunes | Hecho |
| US-024 Validación de coeficientes | Hecho con resumen y tolerancia |
| US-025 Carga inicial de unidades | Pendiente |

## 7. Próximos pasos

1. Añadir edición y archivado en la UI.
2. Implementar importación CSV de unidades.
3. Implementar FASE 3.2: propietarios, inquilinos y titularidad.
4. Añadir validación bloqueante al activar la comunidad.
5. Añadir pruebas E2E de estructura.
6. Revisar la experiencia de configuración con la comunidad piloto.

---

*Fin de docs/FASE3.1-IMPLEMENTATION.md — v0.1.0.*
