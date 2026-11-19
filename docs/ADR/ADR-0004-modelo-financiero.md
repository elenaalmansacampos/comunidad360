# ADR-0004 — Modelo financiero y precisión decimal

- **Estado**: Aceptado (pendiente de validación funcional en FASE 1)
- **Fecha**: 2026-11-19
- **Decisores**: Database Architect, Backend Lead, Product Owner, asesoría contable

## Contexto

La gestión financiera de una comunidad incluye ingresos, gastos, cuotas, derramas, facturas, pagos,
presupuestos, ejercicios y conciliación bancaria. Los errores de redondeo o la mezcla de conceptos pueden
afectar a la confianza y a la contabilidad.

## Decisión

1. Usar `BigDecimal` en Java y `DECIMAL` en PostgreSQL para todos los importes monetarios.
2. Prohibir `float` y `double` en lógica financiera.
3. Separar explícitamente:
   - movimientos bancarios (`bank_transaction`);
   - apuntes contables (`ledger_entry`);
   - facturas (`invoice`);
   - pagos (`payment`);
   - cargos por cuota/derrama (`assessment_charge`).
4. Guardar la instantánea del coeficiente al generar un cargo.
5. Los apuntes contabilizados son inmutables; las correcciones se realizan con un apunte de reverso.
6. Toda operación financiera genera auditoría.
7. Aplicar idempotencia en operaciones de creación de cargos, pagos y conciliaciones.
8. Documentar y probar la regla de redondeo y distribución de céntimos antes de implementar.

## Alternativas consideradas

### `double`/`float`

- **Ventajas**: simplicidad.
- **Inconvenientes**: errores de precisión inaceptables en dinero.
- **Motivo de descarte**: riesgo financiero.

### Contabilidad por partida doble completa en el MVP

- **Ventajas**: rigor contable.
- **Inconvenientes**: complejidad funcional y de formación para el piloto.
- **Motivo de descarte**: el MVP usará un libro de apuntes controlado con tipos y reversos; la partida
  doble se evaluará si la comunidad o la gestoría lo exigen (Q-16).

### Modelo único de "movimiento"

- **Ventajas**: menos tablas.
- **Inconvenientes**: mezcla banco, contabilidad, factura y pago; dificulta conciliación y auditoría.
- **Motivo de descarte**: el requisito exige separación explícita.

## Consecuencias

### Positivas

- Precisión monetaria.
- Trazabilidad y conciliación claras.
- Base para informes y auditoría.

### Negativas / riesgos

- Más entidades y casos de uso.
- Necesidad de definir categorías, ejercicios y reglas de redondeo con la comunidad piloto.
- Requiere validación con asesoría contable.

## Pendiente de validación

- Plan de categorías y cuentas (Q-07).
- Regla exacta de redondeo y reparto (Q-08).
- Necesidad de partida doble o facturación electrónica (Q-16).
- Plazos de conservación (Q-09).
