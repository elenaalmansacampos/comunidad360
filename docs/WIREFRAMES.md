# COMUNIDAD 360 — Navegación y wireframes

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Propuesta para validación (FASE 1) |
| Fecha | 2026-11-19 |
| Enfoque | Mobile-first, accesible y bilingüe |

---

## 1. Principios

- Diseñar primero para 360 px.
- Una acción principal por pantalla.
- Navegación inferior en móvil y sidebar en escritorio.
- Los datos financieros individuales solo se muestran a su titular y a roles autorizados.
- Los estados vacíos explican qué hacer.
- Todos los textos pasan por claves i18n.
- Los wireframes son de baja fidelidad; no definen el aspecto visual final.

## 2. Mapa de navegación

### 2.1. Móvil

```text
┌─────────────────────────────────────────┐
│ Comunidad [Comunidad Example v]  🔔  ES │
├─────────────────────────────────────────┤
│                                         │
│              CONTENIDO                  │
│                                         │
├─────────────────────────────────────────┤
│ Inicio  Finanzas  Incid.  Docs.  Más    │
└─────────────────────────────────────────┘
```

- Máximo 5 elementos.
- "Más" abre un menú con el resto de secciones permitidas.
- La campana abre notificaciones.
- El selector de idioma está siempre accesible.

### 2.2. Escritorio

```text
┌──────────────┬──────────────────────────────────────────────┐
│ COMUNIDAD 360│ Comunidad [Example v]       🔔  ES  [Avatar] │
│              ├──────────────────────────────────────────────┤
│ Inicio       │                                              │
│ Comunidad    │                 CONTENIDO                    │
│ Finanzas     │                                              │
│ Incidencias  │                                              │
│ Documentos   │                                              │
│ Reuniones    │                                              │
│ Votaciones*  │                                              │
│ Chat*        │                                              │
│ Proveedores* │                                              │
│ Ajustes      │                                              │
└──────────────┴──────────────────────────────────────────────┘
* MVP+ / condicionado
```

### 2.3. Rutas propuestas

```text
/login
/invitation/:token
/select-community
/communities/:communityId
  /dashboard
  /structure
  /structure/buildings/:buildingId
  /structure/units/:unitId
  /members
  /finance
  /finance/assessments
  /finance/payments
  /finance/invoices
  /finance/banking
  /incidents
  /incidents/new
  /incidents/:incidentId
  /documents
  /documents/:documentId
  /meetings
  /meetings/:meetingId
  /votings*
  /chat*
  /providers*
  /settings
  /notifications
```

## 3. Inventario de pantallas

| ID | Pantalla | Roles principales | Prioridad |
| --- | --- | --- | --- |
| W-01 | Login | Público | P0 |
| W-02 | Aceptar invitación | Público + token | P0 |
| W-03 | Recuperar contraseña | Público | P0 |
| W-04 | Seleccionar comunidad | Todos | P0 |
| W-05 | Dashboard | Todos | P0 |
| W-06 | Estructura de comunidad | Admin, president, secretary | P0 |
| W-07 | Detalle de unidad | Admin, owner, tenant | P0 |
| W-08 | Directorio de miembros | Admin, órgano | P0 |
| W-09 | Resumen financiero | Admin, treasurer, president, owner | P0 |
| W-10 | Cuotas y cargos | Admin, treasurer, owner | P0 |
| W-11 | Crear cuota/derrama | Admin, treasurer | P0 |
| W-12 | Registrar pago | Admin, treasurer | P0 |
| W-13 | Facturas | Admin, treasurer, provider | P0 |
| W-14 | Banca y conciliación | Admin, treasurer | P0 |
| W-15 | Listado de incidencias | Todos | P0 |
| W-16 | Crear incidencia | Owner, tenant, admin | P0 |
| W-17 | Detalle de incidencia | Según permiso | P0 |
| W-18 | Documentos | Todos | P0 |
| W-19 | Detalle de documento | Según permiso | P0 |
| W-20 | Reuniones | Todos | P0 |
| W-21 | Detalle de reunión | Todos / órgano | P0 |
| W-22 | Notificaciones | Todos | P0 |
| W-23 | Preferencias y perfil | Todos | P0 |
| W-24 | Votaciones | Todos / órgano | P1 |
| W-25 | Chat | Todos | P1 |
| W-26 | Proveedores | Admin, treasurer, provider | P1 |

## 4. Wireframes de flujos críticos

### W-01 — Login móvil

```text
┌─────────────────────────────┐
│                             │
│        COMUNIDAD 360        │
│   Gestiona tu comunidad     │
│                             │
│  Email                      │
│  ┌───────────────────────┐  │
│  │                       │  │
│  └───────────────────────┘  │
│  Contraseña                 │
│  ┌───────────────────────┐  │
│  │                   👁  │  │
│  └───────────────────────┘  │
│                             │
│  ┌───────────────────────┐  │
│  │   Iniciar sesión      │  │
│  └───────────────────────┘  │
│                             │
│  ¿Has olvidado la           │
│  contraseña?                │
│                             │
│  Idioma: [ES v]             │
└─────────────────────────────┘
```

Notas:

- Error genérico ante credenciales incorrectas.
- No revelar si el email existe.
- Enlace de recuperación visible.
- Estado de carga en el botón.

### W-02 — Aceptar invitación

```text
┌─────────────────────────────┐
│  Invitación                 │
│  Comunidad Example          │
│  Rol: Propietario           │
│                             │
│  Nombre                     │
│  ┌───────────────────────┐  │
│  └───────────────────────┘  │
│  Email                      │
│  ┌───────────────────────┐  │
│  │ bloqueado             │  │
│  └───────────────────────┘  │
│  Contraseña                 │
│  ┌───────────────────────┐  │
│  └───────────────────────┘  │
│  Repetir contraseña         │
│  ┌───────────────────────┐  │
│  └───────────────────────┘  │
│                             │
│  [ Crear cuenta ]           │
│                             │
│  Invitación válida hasta    │
│  19/12/2026                 │
└─────────────────────────────┘
```

Casos:

- Token inválido o caducado: pantalla de error con acción de solicitar nueva invitación.
- Usuario existente: opción de vincular la nueva membresía a su cuenta.

### W-04 — Seleccionar comunidad

```text
┌─────────────────────────────┐
│  Tus comunidades            │
│                             │
│  ┌───────────────────────┐  │
│  │ Comunidad Example     │  │
│  │ Administrador         │  │
│  │ 12 edificios · 84 u.  │  │
│  └───────────────────────┘  │
│  ┌───────────────────────┐  │
│  │ Comunidad Portal      │  │
│  │ Propietario           │  │
│  └───────────────────────┘  │
│                             │
│  [ Cerrar sesión ]          │
└─────────────────────────────┘
```

### W-05 — Dashboard

#### Móvil, propietario

```text
┌─────────────────────────────┐
│ Example v          🔔   ES  │
├─────────────────────────────┤
│ Hola, Anna                  │
│                             │
│ ┌─────────────────────────┐ │
│ │ Mis cuotas              │ │
│ │ Pendiente: 120,00 €     │ │
│ │ [Ver detalle]           │ │
│ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │
│ │ Avisos                  │ │
│ │ Junta ordinaria 12/12   │ │
│ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │
│ │ Mis incidencias         │ │
│ │ 1 abierta               │ │
│ └─────────────────────────┘ │
│                             │
│ Inicio Finanzas Incid. Docs │
└─────────────────────────────┘
```

#### Escritorio, administrador

```text
┌──────────────┬──────────────────────────────────────────────┐
│ COMUNIDAD 360│ Example v                   🔔  ES  [A]     │
│              ├──────────────────────────────────────────────┤
│ Inicio       │ Resumen de la comunidad                      │
│ Comunidad    │ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ │
│ Finanzas     │ │Saldo   │ │Ingresos│ │Gastos  │ │Pendiente│ │
│ Incidencias  │ │12.340 €│ │ 8.200 €│ │ 5.100 €│ │  2.400 €│ │
│ Documentos   │ └────────┘ └────────┘ └────────┘ └────────┘ │
│ Reuniones    │                                              │
│ Ajustes      │ ┌───────────────────────┐ ┌───────────────┐ │
│              │ │ Evolución mensual     │ │ Incidencias   │ │
│              │ │ [gráfico]             │ │ [lista]       │ │
│              │ └───────────────────────┘ └───────────────┘ │
│              │ ┌──────────────────────────────────────────┐ │
│              │ │ Próximas reuniones                       │ │
│              │ └──────────────────────────────────────────┘ │
└──────────────┴──────────────────────────────────────────────┘
```

Widgets por rol según `DESIGN-SYSTEM.md`.

### W-06 — Estructura de comunidad

```text
┌──────────────────────────────────────────────┐
│ Comunidad › Estructura        [+ Unidad]     │
├──────────────────────────────────────────────┤
│ [Edificios v]  [Buscar unidad...]            │
│                                              │
│ Edificio A                                   │
│ ┌──────────────────────────────────────────┐ │
│ │ 1º 1ª  Vivienda   Ana P.   6,250000 %   │ │
│ │ 1º 2ª  Vivienda   Marc G.  6,250000 %   │ │
│ │ Bajo  Local       —        3,100000 %   │ │
│ └──────────────────────────────────────────┘ │
│                                              │
│ Edificio B                                   │
│ ┌──────────────────────────────────────────┐ │
│ │ ...                                      │ │
│ └──────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

Acciones:

- Crear edificio.
- Crear unidad.
- Importar unidades (MVP+).
- Validar coeficientes.
- Archivar con confirmación.

### W-07 — Detalle de unidad

```text
┌──────────────────────────────────────────────┐
│ Unidad 1º 1ª · Edificio A                    │
│ [Datos] [Titularidad] [Inquilinos] [Historial]│
├──────────────────────────────────────────────┤
│ Tipo: Vivienda      Superficie: 78 m²        │
│ Coeficiente: 6,250000 %                      │
│ Estado: Activa                               │
│                                              │
│ Titularidad                                  │
│ ┌──────────────────────────────────────────┐ │
│ │ Ana P.    Propietaria    100 %           │ │
│ └──────────────────────────────────────────┘ │
│                                              │
│ Inquilino actual: Marc G. (hasta 31/12/2026) │
└──────────────────────────────────────────────┘
```

Reglas:

- El propietario ve solo sus datos.
- Los porcentajes no pueden superar 100 %.
- El histórico no se sobrescribe.

### W-09 — Resumen financiero

```text
┌──────────────────────────────────────────────┐
│ Finanzas · Ejercicio 2026                    │
│ [Resumen] [Cuotas] [Movimientos] [Facturas]  │
├──────────────────────────────────────────────┤
│ ┌────────────┐ ┌────────────┐ ┌────────────┐ │
│ │ Saldo      │ │ Pendiente  │ │ Gastos     │ │
│ │ 12.340,00 €│ │  2.400,00 €│ │  5.100,00 €│ │
│ └────────────┘ └────────────┘ └────────────┘ │
│                                              │
│ Evolución mensual                            │
│ ┌──────────────────────────────────────────┐ │
│ │ [gráfico de barras]                      │ │
│ └──────────────────────────────────────────┘ │
│                                              │
│ Últimos movimientos                          │
│ ┌──────────────────────────────────────────┐ │
│ │ Fecha  Concepto        Importe   Estado  │ │
│ │ 12/11  Limpieza        -450,00   Pagado  │ │
│ │ 10/11  Cuota Anna      +120,00   Cobrado │ │
│ └──────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

### W-11 — Crear cuota o derrama

```text
┌──────────────────────────────────────────────┐
│ Nueva cuota                         Paso 1/4 │
│ ●─────○─────○─────○                          │
├──────────────────────────────────────────────┤
│ Tipo: (•) Ordinaria  ( ) Derrama             │
│ Concepto: [Gastos generales 2026.........]    │
│ Periodo:  [01/01/2026] a [31/12/2026]        │
│ Vencimiento: [30/06/2026]                    │
│ Importe total: [ 8.400,00 € ]                │
│                                              │
│ Reparto: (•) Por coeficiente                 │
│          ( ) A partes iguales                │
│          ( ) Manual                          │
│                                              │
│                         [Cancelar] [Siguir]  │
└──────────────────────────────────────────────┘
```

Pasos:

1. Datos de la cuota.
2. Previsualización del reparto por unidad.
3. Revisión de suma y redondeo.
4. Emisión con confirmación y auditoría.

### W-12 — Registrar pago

```text
┌──────────────────────────────────────────────┐
│ Registrar pago                               │
├──────────────────────────────────────────────┤
│ Unidad: [1º 1ª · Ana P. v]                   │
│ Cargo:  [Cuota 2026 · 120,00 € v]            │
│ Importe: [120,00] €                          │
│ Fecha: [19/11/2026]                          │
│ Método: [Transferencia v]                    │
│ Referencia: [............................]   │
│ Documento: [Adjuntar justificante]           │
│                                              │
│ Saldo del cargo tras el pago: 0,00 €         │
│                         [Cancelar] [Guardar] │
└──────────────────────────────────────────────┘
```

Validaciones:

- Importe mayor que cero.
- No superar el cargo sin confirmación explícita.
- Idempotencia en el envío.

### W-15 — Listado de incidencias

```text
┌──────────────────────────────────────────────┐
│ Incidencias                    [+ Nueva]     │
│ [Todas v] [Prioridad v] [Buscar...]          │
├──────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────┐ │
│ │ ● Abierta   Fuga en garaje       Alta    │ │
│ │ Garaje · 12/11/2026 · Asignada a Juan    │ │
│ ├──────────────────────────────────────────┤ │
│ │ ● En curso  Luz escalera         Media   │ │
│ │ Portal · 10/11/2026 · Proveedor Electra  │ │
│ └──────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

En móvil, cada incidencia se muestra como tarjeta. Los filtros se abren en un panel.

### W-16 — Crear incidencia

```text
┌──────────────────────────────────────────────┐
│ Nueva incidencia                             │
├──────────────────────────────────────────────┤
│ Título: [Fuga en garaje...................]  │
│ Categoría: [Fontanería v]                    │
│ Ubicación: [Garaje · Plaza 12 v]             │
│ Prioridad: [Alta v]                          │
│ Descripción:                                 │
│ ┌──────────────────────────────────────────┐ │
│ │ Pérdida de agua junto a la plaza 12...   │ │
│ └──────────────────────────────────────────┘ │
│ Fotos: [📷 Añadir]                           │
│                                              │
│                         [Cancelar] [Crear]   │
└──────────────────────────────────────────────┘
```

### W-17 — Detalle de incidencia

```text
┌──────────────────────────────────────────────┐
│ Fuga en garaje                 [Editar]      │
│ Abierta · Alta · Fontanería                  │
├──────────────────────────────────────────────┤
│ Descripción                                  │
│ Pérdida de agua junto a la plaza 12.         │
│                                              │
│ Fotos                                        │
│ [imagen 1] [imagen 2]                        │
│                                              │
│ Seguimiento                                  │
│ ● 12/11 Ana creó la incidencia               │
│ ● 12/11 Juan la asignó a Fontanería Pérez    │
│ ● 13/11 Proveedor: visita prevista 14/11     │
│                                              │
│ [Escribir comentario......................]  │
│                         [Comentar]           │
└──────────────────────────────────────────────┘
```

Los comentarios internos se marcan visualmente y solo los ve el equipo de gestión.

### W-18 — Documentos

```text
┌──────────────────────────────────────────────┐
│ Documentos                     [+ Subir]     │
│ [Categoría v] [Buscar...]                    │
├──────────────────────────────────────────────┤
│ 📄 Acta junta ordinaria 2026                 │
│    Acta · Visible propietarios · 12/11/2026  │
│ 📄 Estatutos de la comunidad                 │
│    Estatuto · Visible comunidad · v2         │
│ 📄 Factura limpieza noviembre                │
│    Factura · Solo administración · 10/11     │
└──────────────────────────────────────────────┘
```

La visibilidad se muestra con texto, no solo con color. La descarga abre una URL firmada.

### W-20/W-21 — Reuniones

```text
┌──────────────────────────────────────────────┐
│ Reuniones                     [+ Convocar]   │
├──────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────┐ │
│ │ Junta ordinaria                          │ │
│ │ 12/12/2026 · 19:00 · Salón comunitario   │ │
│ │ Estado: Convocada                        │ │
│ └──────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

Detalle:

```text
┌──────────────────────────────────────────────┐
│ Junta ordinaria 12/12/2026       [Editar]    │
│ [Orden del día] [Asistencia] [Acuerdos] [Acta]│
├──────────────────────────────────────────────┤
│ 1. Aprobación del acta anterior              │
│ 2. Presupuesto 2027                          │
│ 3. Derrama de rehabilitación                 │
│                                              │
│ Asistencia: 42/84 unidades · 51,2 %          │
│ Representadas: 8 unidades · 9,4 %            │
└──────────────────────────────────────────────┘
```

### W-24 — Votación consultiva (MVP+)

```text
┌──────────────────────────────────────────────┐
│ Consulta: Derrama de rehabilitación          │
│ Estado: Abierta · Cierra 19/12/2026 20:00    │
├──────────────────────────────────────────────┤
│ Propuesta                                    │
│ Aprobar una derrama de 24.000 € repartida... │
│                                              │
│ ( ) A favor                                  │
│ ( ) En contra                                │
│ ( ) Abstención                               │
│                                              │
│ [ Enviar voto ]                              │
│                                              │
│ ⚠ Votación consultiva. No constituye por sí  │
│   sola un acuerdo jurídicamente válido.      │
└──────────────────────────────────────────────┘
```

El aviso legal es siempre visible, no ocultable y traducible.

### W-25 — Chat (MVP+)

```text
┌─────────────────────────────┐
│ Chat                        │
│ [General] [Temas] [Privados]│
├─────────────────────────────┤
│ Ana: ¿Se ha revisado la     │
│      fuga del garaje?       │
│ Juan: Sí, el proveedor      │
│       viene mañana.         │
│                             │
│ [Escribir mensaje......] 📎 │
└─────────────────────────────┘
```

### W-22/W-23 — Notificaciones, perfil e idioma

```text
┌──────────────────────────────┐
│ Notificaciones               │
├──────────────────────────────┤
│ ● Nueva cuota emitida        │
│   12/11/2026                 │
│ ○ Acta de la junta publicada │
│   10/11/2026                 │
├──────────────────────────────┤
│ Preferencias                 │
│ Email:        [x]            │
│ Avisos:       [x]            │
│ Incidencias:  [x]            │
│                              │
│ Idioma: [Català v]           │
└──────────────────────────────┘
```

## 5. Estados de interfaz

Cada pantalla debe contemplar:

| Estado | Comportamiento |
| --- | --- |
| Carga | Esqueleto o spinner sin saltos de layout |
| Vacío | Explicación y acción principal |
| Error | Mensaje claro, `traceId` y reintento |
| Sin permiso | Acceso denegado sin filtrar datos |
| Sin conexión | Aviso y opción de reintentar |
| Guardando | Botón deshabilitado y feedback |
| Conflicto | Aviso de versión desactualizada y recarga |

## 6. Responsive

- 360–767 px: una columna, navegación inferior, tarjetas.
- 768–1023 px: dos columnas cuando aporte valor, sidebar colapsable.
- 1024 px+: sidebar fijo, tablas y paneles.
- Las tablas se convierten en listas de tarjetas en móvil.
- Los formularios ocupan el ancho disponible con máximo legible.
- Los importes se alinean a la derecha en tablas.

## 7. Accesibilidad

- Etiquetas visibles y asociadas.
- Foco visible y orden lógico.
- Navegación completa por teclado.
- `aria-live` para errores y confirmaciones.
- Contraste mínimo 4,5:1.
- Áreas táctiles de 44×44 px.
- No usar solo color para estados.
- Modales con trampa de foco y cierre con Escape.
- Gráficos con tabla o resumen textual alternativo.
- Idioma del documento actualizado.

## 8. i18n en wireframes

Ejemplos de claves:

```text
nav.home
nav.finance
nav.incidents
nav.documents
nav.more
auth.login.title
auth.login.submit
finance.summary.balance
finance.charges.pending
incidents.status.open
documents.visibility.owners
meetings.attendance.summary
votings.legalNotice
common.language.selector
common.actions.cancel
common.actions.save
common.errors.required
```

Formatos:

- Fecha: `Intl.DateTimeFormat("es-ES" | "ca-ES")`.
- Importe: `Intl.NumberFormat(..., { style: "currency", currency: "EUR" })`.
- Porcentaje: `Intl.NumberFormat(..., { style: "percent" })`.

## 9. Pendiente de validación

- Aprobación de wireframes por Product Owner y comunidad piloto.
- Etiquetas definitivas de navegación en catalán.
- Widgets exactos del dashboard por rol.
- Diseño de la carga inicial de unidades.
- Diseño de la conciliación bancaria.
- Diseño de reuniones y votaciones con asesoría legal.
- Validación de contraste y tipografía.
- Pruebas de usabilidad con personas de baja alfabetización digital.

---

*Fin de docs/WIREFRAMES.md — v0.1.0 (FASE 1).*
