# COMUNIDAD 360 — FASE 3.4: Banca, CSV y conciliación

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Implementada; validación funcional pendiente |
| Fecha | 2026-11-19 |
| Alcance | Cuentas, importación CSV, movimientos y conciliación |

---

## 1. Resumen

Se ha implementado el cuarto incremento vertical de FASE 3:

- Alta y consulta de cuentas bancarias con IBAN enmascarado.
- Importación de movimientos desde CSV con mapeo de columnas.
- Detección de duplicados por huella.
- Histórico de importaciones con contadores.
- Listado de movimientos bancarios.
- Conciliación con pagos o apuntes contables.
- Deshacer conciliación.
- Pantalla de banca en el frontend.

## 2. Backend

### 2.1. Migración

`backend/src/main/resources/db/migration/V6__banking.sql` crea:

- `bank_account`
- `bank_import_batch`
- `bank_transaction`
- `reconciliation`

Incluye:

- FKs compuestas por comunidad.
- IBAN enmascarado y últimos cuatro dígitos; no se guarda el IBAN completo.
- Unicidad de movimiento por cuenta y huella.
- Estados de movimiento `UNRECONCILED`, `RECONCILED`, `IGNORED`.
- Conciliación exclusiva con pago o apunte.
- Índices únicos para evitar conciliar dos veces el mismo pago o apunte.
- Contador de duplicados en cada importación.

### 2.2. Endpoints

| Método | Ruta | Permiso |
| --- | --- | --- |
| GET/POST | `/finance/banking/accounts` | lectura `BANK_ACCOUNT_VIEW` / alta `FINANCE_MANAGE` |
| GET | `/finance/banking/imports` | `BANK_IMPORT` |
| POST | `/finance/banking/imports` | `BANK_IMPORT` |
| GET | `/finance/banking/transactions` | `BANK_ACCOUNT_VIEW` |
| POST | `/finance/banking/transactions/{id}/reconcile` | `BANK_RECONCILE` |
| DELETE | `/finance/banking/reconciliations/{id}` | `BANK_RECONCILE` |

La importación es multipart con:

- `file`.
- `accountId`.
- `dateColumn`, `descriptionColumn`, `amountColumn`.
- `balanceColumn`, `externalIdColumn` opcionales.
- `separator` y `dateFormat` opcionales. El separador admite `comma`, `semicolon`, `tab`, `pipe` o un carácter literal.

### 2.3. Reglas de negocio

- La banca es de solo lectura: no se ejecutan transferencias ni pagos.
- No se almacenan credenciales bancarias ni el IBAN completo.
- El CSV se procesa con cabecera y mapeo por nombre de columna.
- Formatos de fecha admitidos: ISO, `d/M/yyyy`, `dd/MM/yyyy`, `yyyy/MM/dd` y `dd-MM-yyyy`.
- Importes con coma o punto decimal y separadores de miles.
- Cada movimiento tiene una huella única por cuenta para evitar duplicados.
- Reimportar un CSV no duplica movimientos; se contabilizan como duplicados.
- La conciliación exige importe exacto al valor absoluto del movimiento.
- Solo se concilia con pagos registrados o apuntes contabilizados.
- Una conciliación puede deshacerse.
- Conciliar un pago cambia su estado a `RECONCILED`; deshacerlo lo devuelve a `REGISTERED`.
- La importación y la conciliación generan auditoría.
- Los límites de tamaño y número de filas son configurables.

## 3. Frontend

- Nueva ruta: `/communities/:communityId/banking`.
- Enlace `Banca` en sidebar y enlace desde Finanzas.
- Listado de cuentas con IBAN enmascarado.
- Formulario de alta de cuenta.
- Formulario de importación CSV con mapeo de columnas.
- Histórico de importaciones con filas, correctas, duplicadas y errores.
- Tabla de movimientos con importes positivos/negativos diferenciados.
- Acción de conciliar con pago o apunte contable.
- Acción de deshacer conciliación.
- Mensajes de error específicos para importe incorrecto y movimiento ya conciliado.
- Textos en español y catalán.

## 4. Pruebas ejecutadas

| Validación | Resultado |
| --- | --- |
| Backend `mvn test` | 29 tests, 0 fallos |
| `BankingIntegrationTest` | 4 tests, 0 fallos |
| Cuenta con IBAN enmascarado | Correcto |
| Importación CSV con mapeo español | Correcto |
| Detección de duplicados al reimportar | Correcto |
| Conciliación con apunte contable | Correcto |
| Deshacer conciliación | Correcto |
| Importe de conciliación incorrecto | Correcto (`422`) |
| Propietario no puede importar | Correcto (`403`) |
| Aislamiento entre comunidades | Correcto (`403`) |
| Frontend `npm test` | 14 tests, 0 fallos |
| Frontend `npm run typecheck` | Correcto |
| Frontend `npm run build` | Correcto |
| Smoke test completo | Importación, conciliación, deshacer y ruta frontend correctos |

## 5. Limitaciones conocidas

- No hay sincronización bancaria automática PSD2; se implementará post-MVP.
- No se almacenan credenciales bancarias.
- No hay pagos ni transferencias.
- No hay sugerencias automáticas de conciliación.
- La conciliación es total, no parcial.
- No hay edición/archivado de cuentas desde la UI.
- No hay previsualización del CSV antes de importar.
- No hay conciliación de saldos de cuenta.
- No hay pruebas E2E.
- La sección bancaria del OpenAPI v0 no se ha sincronizado por completo; `API.md` y este documento son la referencia actual.

## 6. Trazabilidad

| Historia | Estado |
| --- | --- |
| US-060 Cuentas bancarias enmascaradas | Hecho |
| US-061 Importación CSV | Hecho |
| US-062 Detección de duplicados | Hecho |
| US-063 Conciliación | Hecho |

## 7. Próximos pasos

1. Implementar FASE 3.5: incidencias.
2. Valorar conciliación parcial y sugerencias automáticas.
3. Añadir previsualización y validación previa del CSV.
4. Implementar PSD2 con proveedor autorizado post-MVP.
5. Añadir pruebas E2E del flujo de conciliación.

---

*Fin de docs/FASE3.4-IMPLEMENTATION.md — v0.1.0.*
