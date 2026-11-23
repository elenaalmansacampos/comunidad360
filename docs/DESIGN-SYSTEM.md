# COMUNIDAD 360 — Design System

| Campo | Valor |
| --- | --- |
| Versión | 0.1.0 |
| Estado | Propuesta inicial (FASE 0) |
| Fecha | 2026-11-19 |
| Principio | Mobile-first, accesible, claro y profesional |

---

## 1. Principios de diseño

1. **Claridad antes que densidad**: una acción principal por pantalla.
2. **Mobile-first**: diseñar para 360 px y ampliar.
3. **Confianza**: tono institucional, sin adornos innecesarios.
4. **Accesibilidad**: WCAG 2.1 AA, teclado y lectores de pantalla.
5. **Lenguaje sencillo**: evitar jerga contable cuando no sea necesaria.
6. **Consistencia**: mismos patrones para acciones, errores y estados.
7. **Bilingüe**: todo texto pasa por claves; nada incrustado.
8. **Privacidad visual**: no mostrar datos financieros de otras unidades.

## 2. Identidad visual

### 2.1. Paleta

| Token | Hex | Uso |
| --- | --- | --- |
| `color-navy-900` | `#0A2342` | Marca, cabeceras, botón primario |
| `color-navy-800` | `#103358` | Sidebar, énfasis |
| `color-blue-600` | `#1B5E9E` | Enlaces, acciones secundarias |
| `color-blue-100` | `#DCEAF7` | Fondos de selección |
| `color-turquoise-600` | `#0F766E` | Acentos, indicadores positivos |
| `color-turquoise-500` | `#14B8A6` | Elementos decorativos, nunca texto pequeño |
| `color-white` | `#FFFFFF` | Superficies |
| `color-gray-50` | `#F6F8FA` | Fondo general |
| `color-gray-100` | `#E8EDF2` | Separadores suaves |
| `color-gray-300` | `#CBD5E1` | Bordes |
| `color-gray-500` | `#64748B` | Texto secundario |
| `color-gray-700` | `#334155` | Texto principal secundario |
| `color-gray-900` | `#0F172A` | Texto principal |
| `color-success` | `#2E7D32` | Éxito |
| `color-warning` | `#B45309` | Advertencia |
| `color-danger` | `#B91C1C` | Error, destrucción |
| `color-info` | `#1D4ED8` | Información |

Reglas:

- Texto normal sobre blanco: `gray-900` o `navy-900`.
- Texto sobre `navy-900`: blanco.
- No usar `turquoise-500` para texto pequeño sobre blanco.
- No comunicar estado solo con color; añadir icono o texto.
- Validar contrastes con herramienta automatizada en FASE 1.

### 2.2. Tipografía

- Fuente principal: **Inter** autoalojada (o alternativa sans-serif del sistema).
- Fallback: `system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`.
- No cargar fuentes desde CDNs de terceros sin evaluación de privacidad.

| Token | Tamaño | Line-height | Uso |
| --- | --- | --- | --- |
| `text-xs` | 12 px | 16 px | Etiquetas, metadatos |
| `text-sm` | 14 px | 20 px | Texto secundario, tablas |
| `text-base` | 16 px | 24 px | Texto principal, formularios |
| `text-lg` | 18 px | 28 px | Subtítulos |
| `text-xl` | 20 px | 28 px | Títulos de tarjeta |
| `text-2xl` | 24 px | 32 px | Títulos de sección |
| `text-3xl` | 30 px | 36 px | Títulos de página |
| `text-4xl` | 36 px | 44 px | Cifras destacadas |

Pesos: 400, 500, 600, 700. Evitar más de tres pesos por pantalla.

### 2.3. Espaciado

Base 4 px:

```text
space-1 = 4px
space-2 = 8px
space-3 = 12px
space-4 = 16px
space-5 = 24px
space-6 = 32px
space-7 = 48px
space-8 = 64px
```

### 2.4. Radios y sombras

| Token | Valor | Uso |
| --- | --- | --- |
| `radius-sm` | 4 px | Inputs, badges |
| `radius-md` | 8 px | Botones, tarjetas |
| `radius-lg` | 12 px | Modales, paneles |
| `radius-full` | 9999 px | Avatares, píldoras |
| `shadow-sm` | `0 1px 2px rgba(15,23,42,.08)` | Tarjetas |
| `shadow-md` | `0 4px 12px rgba(15,23,42,.10)` | Modales, menús |

### 2.5. Breakpoints

| Token | Valor | Uso |
| --- | --- | --- |
| `bp-sm` | 480 px | Móvil grande |
| `bp-md` | 768 px | Tablet |
| `bp-lg` | 1024 px | Escritorio |
| `bp-xl` | 1280 px | Escritorio amplio |

Diseñar primero a 360 px.

### 2.6. Tema claro y oscuro

- Tema por defecto: preferencia del sistema (`prefers-color-scheme`).
- El usuario puede alternarlo con el botón de tema; se persiste en `localStorage`
  (`comunidad360.theme`).
- Se aplica con `data-theme="light|dark"` en `<html>`.
- Los tokens oscuros y los ajustes de superficies viven en `frontend/src/styles/refresh.scss`.

## 3. Layout y navegación

### 3.1. Estructura

- **AuthLayout**: login, invitación, recuperación. Centrado, una columna, máximo 420 px.
- **AppLayout móvil**: barra superior + contenido + navegación inferior.
- **AppLayout escritorio**: sidebar de 240 px + barra superior + contenido.

Navegación inferior móvil (máximo 5 elementos):

1. Inicio.
2. Finanzas.
3. Incidencias.
4. Documentos.
5. Más.

En escritorio, el sidebar muestra todas las secciones permitidas. Los elementos no permitidos no se
renderizan, pero la seguridad real está en backend.

### 3.2. Cabecera

- Nombre de la comunidad activa y selector de comunidad.
- Selector de idioma.
- Centro de notificaciones.
- Menú de usuario.

### 3.3. Dashboard por rol

| Rol | Contenido principal |
| --- | --- |
| `COMMUNITY_ADMIN` | Saldo, cuotas pendientes, incidencias abiertas, próximas reuniones, accesos rápidos |
| `PRESIDENT` | Resumen financiero, incidencias, reuniones, documentos del órgano |
| `SECRETARY` | Reuniones, actas, convocatorias, documentos |
| `TREASURER` | Saldo, ingresos/gastos, cuotas, conciliación, facturas |
| `OWNER` | Mis cuotas, mis incidencias, avisos, documentos visibles |
| `TENANT` | Avisos, incidencias, documentos permitidos, chat |
| `PROVIDER` | Incidencias asignadas, facturas y documentos asignados |

## 4. Componentes

### 4.1. Inventario inicial

- **Acciones**: `Button`, `IconButton`, `LinkButton`, `ButtonGroup`.
- **Formularios**: `Input`, `Textarea`, `Select`, `Combobox`, `Checkbox`, `RadioGroup`, `Switch`,
  `DateInput`, `MoneyInput`, `FileUpload`, `SearchInput`.
- **Feedback**: `Alert`, `Toast`, `InlineError`, `Spinner`, `Skeleton`, `EmptyState`.
- **Superficies**: `Card`, `Panel`, `Modal`, `Drawer`, `Popover`, `Tooltip`.
- **Navegación**: `Tabs`, `Breadcrumb`, `Pagination`, `Sidebar`, `BottomNav`, `LanguageSwitcher`.
- **Datos**: `Table`, `DataList`, `StatCard`, `Badge`, `Avatar`, `DescriptionList`, `Timeline`.
- **Dominio**: `CommunitySelector`, `UnitSelector`, `MoneyDisplay`, `AssessmentStatus`,
  `IncidentStatus`, `DocumentCard`, `ChatBubble`, `MeetingCard`, `VoteCard`.
- **Gráficos**: `BarChart`, `LineChart`, `DonutChart` con alternativa tabular.

### 4.2. Reglas de componentes

- Un botón primario por vista.
- Acciones destructivas en `color-danger` y con confirmación.
- Los estados de carga no deben provocar saltos de layout.
- Los errores se muestran junto al campo y en resumen si hay varios.
- Los formularios conservan los datos introducidos ante error.
- Los componentes no contienen textos literales; reciben claves o `children` traducidos.
- Los componentes de dominio no acceden directamente a la API.

### 4.3. Formularios

- Etiqueta visible arriba del campo.
- Marcado de obligatorios textual, no solo con asterisco.
- Texto de ayuda antes del error.
- Error específico y acción recomendada.
- `MoneyInput` trabaja internamente con string decimal, no `number`.
- `DateInput` respeta el locale y la zona horaria.
- Autocompletado y `inputMode` adecuados en móvil.
- Envío con Enter cuando sea seguro.
- Protección contra doble envío.

### 4.4. Tablas y listas

- En móvil, tabla se transforma en lista de tarjetas.
- Columnas prioritarias visibles; detalles en expansión.
- Paginación clara.
- Ordenación accesible.
- Estados vacíos con explicación y acción.
- Importes alineados a la derecha y con formato local.

## 5. Accesibilidad

- Objetivo WCAG 2.1 AA.
- Contraste mínimo 4,5:1 en texto normal y 3:1 en texto grande.
- Foco visible y orden lógico.
- Navegación completa por teclado.
- Etiquetas asociadas a campos.
- Mensajes de error anunciados por lector de pantalla.
- No usar solo color para estados.
- Áreas táctiles mínimas de 44×44 px.
- Respetar `prefers-reduced-motion`.
- Textos alternativos en imágenes informativas.
- Idioma del documento actualizado (`lang="es"` / `lang="ca"`).
- Iconos decorativos con `aria-hidden`.
- Modales con trampa de foco y cierre con Escape.
- Gráficos con resumen textual o tabla equivalente.

## 6. Internacionalización visual

- Fechas: `Intl.DateTimeFormat` con `es-ES` y `ca-ES`.
- Números: `Intl.NumberFormat`.
- Importes: `EUR`, dos decimales, símbolo según locale.
- Porcentajes y coeficientes con precisión definida.
- Fechas relativas con `Intl.RelativeTimeFormat` cuando aporte valor.
- No concatenar fragmentos traducidos.
- Evitar textos que dependan de género; usar formulaciones neutras cuando sea posible.
- El contenido escrito por usuarios se muestra en su idioma original, sin traducir.

### 6.1. Glosario catalán inicial

| Español | Catalán |
| --- | --- |
| Comunidad de propietarios | Comunitat de propietaris |
| Junta | Junta |
| Acta | Acta |
| Convocatoria | Convocatòria |
| Orden del día | Ordre del dia |
| Cuota | Quota |
| Derrama | Derrama |
| Ejercicio | Exercici |
| Presupuesto | Pressupost |
| Incidencia | Incidència |
| Propietario | Propietari |
| Inquilino | Inquilí |
| Administrador de fincas | Administrador de finques |
| Presidente/a | President/a |
| Secretario/a | Secretari/ària |
| Tesorero/a | Tresorer/a |
| Coeficiente | Coeficient |
| Recibo | Rebut |
| Gasto | Despesa |
| Ingreso | Ingrés |
| Saldo | Saldo |
| Proveedor | Proveïdor |
| Vivienda | Habitatge |
| Local | Local |
| Garaje | Garatge |
| Trastero | Traster |

La terminología debe revisarse con una persona competente en catalán administrativo. No se traducen
nombres propios, documentos oficiales ni mensajes de usuarios.

## 7. Contenido y tono

- Frases cortas y directas.
- Voz activa.
- Explicar consecuencias antes de acciones destructivas.
- Evitar tecnicismos innecesarios.
- Errores: qué ocurrió, por qué y qué hacer.
- Confirmaciones: qué se ha hecho y qué se puede hacer después.
- Avisos legales visibles y no ambiguos.
- No prometer resultados ni validez jurídica.

## 8. Iconografía e imágenes

- Iconografía lineal, coherente, con trazo uniforme.
- Set definitivo pendiente de decisión (preferible autoalojado).
- No usar emojis como iconos funcionales.
- Ilustraciones sobrias y no infantiles.
- Fotografías de incidencias con tratamiento de privacidad.
- Logotipo y manual de marca pendientes.

## 9. Movimiento

- Transiciones de 150–200 ms.
- Sin animaciones llamativas.
- Respetar reducción de movimiento.
- No animar importes ni datos críticos de forma que dificulte la lectura.

## 10. Implementación

- Tokens en Sass y variables CSS.
- Nombres semánticos, no basados en apariencia.
- Componentes documentados con estados: default, hover, focus, active, disabled, loading, error.
- Cada componente debe tener pruebas de accesibilidad básicas.
- No usar estilos inline salvo casos justificados.
- Evitar dependencias de UI pesadas sin evaluación.

Ejemplo de tokens:

```scss
$color-navy-900: #0a2342;
$color-blue-600: #1b5e9e;
$color-turquoise-600: #0f766e;
$color-gray-50: #f6f8fa;
$color-gray-900: #0f172a;
$color-danger: #b91c1c;

$space-1: 4px;
$space-2: 8px;
$space-3: 12px;
$space-4: 16px;
$space-5: 24px;

$radius-md: 8px;
$radius-lg: 12px;
```

## 11. Pendientes

| Elemento | Estado |
| --- | --- |
| Logotipo y manual de marca | Pendiente |
| Set de iconos | Pendiente |
| Tipografía definitiva y licencia | Pendiente |
| Validación de contraste con herramienta | Pendiente |
| Wireframes de flujos críticos | FASE 1 |
| Auditoría de accesibilidad | FASE 1/4 |
| Modo oscuro | No incluido en MVP |

---

*Fin de docs/DESIGN-SYSTEM.md — v0.1.0 (FASE 0).*
