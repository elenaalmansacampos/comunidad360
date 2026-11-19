# COMUNIDAD 360 — FASE 3.3: Finanzas núcleo

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Implementada; validación funcional pendiente |
| Fecha | 2026-11-19 |
| Alcance | Ejercicios, presupuesto, cuotas, cargos, facturas, pagos, apuntes y resumen |

---

## 1. Resumen

Se ha implementado el tercer incremento vertical de FASE 3:

- Ejercicios fiscales con apertura y cierre.
- Presupuesto con partidas de ingreso y gasto.
- Cuotas ordinarias y derramas extraordinarias.
- Reparto por coeficiente o a partes iguales.
- Cargos por unidad con instantánea de coeficiente.
- Facturas de proveedor.
- Pagos vinculados a cargos o facturas.
- Apuntes contables con contabilización y reverso.
- Resumen financiero por comunidad o por propietario.
- Idempotencia en operaciones financieras.
- Pantalla de finanzas con pestañas y formularios.

## 2. Backend

### 2.1. Migración

`backend/src/main/resources/db/migration/V5__finance.sql` crea:

- `fiscal_year`
- `budget`, `budget_item`
- `assessment`, `assessment_charge`
- `invoice`
- `payment`
- `ledger_entry`
- `idempotency_record`

Incluye FKs compuestas por comunidad, restricciones de estado, importes `NUMERIC(19,4)`, coeficientes
`NUMERIC(9,6)` y unicidad de número de apunte por ejercicio.

### 2.2. Endpoints

| Método | Ruta | Permiso |
| --- | --- | --- |
| GET/POST | `/finance/fiscal-years` | lectura staff / `FINANCE_MANAGE` |
| POST | `/finance/fiscal-years/{id}/close` | `FINANCE_MANAGE` |
| GET/POST | `/finance/budgets` | lectura staff / `FINANCE_MANAGE` |
| POST | `/finance/budgets/{id}/items` | `FINANCE_MANAGE` |
| POST | `/finance/budgets/{id}/approve` | `FINANCE_MANAGE` |
| GET/POST | `/finance/assessments` | lectura staff / `FINANCE_MANAGE` |
| POST | `/finance/assessments/{id}/issue` | `FINANCE_MANAGE` |
| GET | `/finance/charges` | staff o propias para propietario |
| GET/POST | `/finance/invoices` | lectura staff / `FINANCE_MANAGE` |
| GET/POST | `/finance/payments` | lectura staff / `FINANCE_MANAGE` |
| GET/POST | `/finance/ledger-entries` | lectura staff / `FINANCE_MANAGE` |
| POST | `/finance/ledger-entries/{id}/post` | `FINANCE_MANAGE` |
| POST | `/finance/ledger-entries/{id}/reverse` | `FINANCE_MANAGE` |
| GET | `/finance/summary` | staff o resumen propio |
| GET | `/finance/reports/categories` | staff |

`POST` de cuotas, facturas, pagos y apuntes acepta `Idempotency-Key`.

### 2.3. Reglas de negocio

- Los importes usan `BigDecimal` y `NUMERIC(19,4)`.
- El reparto por coeficiente exige suma de coeficientes 100 % con tolerancia configurable.
- El reparto utiliza el método del resto mayor para distribuir céntimos.
- El reparto a partes iguales también distribuye los céntimos restantes de forma determinista.
- El reparto manual (`CUSTOM`) queda pendiente de una interfaz específica.
- Los cargos guardan la instantánea del coeficiente y dos decimales de importe.
- No se emite una cuota ya emitida.
- No se puede cerrar un ejercicio con apuntes en borrador.
- Un ejercicio cerrado rechaza nuevas cuotas, facturas, pagos y apuntes.
- Los pagos actualizan el cargo o la factura y no permiten superar el importe pendiente.
- Los apuntes contabilizados no se editan; se corrigen mediante reverso.
- La contabilidad y los pagos/cargos están separados: el saldo se calcula con apuntes contabilizados.
- Un propietario ve únicamente sus cargos y su resumen; el resto de finanzas requiere perfil staff.
- Toda operación financiera genera auditoría.
- Las claves de idempotencia se asocian a comunidad, usuario y operación.

### 2.4. Reparto de céntimos

El reparto se realiza en céntimos:

1. Se redondea el total a dos decimales.
2. Se calcula la parte proporcional o igual.
3. Se asigna la parte entera en céntimos.
4. Los céntimos restantes se asignan por resto mayor y, en empate, por código de unidad.
5. La suma de cargos coincide exactamente con el total de la cuota.

## 3. Frontend

- Nueva ruta: `/communities/:communityId/finance`.
- Enlace `Finanzas` en sidebar y navegación móvil.
- Selector de ejercicio fiscal y cierre para administración/tesorería.
- Tarjetas de resumen: saldo, ingresos, gastos, cuotas pendientes y facturas pendientes.
- Propietarios: ven solo sus cuotas pendientes y sus cargos.
- Pestañas: cuotas, cargos, pagos, facturas, apuntes y presupuesto.
- Formularios de alta de ejercicio, cuota/derrama, pago, factura, apunte y presupuesto.
- Acciones para emitir cuotas, contabilizar/reversar apuntes y aprobar presupuestos.
- Formularios y textos en español y catalán.
- Invalidación automática de consultas tras cada operación.

## 4. Pruebas ejecutadas

| Validación | Resultado |
| --- | --- |
| Backend `mvn test` | 25 tests, 0 fallos |
| `FinanceIntegrationTest` | 6 tests, 0 fallos |
| Reparto por coeficiente y suma de cargos | Correcto |
| Reparto a partes iguales y céntimos | Correcto |
| Pagos parciales, estado de cargo e idempotencia | Correcto |
| Facturas, pagos y resumen | Correcto |
| Apuntes y presupuesto | Correcto |
| Propietario solo ve sus cargos | Correcto |
| Cierre de ejercicio y bloqueo de escrituras | Correcto |
| Aislamiento entre comunidades | Correcto |
| Frontend `npm test` | 11 tests, 0 fallos |
| Frontend `npm run typecheck` | Correcto |
| Frontend `npm run build` | Correcto |
| Smoke test completo | Cuota emitida, pago, apunte, factura, resumen y ruta frontend correctos |
| OpenAPI v0 | Válido; la sección financiera se sincronizará en una revisión posterior |

## 5. Limitaciones conocidas

- No hay conciliación bancaria ni importación CSV; corresponde a FASE 3.4.
- No hay contabilidad por partida doble; el modelo actual es un libro de apuntes controlado.
- Los pagos no generan apuntes contables automáticamente; ambos registros están separados.
- El reparto manual no tiene UI ni formato de líneas personalizadas.
- Las facturas usan nombre de proveedor libre; el módulo de proveedores llega en FASE 3.9.
- No hay adjuntos documentales en facturas o pagos.
- No hay paginación en listados financieros.
- No hay UI del informe por categorías; el endpoint existe.
- No hay pruebas E2E.
- La sección financiera del OpenAPI v0 no refleja todavía todos los endpoints implementados; `API.md` y este documento son la referencia actual.
- No hay cierre de cuentas, remesas ni domiciliación.

## 6. Trazabilidad

| Historia | Estado |
| --- | --- |
| US-040 Ejercicios fiscales | Hecho |
| US-041 Presupuesto y partidas | Hecho |
| US-042 Ingresos y gastos | Hecho |
| US-043 Cuotas ordinarias | Hecho |
| US-044 Derramas extraordinarias | Hecho |
| US-045 Cargos por unidad | Hecho |
| US-046 Facturas de proveedor | Hecho, sin proveedor vinculado |
| US-047 Pagos | Hecho |
| US-048 Saldo y cuotas pendientes | Hecho |
| US-049 Informes básicos | Parcial: endpoint por categorías |
| US-050 Auditoría financiera | Hecho |
| US-051 Redondeo e idempotencia | Hecho |

## 7. Próximos pasos

1. Implementar FASE 3.4: importación bancaria CSV y conciliación.
2. Añadir UI del informe por categorías.
3. Añadir edición/anulación controlada de borradores.
4. Añadir adjuntos de facturas y justificantes con el módulo documental.
5. Validar el plan de categorías y la regla de redondeo con la comunidad piloto.
6. Añadir pruebas E2E de finanzas.

---

*Fin de docs/FASE3.3-IMPLEMENTATION.md — v0.1.0.*
