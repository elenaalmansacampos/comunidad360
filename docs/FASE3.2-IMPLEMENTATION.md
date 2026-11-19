# COMUNIDAD 360 — FASE 3.2: Propietarios, inquilinos y titularidad

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Implementada; validación funcional pendiente |
| Fecha | 2026-11-19 |
| Alcance | Titularidad, copropiedad, inquilinos e histórico |

---

## 1. Resumen

Se ha implementado el segundo incremento vertical de FASE 3:

- Alta de titularidad por unidad y persona.
- Copropiedad con porcentajes y tipos de titularidad.
- Histórico temporal mediante `valid_from` y `valid_to`.
- Finalización de titularidad conservando histórico.
- Autorización y revocación de inquilinos con vigencia.
- Validación de suma de porcentajes activos.
- Control de acceso: administración, propietario e inquilino.
- Pantalla de detalle de unidad con titularidad y alquileres.

## 2. Backend

### 2.1. Migración

`backend/src/main/resources/db/migration/V4__ownership_tenancy.sql` crea:

- `property_ownership`
- `tenancy`

Incluye:

- FK compuesta `(unit_id, community_id)`.
- Tipos de titularidad `OWNER`, `CO_OWNER`, `USUFRUCTUARY`, `NAKED_OWNER`.
- Porcentaje `NUMERIC(7,4)` entre 0 y 100.
- Vigencias `valid_from` / `valid_to`.
- Estado de alquiler `ACTIVE`, `ENDED`, `REVOKED`.
- Índice único parcial para evitar dos titularidades abiertas de la misma persona en la misma unidad.
- Índice único parcial para evitar dos alquileres activos de la misma persona en la misma unidad.
- Versión optimista para concurrencia.

### 2.2. Endpoints

| Método | Ruta | Permiso |
| --- | --- | --- |
| GET | `/api/v1/communities/{cid}/units/{unitId}/ownerships` | `OWNERSHIP_MANAGE`, `MEMBER_VIEW` o acceso propio |
| POST | `/api/v1/communities/{cid}/units/{unitId}/ownerships` | `OWNERSHIP_MANAGE` |
| PATCH | `/api/v1/communities/{cid}/units/{unitId}/ownerships/{id}` | `OWNERSHIP_MANAGE` |
| DELETE | `/api/v1/communities/{cid}/units/{unitId}/ownerships/{id}` | `OWNERSHIP_MANAGE` |
| GET | `/api/v1/communities/{cid}/units/{unitId}/tenancies` | `OWNERSHIP_MANAGE`, `MEMBER_VIEW` o acceso propio |
| POST | `/api/v1/communities/{cid}/units/{unitId}/tenancies` | `OWNERSHIP_MANAGE` |
| PATCH | `/api/v1/communities/{cid}/units/{unitId}/tenancies/{id}` | `OWNERSHIP_MANAGE` |
| DELETE | `/api/v1/communities/{cid}/units/{unitId}/tenancies/{id}` | `OWNERSHIP_MANAGE` |

### 2.3. Reglas de negocio

- La persona titular o inquilina debe ser miembro activo de la comunidad.
- No se permite una segunda titularidad activa de la misma persona en la misma unidad.
- La suma de titularidades activas no puede superar el 100 %.
- `valid_to` es exclusivo: una titularidad con `valid_to` igual a la fecha actual ya no está activa.
- Finalizar una titularidad conserva el registro histórico.
- No se permite un segundo alquiler activo de la misma persona en la misma unidad.
- Revocar un alquiler conserva el registro histórico y cambia el estado a `REVOKED`.
- Crear, editar, finalizar o revocar genera auditoría.
- El acceso de lectura se permite a administración, a personas con `MEMBER_VIEW`, y a titulares o inquilinos de la unidad.

### 2.4. Respuesta de titularidad

```json
{
  "id": "uuid",
  "userId": "uuid",
  "fullName": "Ana Pérez",
  "email": "ana@example.com",
  "ownershipType": "OWNER",
  "sharePercentage": "60.0000",
  "validFrom": "2026-11-19",
  "validTo": null,
  "active": true,
  "version": 0
}
```

## 3. Frontend

- Nueva ruta: `/communities/:communityId/units/:unitId`.
- Enlace desde el código de unidad en la pantalla de estructura.
- Resumen de unidad: edificio, tipo, coeficiente, estado y cuota activa total.
- Tabla de titularidad con persona, tipo, porcentaje, vigencia y estado.
- Tabla de alquileres con persona, vigencia y estado.
- Formulario de alta de titular para administración.
- Formulario de autorización de inquilino para administración.
- Acciones para finalizar titularidad y revocar alquiler.
- Mensajes específicos para suma excedida, duplicados y persona no miembro.
- Componentes y textos en español y catalán.

## 4. Pruebas ejecutadas

| Validación | Resultado |
| --- | --- |
| Backend `mvn test` | 19 tests, 0 fallos |
| `OwnershipIntegrationTest` | 4 tests, 0 fallos |
| Titularidad y alquiler por administración | Correcto |
| Propietario puede ver y no gestionar | Correcto (`403`) |
| Inquilino puede ver su alquiler | Correcto |
| Duplicado de titularidad | Correcto (`409`) |
| Suma superior al 100 % | Correcto (`422`) |
| Persona no miembro | Correcto (`422`) |
| Aislamiento entre comunidades | Correcto (`403`) |
| Histórico tras finalizar | Correcto |
| Frontend `npm test` | 8 tests, 0 fallos |
| Frontend `npm run typecheck` | Correcto |
| Frontend `npm run build` | Correcto |
| Smoke test del stack completo | Invitación, aceptación, titularidad, alquiler, accesos y ruta de unidad correctos |
| OpenAPI con Redocly | Válido; 1 warning esperado por localhost |

## 5. Limitaciones conocidas

- No hay UI de edición de titularidad/alquiler; el backend soporta `PATCH`.
- No se adjuntan documentos justificativos; se incorporarán con el módulo documental.
- La suma del 100 % se valida sobre titulares activos en la fecha actual.
- No hay instantánea de coeficiente en la titularidad; se realizará al generar cargos financieros.
- No hay importación masiva de propietarios.
- No hay paginación en el histórico de una unidad.
- No hay pruebas E2E del detalle de unidad.
- El inquilino puede consultar la titularidad de su unidad; revisar con la comunidad piloto si debe restringirse.

## 6. Trazabilidad

| Historia | Estado |
| --- | --- |
| US-030 Alta de titularidad | Hecho |
| US-031 Copropiedad | Hecho |
| US-032 Autorización de inquilinos | Hecho |
| US-033 Histórico de titularidad y alquiler | Hecho |
| US-034 Visibilidad de datos de propietarios | Parcial: backend y detalle de unidad |

## 7. Próximos pasos

1. Añadir edición de titularidad y alquiler en la UI.
2. Incorporar documentos justificativos en FASE 3.6.
3. Implementar FASE 3.3: finanzas, cuotas, cargos y pagos.
4. Guardar instantánea del coeficiente al generar cargos.
5. Añadir pruebas E2E del detalle de unidad.
6. Validar con la comunidad piloto la visibilidad de titularidad para inquilinos.

---

*Fin de docs/FASE3.2-IMPLEMENTATION.md — v0.1.0.*
