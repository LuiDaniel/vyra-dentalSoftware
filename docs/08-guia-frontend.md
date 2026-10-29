# Vyra — Guía de Frontend (JavaFX)

**Versión:** 0.1
**Fecha:** 2026-10-29

Este documento dice **cómo se ve y cómo se comporta cada pantalla**, para implementarla en JavaFX con FXML + CSS. Va de la mano con el diseño de referencia:

- **Diseño de referencia (HTML):** carpeta [`docs/ui/`](./ui). Abre `docs/ui/Inicio.html` en el navegador; el menú lateral enlaza las demás pantallas. Los datos son de ejemplo.
- **Lienzo interactivo (original):** https://claude.ai/artifact/5kwuCb73dLZPVYQn4iB68W. Es privado hasta que se comparta desde su menú Compartir. Allí el odontograma, la agenda y los formularios responden a clics.

Cuando el HTML y este documento no coincidan, **manda este documento**.

---

## 1. Principios

1. **Una sola hoja de estilos** (`styles/vyra.css`) con variables de color. Nada de colores escritos a mano en el FXML.
2. **El estado siempre con texto + color**, nunca solo con color (píldoras con texto, piezas dentales con letra).
3. **Errores junto al campo**, en rojo, con el mensaje de Jakarta o del servicio. Sin ventanas emergentes para validar.
4. **Un solo botón primario** (verde azulado) por pantalla o panel.
5. **Patrón lista + panel:** las pantallas de gestión tienen la tabla a la izquierda y el formulario/detalle a la derecha. Seleccionar una fila llena el panel; "Nuevo..." lo vacía.
6. **Textos en español de Perú**, con tildes y "S/" para soles.

## 2. Colores (variables CSS)

Se declaran una sola vez en `.root` de `vyra.css`. JavaFX permite usarlas en cualquier regla (`-fx-background-color: -vy-primario;`).

| Variable | Valor | Uso |
|---|---|---|
| `-vy-primario` | `#0F766E` | Botón primario, enlaces, selección, bordes de foco |
| `-vy-primario-hover` | `#115E59` | Botón primario al pasar el mouse |
| `-vy-acento` | `#2DD4BF` | Logo, ícono del ítem activo del menú, contadores del menú |
| `-vy-primario-suave` | `#F0FDFA` | Fondo de la fila seleccionada, filtro activo, aviso informativo |
| `-vy-primario-borde` | `#99F6E4` | Borde del aviso informativo, barras secundarias del gráfico |
| `-vy-sidebar` | `#0B1F2A` | Fondo del menú lateral y del panel izquierdo del login |
| `-vy-sidebar-activo` | `#123B40` | Fondo del ítem activo del menú |
| `-vy-sidebar-hover` | `#132D38` | Fondo del ítem del menú al pasar el mouse |
| `-vy-sidebar-texto` | `#C3D0D8` | Texto de los ítems del menú |
| `-vy-sidebar-tenue` | `#7E95A3` | Títulos de grupo del menú (PRINCIPAL, CLÍNICA...) |
| `-vy-tinta` | `#0F172A` | Texto principal |
| `-vy-texto-2` | `#475569` | Texto secundario (subtítulos, columnas) |
| `-vy-texto-3` | `#64748B` | Texto tenue (placeholders, ayudas) |
| `-vy-borde` | `#E2E8F0` | Bordes de tarjetas y separadores |
| `-vy-borde-campo` | `#CBD5E1` | Borde de campos y botones secundarios |
| `-vy-fondo` | `#F4F7F9` | Fondo general del contenido |
| `-vy-superficie` | `#FFFFFF` | Tarjetas, paneles, barra superior |
| `-vy-error` | `#DC2626` | Borde de campo con error |
| `-vy-error-texto` | `#B91C1C` | Mensaje de error bajo el campo, botón de peligro |
| `-vy-alerta-naranja` | `#EA580C` | Punto de notificación, línea de "hora actual" en la agenda |

`vyra.css` define además variables complementarias tomadas de `docs/ui` (`-vy-etiqueta` `#334155`, `-vy-separador` `#F1F5F9`, `-vy-fila-hover` `#F8FAFC`, `-vy-halo`, `-vy-sidebar-sub` `#8FA3B0`, `-vy-sidebar-tarjeta` `#10293A`...) y una variable por color de píldora y de aviso (`-vy-pill-*`, `-vy-aviso-*`).

**Estados** (fondo de píldora / texto):

| Estado | Fondo | Texto | Clase CSS |
|---|---|---|---|
| Cita `PENDIENTE` / Tratamiento `EN_CURSO` | `#FEF3C7` | `#92400E` | `.pill-pendiente` |
| Cita `CONFIRMADA` | `#DBEAFE` | `#1E40AF` | `.pill-confirmada` |
| Cita `ATENDIDA` / Tratamiento `FINALIZADO` | `#D1FAE5` | `#065F46` | `.pill-atendida` |
| Cita `CANCELADA` | `#FEE2E2` | `#991B1B` | `.pill-cancelada` |
| Alergia | `#FFEDD5` | `#9A3412` | `.pill-alergia` |

**Bloques de la agenda** (fondo / borde / texto): Pendiente `#FFFBEB / #FDE68A / #78350F`, Confirmada `#EFF6FF / #BFDBFE / #1E3A8A`, Atendida `#ECFDF5 / #A7F3D0 / #064E3B`, Cancelada `#F8FAFC / #E2E8F0 / #64748B` con el nombre tachado.

**Piezas del odontograma** (relleno / borde / letra):

| `EstadoPieza` | Relleno | Borde | Letra |
|---|---|---|---|
| `SANA` | `#FFFFFF` | `#64748B` | (ninguna) |
| `CARIADA` | `#FCA5A5` | `#B91C1C` | C |
| `OBTURADA` | `#93C5FD` | `#1D4ED8` | O |
| `EN_TRATAMIENTO` | `#FCD34D` | `#B45309` | T |
| `EXTRAIDA` | `#F1F5F9` | `#64748B` punteado | X (y una X dibujada encima) |

**Colores de avatar** (fondo / texto), elegidos con `id % 7`: `#CCFBF1/#115E59`, `#DBEAFE/#1E40AF`, `#EDE9FE/#5B21B6`, `#FEF3C7/#92400E`, `#FFE4E6/#9F1239`, `#E0F2FE/#075985`, `#FCE7F3/#9D174D`.

## 3. Tipografía, espacios y formas

- **Fuente:** Inter (Regular 400, Medium 500, SemiBold 600, Bold 700), en `resources/com/vyra/fonts/`. Se carga en `App.start()` con `Font.loadFont(getClass().getResourceAsStream("/com/vyra/fonts/Inter-Regular.ttf"), 14)` (una vez por archivo). Si falla, JavaFX usa la fuente del sistema.
- **Importante (pesos en JavaFX):** JavaFX registra Medium y SemiBold como familias aparte ("Inter Medium", "Inter SemiBold"), así que `-fx-font-weight: 600` sobre "Inter" se ve Regular. En `vyra.css`: 500 → `-fx-font-family: "Inter Medium"`, 600 → `-fx-font-family: "Inter SemiBold"`, 700 → `-fx-font-weight: bold` (con la familia "Inter").

| Uso | Tamaño / peso | Clase |
|---|---|---|
| Título de página | 28 / Bold | `.titulo-pagina` |
| Cifra destacada (KPI) | 30 / Bold | `.kpi-valor` |
| Título de tarjeta | 16 / SemiBold | `.card-titulo` |
| Título de panel de formulario | 18 / Bold | `.panel-titulo` |
| Cuerpo | 14 / Regular | (por defecto en `.root`) |
| Etiqueta de campo | 13 / SemiBold, color `#334155` | `.campo-etiqueta` |
| Texto de apoyo | 12 / Medium, `-vy-texto-2` | `.texto-apoyo` |
| Encabezado de tabla | 12 / SemiBold, mayúsculas, `-vy-texto-2` | `.table-view .column-header` |

- **Espacios:** usar solo 4, 8, 12, 16, 20, 24, 32 px. Contenido de página con padding 28 px arriba y 32 px a los lados; separación entre tarjetas 16–20 px.
- **Radios:** 10 px en botones y campos, 14 px en tarjetas, 999 px (completamente redondo) en píldoras y avatares.
- **Sombras:** tarjeta `dropshadow(gaussian, rgba(15,23,42,0.05), 4, 0, 0, 1)`; panel de formulario `dropshadow(gaussian, rgba(15,23,42,0.08), 16, 0, 0, 4)`.
- **Alturas:** botones 40 px, campos 40 px, ítems del menú 40 px, filas de tabla 58 px.

## 4. Clases CSS que debe tener `vyra.css`

| Clase | Se aplica a | Aspecto |
|---|---|---|
| `.app-shell` | `BorderPane` de `MainView` | Fondo `-vy-fondo` |
| `.sidebar` | `VBox` del menú | Fondo `-vy-sidebar`, ancho 240, padding 24/16 |
| `.nav-grupo` | `Label` de grupo | 11 px, SemiBold, mayúsculas, `-vy-sidebar-tenue` |
| `.nav-item` | `ToggleButton` del menú | 40 px de alto, radio 10, texto `-vy-sidebar-texto`; `:hover` fondo `-vy-sidebar-hover`; `:selected` fondo `-vy-sidebar-activo`, texto blanco, ícono `-vy-acento` |
| `.nav-badge` | contador del menú | Fondo `-vy-acento`, texto `-vy-sidebar`, 11 px Bold, píldora |
| `.topbar` | barra superior | Fondo blanco, borde inferior `-vy-borde`, padding 14/32 |
| `.card` | tarjetas | Fondo blanco, borde `-vy-borde`, radio 14, sombra de tarjeta |
| `.panel-form` | panel derecho de formularios | Igual que `.card` con sombra de panel |
| `.btn-primario` | `Button` | Fondo `-vy-primario`, texto blanco, SemiBold; `:hover` `-vy-primario-hover`; `:disabled` fondo `#E2E8F0`, texto `#64748B` |
| `.btn-secundario` | `Button` | Fondo blanco, borde `-vy-borde-campo`, texto `-vy-tinta` |
| `.btn-peligro` | `Button` | Fondo blanco, borde `#FECACA`, texto `-vy-error-texto` |
| `.btn-icono` | `Button` solo ícono | 34–40 px cuadrado, fondo transparente; `:hover` `#F1F5F9` |
| `.chip`, `.chip:selected` | filtros (`ToggleButton`) | Píldora 32 px; seleccionado: borde y texto `-vy-primario`, fondo `-vy-primario-suave` |
| `.segmentado` | `HBox` de `ToggleButton` (Día/Semana/Lista) | Fondo `#E9EEF3`, radio 10, padding 3; botón seleccionado blanco con sombra leve |
| `.text-field`, `.combo-box`, `.date-picker`, `.text-area` | campos | Borde `-vy-borde-campo`, radio 9, alto 40; `:focused` borde `-vy-primario` y halo `rgba(20,184,166,0.18)` |
| `.campo-error` | campo con error (se agrega por código) | Borde `-vy-error` |
| `.mensaje-error` | `Label` bajo el campo | 12 px, `-vy-error-texto` |
| `.pill` + `.pill-*` | `Label` de estado | 12 px SemiBold, padding 4/10, radio 999. `.pill-neutra` (gris) para el método de pago |
| `.icono` | `SVGPath` de `ui.Icono` | Color con `-fx-stroke` según el contenedor (`.nav-item:selected .icono`, `.btn-primario .icono`...) |
| `.campo-icono` | `HBox` con ícono + `TextField` (login) | Dibuja el borde del campo; `:focus-within` muestra el halo |
| `.avatar` | `StackPane` con `Label` | Círculo de 38 px (52 px en encabezados), iniciales 13 px Bold |
| `.aviso-info`, `.aviso-exito`, `.aviso-advertencia`, `.aviso-error` | `HBox` de aviso | Ver colores en la pantalla Sistema del diseño |
| `.kpi` | tarjeta de indicador | `.card` con padding 18/20 |
| `.table-view` | tablas | Sin bordes externos, encabezado transparente, filas 58 px, separador `#F1F5F9`, `:hover` `#F8FAFC`, `:selected` `-vy-primario-suave` con texto oscuro |
| `.pieza`, `.pieza:selected` | botón del odontograma | 54×88, radio 12; seleccionado borde 2 px `-vy-primario` y fondo `-vy-primario-suave` |
| `.agenda-hora`, `.agenda-linea`, `.cita-bloque`, `.cita-*` | agenda | Ver sección 7.3 |

## 5. Íconos

Son trazos SVG de 24×24 (sin relleno, borde de 2 px, puntas redondeadas). En JavaFX se dibujan con `SVGPath`: `setContent(d)`, `setFill(null)`, `setStroke(color)`, `setStrokeWidth(2)`, `setStrokeLineCap(ROUND)`, `setStrokeLineJoin(ROUND)`. La clase `ui.Icono` recibe el nombre y el tamaño (18 px en el menú, 16 en botones) y escala el trazo. Para que ocupe el tamaño correcto, envolver el `SVGPath` escalado en un `Group`.

| Nombre | Dónde | Trazo (`d`) |
|---|---|---|
| `inicio` | Menú | `M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z` |
| `agenda` | Menú, botones | `M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zM16 2v4M8 2v4M3 10h18` |
| `pacientes` | Menú | `M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M5 7a4 4 0 1 0 8 0a4 4 0 1 0-8 0M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75` |
| `diente` | Logo, menú, odontograma | `M12 5.5C10.5 4 8.5 3 6.5 3.5 3.5 4.3 3 7.5 3.8 10.5c.6 2.2 1.2 4 1.5 6.5.3 2.3.8 4 2 4 1.7 0 1.7-5 4.7-5s3 5 4.7 5c1.2 0 1.7-1.7 2-4 .3-2.5.9-4.3 1.5-6.5.8-3 .3-6.2-2.7-7C15.5 3 13.5 4 12 5.5z` |
| `historia` | Menú | `M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 2v6h6M16 13H8M16 17H8` |
| `tratamientos` | Menú | `M9 2h6a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1zM16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2M9 12h6M9 16h4` |
| `pagos` | Menú | `M4 5h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2zM2 10h20M6 15h4` |
| `reportes` | Menú | `M3 3v18h18M7 16v-4M12 16V8M17 16v-7` |
| `profesionales` | Menú | `M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M5 7a4 4 0 1 0 8 0a4 4 0 1 0-8 0M16 11l2 2 4-4` |
| `usuarios` | Menú | `M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z` |
| `buscar` | Barra superior, filtros | `M4 11a7 7 0 1 0 14 0a7 7 0 1 0-14 0M21 21l-4.3-4.3` |
| `campana` | Notificaciones | `M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.94 1.94 0 0 0 3.4 0` |
| `mas` | Botones "Nuevo..." | `M12 5v14M5 12h14` |
| `salir` | Cerrar sesión | `M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9` |
| `reloj` | Aviso de citas próximas | `M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0M12 6v6l4 2` |
| `alerta` | Alergias, advertencias | `M12 9v4M12 17h.01M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z` |
| `error` | Avisos de error | `M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0M12 8v4M12 16h.01` |
| `check` | Éxito, listas | `M20 6 9 17l-5-5` |
| `cerrar` | Quitar selección | `M18 6 6 18M6 6l12 12` |
| `descargar` | Exportar PDF | `M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3` |
| `anterior` / `siguiente` | Navegar fechas | `m15 18-6-6 6-6` / `m9 18 6-6-6-6` |
| `usuario` | Campo de login | `M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2M8 7a4 4 0 1 0 8 0a4 4 0 1 0-8 0` |
| `candado` | Campo de contraseña | `M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2zM7 11V7a5 5 0 0 1 10 0v4` |
| `ojo` | Mostrar contraseña | `M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12zM9 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0` |
| `efectivo` | Método de pago | `M2 6h20v12H2zM9.5 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0M6 12h.01M18 12h.01` |
| `celular` | Yape / Plin | `M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zM12 18h.01` |
| `banco` | Transferencia | `M3 21h18M3 10h18M5 6l7-3 7 3M4 10v11M20 10v11M8 14v3M12 14v3M16 14v3` |
| `dinero` | KPI ingresos | `M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6` |

## 6. Estructura común (`MainView.fxml` → `MainController`)

Referencia: cualquier pantalla de `docs/ui` excepto Login y Sistema.

```
BorderPane.app-shell
├── left: VBox.sidebar (240 px)
│   ├── Logo: cuadrado 38 px radio 11 color -vy-acento con ícono "diente" + "Vyra" (18 Bold blanco) + "Consultorio dental" (12, #8FA3B0)
│   ├── Grupo PRINCIPAL: Inicio, Agenda (con contador de citas de hoy), Pacientes
│   ├── Grupo CLÍNICA: Odontograma, Historia clínica, Tratamientos
│   ├── Grupo ADMINISTRACIÓN: Pagos, Reportes, Profesionales, Usuarios y roles
│   └── (abajo) Tarjeta de usuario: avatar + nombre completo + rol + botón "salir"
└── center: VBox
    ├── HBox.topbar: ruta ("Inicio / Pacientes") o buscador global, fecha larga, botón campana
    └── StackPane "contenido"  ← aquí se carga cada pantalla (dentro de un ScrollPane)
```

- Los ítems del menú son `ToggleButton` en un `ToggleGroup`, para que solo uno quede seleccionado.
- Se ocultan los grupos e ítems que el rol no puede ver (matriz en [`02-casos-de-uso.md`](./02-casos-de-uso.md#3-matriz-de-permisos-por-rol)).
- El buscador global (en Inicio) busca pacientes por nombre o DNI y, al elegir uno, abre su ficha en Pacientes. Atajo `Ctrl+K`.
- La fecha de la barra superior va en formato largo: "Jueves, 29 de octubre de 2026" (ver sección 9).

## 7. Pantallas

Cada pantalla indica: archivo FXML, controlador, requerimientos que cubre, roles que la ven, distribución, de dónde salen los datos y qué hace cada interacción.

### 7.1 Inicio de sesión — `LoginView.fxml` / `LoginController`
**Referencia:** `docs/ui/Login.html` · **Cubre:** RF-08, CU-08 · **Roles:** todos (sin sesión)

- **Distribución:** `HBox` en dos mitades. Izquierda: panel `-vy-sidebar` con logo, el titular "Tu consultorio, en orden." (44 Bold blanco), un subtítulo y 3 beneficios con ícono `check`, un diente grande decorativo de fondo y "Versión 1.0 · Uso interno del consultorio" abajo. Derecha: formulario centrado de 380 px de ancho.
- **Formulario:** título "Iniciar sesión", subtítulo, campo Usuario (ícono `usuario`), campo Contraseña (ícono `candado`, `PasswordField` + botón `ojo` que alterna con un `TextField` para mostrarla), casilla "Recordar mi usuario", botón primario "Ingresar" de ancho completo (48 px).
- **Comportamiento:** Enter en cualquier campo = Ingresar. Llama a `AuthService.iniciarSesion()`. Si falla, muestra un `aviso-error` bajo el botón: "Usuario o contraseña incorrectos." (sin decir cuál; RN-13). Si el usuario está desactivado: "Tu usuario está desactivado. Consulta con el administrador." Al acertar, se abre `MainView` en Inicio.
- "Recordar mi usuario" guarda solo el nombre de usuario con `java.util.prefs.Preferences`, nunca la contraseña.

### 7.2 Inicio (dashboard) — `InicioView.fxml` / `InicioController`
**Referencia:** `docs/ui/Inicio.html` · **Cubre:** RF-12, CU-12 · **Roles:** todos (el odontólogo ve solo sus citas)

- **Encabezado:** "Buenos días/tardes/noches, {nombre}" según la hora (antes de las 12, antes de las 19, resto) + "Este es el resumen de tu consultorio para hoy." A la derecha: botón secundario "Nuevo paciente" y primario "Nueva cita" (abren esas pantallas con el formulario vacío).
- **Aviso de citas próximas (RF-12):** `aviso-info` con ícono `reloj`: "{n} citas en la próxima hora" y la lista "María Quispe 09:00 · ...". Enlace "Ver agenda". Se oculta si no hay citas en la próxima hora.
- **4 KPI** (`TilePane` o `GridPane` de 4 columnas): Citas de hoy (y cuántas pendientes), Pacientes registrados (y nuevos del mes), Tratamientos en curso, Ingresos del mes (y % respecto al mes anterior). Cada uno con su ícono en un cuadrado de color suave. **El KPI de ingresos solo lo ve el Administrador**; para los demás se reemplaza por "Pagos registrados hoy".
- **Agenda de hoy** (tarjeta grande): lista de filas con hora inicio/fin, avatar, nombre del paciente, "motivo · profesional" y píldora de estado. Clic en una fila → Agenda con esa cita seleccionada.
- **Ingresos de la semana** (solo Administrador): barras verticales lunes a sábado; el día actual en `-vy-primario`, los demás en `-vy-primario-borde`. Se puede hacer con `HBox` de `Region` con alto proporcional (no hace falta `BarChart`).
- **Accesos rápidos:** cuadrícula 2×2 de botones con ícono: Registrar paciente, Agendar cita, Registrar pago, Abrir odontograma.
- **Datos:** `CitaService.citasDelDia(hoy)`, `PacienteService`, `TratamientoService`, `ReporteService.ingresosPorPeriodo()`.

### 7.3 Agenda — `AgendaView.fxml` / `AgendaController`
**Referencia:** `docs/ui/Citas.html` · **Cubre:** RF-02, RF-03, CU-02, CU-03 · **Roles:** Admin y Recepción (completo); Odontólogo (solo lectura de sus citas + "Marcar atendida")

- **Barra:** título "Agenda", botones `anterior` / "Hoy" / `siguiente`, fecha seleccionada ("Jueves 29 de octubre"), selector segmentado Día / Semana / Lista.
- **Tira de la semana:** 6 botones (lunes a sábado) con día, número y cantidad de citas. El día elegido va relleno con `-vy-primario`.
- **Vista Día (principal):** `GridPane` con una columna de horas (64 px) y una columna por odontólogo (encabezado con avatar, nombre y especialidad). Filas de 30 min (56 px de alto) de 08:00 a 20:00 (RN-05); línea más marcada en las horas en punto. Cada cita es un `VBox.cita-bloque` ubicado con `GridPane.setRowIndex` / `setRowSpan` según su duración, con nombre del paciente, estado, rango horario y motivo. Una línea naranja marca la hora actual.
- **Vista Semana:** misma cuadrícula con columnas = días (filtrando por un odontólogo elegido en un `ComboBox`). **Vista Lista:** `TableView` con fecha, hora, paciente, profesional, motivo, estado.
- **Clic en una cita:** el panel derecho muestra su detalle con botones según el estado: Confirmar, Reprogramar, Cancelar (con confirmación) y Marcar atendida.
- **Panel "Nueva cita" (derecha, 340–380 px):**
  - Paciente: buscador con autocompletado (nombre o DNI); al elegir, se muestra como tarjeta con avatar, nombre, DNI y una ✕ para cambiarlo.
  - Odontólogo (`ComboBox` de "Dr. Marco Salinas · Endodoncia"), Fecha (`DatePicker`, sin fechas pasadas: RN-08), Inicio (`ComboBox` de horas cada 30 min), Fin (calculado; por defecto inicio + 30 min, editable), Motivo (`TextArea`), Estado inicial (segmentado Pendiente / Confirmada).
  - **Validación de cruce en vivo (CU-03):** cada vez que cambia el odontólogo, la fecha o la hora, se llama a `CitaService.existeCruceDeHorario()`. Si hay cruce: borde rojo en "Inicio", `aviso-error` "**Cruce de horario.** El Dr. Salinas ya atiende a María Quispe de 09:00 a 10:00." y una fila de chips "Horarios libres: 10:00 10:30 11:30" (`CitaService.horariosLibres()`); al hacer clic en un chip se cambia la hora. El botón "Agendar cita" queda deshabilitado mientras haya cruce. Sin cruce: `aviso-exito` "Horario disponible para el Dr. Salinas."
  - Al guardar: toast "Cita agendada", la cita aparece en la cuadrícula y el formulario se limpia.

### 7.4 Pacientes — `PacientesView.fxml` / `PacientesController`
**Referencia:** `docs/ui/Pacientes.html` · **Cubre:** RF-01, CU-01 · **Roles:** Admin y Recepción (completo); Odontólogo (lectura)

- **Encabezado:** "Pacientes", "{n} pacientes registrados · selecciona uno para ver o editar su ficha.", botón primario "Nuevo paciente".
- **Tarjeta de la lista:** buscador "Buscar por nombre o DNI" (filtra mientras se escribe, con 300 ms de espera), chips de filtro Todos / Con alergias / Menores de edad, y `TableView`:
  - Paciente: avatar + nombre (SemiBold) + "DNI 72481935" debajo (12 px). Se hace con un `cellFactory`.
  - Teléfono ("987 654 321"), Edad ("34 años"), Última visita ("29 oct 2026"), Alergias (píldora naranja con ícono `alerta`, o "Ninguna" en gris), Acciones (botones `diente` → Odontograma y `agenda` → Nueva cita para ese paciente).
  - Pie: "Mostrando 1–7 de 248" + botones Anterior / Siguiente (20 por página).
  - *Implementación (fase 3):* la lista se ordena por apellidos; los filtros se aplican en la consulta, así la paginación cuenta bien. "Última visita" muestra "—" hasta que existan las citas (fase 4). El panel tiene por ahora solo la pestaña Datos; las demás se agregan en sus fases.
- **Panel de ficha (derecha):** avatar grande + nombre completo + "DNI · Paciente desde {mes año}". Pestañas: **Datos**, **Historia clínica** (ver 7.5), **Citas** (lista de sus citas) y **Pagos** (sus pagos y saldo). En Datos, un formulario en 3 secciones:
  - Datos personales: Nombres*, Apellidos*, DNI*, Fecha de nacimiento.
  - Contacto: Teléfono, Contacto de emergencia, Dirección (ancho completo).
  - Información clínica: Alergias (`TextArea`); si tiene texto, debajo aparece un `aviso-advertencia` "Esta alergia se mostrará como alerta al abrir el odontograma." Antecedentes (`TextArea`).
  - Pie del panel: "Dar de baja" (`btn-peligro`, solo con un paciente existente; pide confirmación), "Cancelar" y el primario "Guardar cambios" o "Registrar paciente". Enter en un campo de texto guarda; en el diálogo de confirmación, Enter **cancela** (la baja hay que elegirla a propósito).
- **Errores:** bajo cada campo, con el mensaje del modelo ("El DNI debe tener 8 dígitos"). DNI duplicado: "Ya existe un paciente con el DNI 72481935".

### 7.5 Historia clínica — pestaña de la ficha / `HistoriaClinicaController`
**Referencia:** no tiene pantalla propia en `docs/ui`; usa el mismo estilo que la ficha de Pacientes · **Cubre:** RF-04, CU-04 · **Roles:** Odontólogo (completo); Admin (lectura)

- El ítem "Historia clínica" del menú abre un buscador de pacientes; al elegir uno, muestra su ficha con la pestaña Historia clínica activa.
- **Arriba:** antecedentes y alergias del paciente (editables por el odontólogo).
- **Línea de tiempo** de entradas, de la más reciente a la más antigua: fecha, profesional, diagnóstico (SemiBold) y observaciones. Mismo estilo que el "Historial de la pieza" del odontograma (punto + línea vertical).
- **Botón primario "Nueva entrada":** abre un formulario en el mismo panel con Fecha (hoy, no editable), Cita (la cita de hoy del paciente, si existe), Diagnóstico y Observaciones.
- Las entradas no se editan ni se borran (RN-09). Botón secundario "Exportar PDF" (RF-11).

### 7.6 Odontograma — `OdontogramaView.fxml` / `OdontogramaController`
**Referencia:** `docs/ui/Odontograma.html` (y la versión interactiva del lienzo) · **Cubre:** RF-05, CU-05, RF-11 · **Roles:** Odontólogo (completo); Admin (lectura)

- **Tarjeta del paciente:** avatar, nombre completo, "DNI · edad · Cita actual: {motivo}", píldora naranja "Alergia: Penicilina" si tiene alergias. A la derecha: "Exportar PDF" (secundario) y "Guardar {n} cambios" (primario; deshabilitado y con el texto "Guardado" si no hay cambios pendientes).
- **Barra de herramientas:** "Marcar como:" + 5 `ToggleButton` en un `ToggleGroup` (Sana, Cariada, Obturada, En tratamiento, Extraída), cada uno con su cuadrito de color, la letra y el conteo de piezas en ese estado. A la derecha: "Notación FDI · haz clic en una pieza para aplicar el estado".
- **Arcada:** dos filas, cada una de dos grupos de 8 piezas separados por una línea vertical (línea media):
  - Superior: 18 17 16 15 14 13 12 11 │ 21 22 23 24 25 26 27 28 (número arriba, diente con la raíz hacia arriba, letra abajo).
  - Inferior: 48 47 46 45 44 43 42 41 │ 31 32 33 34 35 36 37 38 (letra arriba, diente, número abajo).
  - Rótulos "Derecha del paciente", "Maxilar superior", "Izquierda del paciente" y "Mandíbula inferior".
  - Cada pieza es un `ui.PiezaDental` (un `Button` con el `SVGPath` "diente" de 40×48 como gráfico, rotado 180° en la arcada superior). Relleno, borde y letra según la tabla de la sección 2.
- **Interacción:** clic en una pieza = seleccionarla y aplicarle el estado elegido en la barra. Los cambios quedan **pendientes** (en un `Map<Integer, EstadoPieza>` en el controlador) hasta pulsar "Guardar", que llama a `OdontogramaService.registrarCambios()` (una fila nueva por pieza cambiada; RN-09). Al salir de la pantalla con cambios pendientes, pedir confirmación.
- **Debajo, dos tarjetas:**
  - Pieza seleccionada: cuadro con el número y su color, "Pieza 36 · Primer molar inferior izquierdo", "Estado actual: En tratamiento", campo "Observación del cambio" y enlace "Registrar tratamiento para esta pieza" (abre Tratamientos con la pieza ya elegida).
  - Historial de la pieza: línea de tiempo (estado, fecha, profesional y nota). Los cambios sin guardar aparecen arriba como "Hoy · sin guardar". Si no hay registros: "Sin cambios registrados..."
- **Nombre de las piezas:** posición (1 incisivo central, 2 incisivo lateral, 3 canino, 4 primer premolar, 5 segundo premolar, 6 primer molar, 7 segundo molar, 8 tercer molar) + cuadrante (1 superior derecho, 2 superior izquierdo, 3 inferior izquierdo, 4 inferior derecho).

### 7.7 Tratamientos — `TratamientosView.fxml` / `TratamientosController`
**Referencia:** no tiene pantalla propia; seguir el patrón de `docs/ui/Pagos.html` (tabla + panel) · **Cubre:** RF-06, CU-06 · **Roles:** Odontólogo (completo); Admin y Recepción (lectura)

- **Encabezado:** "Tratamientos" + selector segmentado En curso / Finalizados / Todos + buscador por paciente.
- **Tabla:** Fecha, Paciente (avatar + nombre), Descripción, Piezas (chips pequeños con los números: "36", "47"), Costo, Pagado (barra de progreso + "S/ 250 de S/ 650"), Estado (píldora).
- **Panel "Nuevo tratamiento":** Cita (las citas de hoy y las atendidas recientes, "29 oct · María Quispe · Endodoncia"), Descripción*, Piezas (mini odontograma de selección múltiple o campo "36, 47" validado con la notación FDI), Costo (S/)*, Estado (segmentado En curso / Finalizado).
- Seleccionar uno existente muestra su detalle, sus pagos y el botón "Marcar como finalizado".

### 7.8 Pagos — `PagosView.fxml` / `PagosController`
**Referencia:** `docs/ui/Pagos.html` · **Cubre:** RF-10, CU-10 · **Roles:** Admin y Recepción

- **Encabezado:** "Pagos", subtítulo, selector de periodo Hoy / Semana / {Mes actual}.
- **3 tarjetas:** Cobrado en el periodo (y % respecto al periodo anterior), Saldo pendiente de tratamientos (y en cuántos tratamientos), Métodos de pago (barra apilada horizontal con el % de cada método y su leyenda).
- **Tabla "Pagos recientes":** Fecha, Paciente, Concepto (descripción del tratamiento), Método (píldora: Efectivo verde, Yape/Plin violeta `#EDE9FE/#5B21B6`, Tarjeta azul, Transferencia gris), Monto (alineado a la derecha, Bold). Botón "Exportar".
- **Panel "Registrar pago":**
  - Tratamiento: buscador; al elegir, una tarjeta con descripción, paciente, estado, costo, barra de progreso de lo pagado y "Pagado S/ 250.00 · Saldo **S/ 400.00**".
  - Monto (S/)* y Fecha de pago* (hoy por defecto; no futura).
  - Método de pago*: 4 botones grandes en cuadrícula 2×2 con ícono (Efectivo, Yape / Plin, Tarjeta, Transferencia), uno seleccionado.
  - N.º de operación (opcional).
  - Pie: "Saldo después de este pago: S/ 200.00" (se recalcula al escribir) y botón primario de ancho completo "Registrar pago de S/ 200.00".
- **Errores:** monto mayor al saldo → "El monto supera el saldo pendiente (S/ 400.00)".

### 7.9 Reportes — `ReportesView.fxml` / `ReportesController`
**Referencia:** sin pantalla propia; usar tarjetas `.card` y el estilo de KPI de Inicio · **Cubre:** RF-09, CU-09 · **Roles:** Admin

- Selector de tipo de reporte (segmentado): Citas, Historial de paciente, Ingresos.
- Filtros según el tipo: rango de fechas (dos `DatePicker` + atajos Hoy / Esta semana / Este mes), profesional, paciente.
- **Citas:** KPI (total, atendidas, canceladas, % de asistencia) + tabla.
- **Historial de paciente:** tratamientos del paciente con costo, pagado y saldo.
- **Ingresos:** total + tabla por método de pago + barras por día.
- Botón "Exportar PDF" (en un `Task`, con indicador de progreso).

### 7.10 Profesionales — `ProfesionalesView.fxml` / `ProfesionalesController`
**Cubre:** RF-07, CU-07 · **Roles:** Admin. Patrón lista + panel: tabla (avatar, nombre, especialidad, teléfono, horario, usuario asociado) y formulario (nombres*, apellidos*, especialidad, teléfono, horario de atención, `ComboBox` de usuario con rol Odontólogo).

### 7.11 Usuarios y roles — `UsuariosView.fxml` / `UsuariosController`
**Cubre:** RF-08 · **Roles:** Admin. Patrón lista + panel: tabla (avatar, nombre completo, usuario, rol en píldora, estado Activo/Inactivo) y formulario (nombre completo*, usuario*, rol* segmentado, contraseña* solo al crear o con un botón "Restablecer contraseña", interruptor Activo). Un administrador no puede desactivarse a sí mismo.

## 8. Componentes reutilizables (`com.vyra.dental.ui`)

| Componente | Qué es | Uso |
|---|---|---|
| `Icono` | `Group` con un `SVGPath` escalado. Constructor `Icono(String nombre, double tamano)`; usable en FXML con `<Icono nombre="agenda" tamano="18"/>` | Menú, botones, avisos |
| `Avatar` | `StackPane` circular con iniciales y color según el id | Tablas, encabezados, menú |
| `Pill` | `Label` con clase `.pill` + la clase del estado. Constructores para `EstadoCita`, `EstadoTratamiento` y `MetodoPago` | Tablas, agenda, Inicio |
| `PiezaDental` | `Button` con el diente, el número y la letra; propiedad `estado` que cambia su clase CSS | Odontograma, selección de piezas en Tratamientos |
| `Aviso` | `HBox` con ícono y texto; tipos INFO, EXITO, ADVERTENCIA, ERROR | Formularios, Inicio |
| `Notificacion` | Toast: `Notificacion.mostrar(ventana, "Paciente registrado")`, 3 s, abajo a la derecha | Después de guardar |
| `CampoFormulario` | `VBox` con etiqueta + control + mensaje de error; métodos `mostrarError(String)` y `limpiarError()` | Todos los formularios |

## 9. Formatos (`util.Formatos`)

| Dato | Formato | Ejemplo |
|---|---|---|
| Fecha larga (barra superior) | `EEEE, d 'de' MMMM 'de' yyyy`, `Locale("es","PE")`, primera letra en mayúscula | Jueves, 29 de octubre de 2026 |
| Fecha corta (tablas) | `dd MMM yyyy` | 29 oct 2026 |
| Hora | `HH:mm` | 09:30 |
| Dinero | `"S/ " + new DecimalFormat("#,##0.00")` | S/ 1,250.00 |
| Teléfono | grupos de 3 | 987 654 321 |
| Edad | años cumplidos con `Period.between` | 34 años |
| Iniciales | primera letra del primer nombre + primera del primer apellido | MQ |

## 10. Lista de verificación por pantalla

Antes de dar por terminada una pantalla:
- [ ] Se parece al HTML de `docs/ui` (colores, espacios, tipografía).
- [ ] No hay colores escritos a mano en el FXML; todo sale de `vyra.css`.
- [ ] Los campos obligatorios llevan `*` y muestran el error bajo el campo.
- [ ] Los estados se distinguen con texto o letra, no solo con color.
- [ ] Funciona con el teclado: Tab recorre los campos en orden y Enter envía el formulario.
- [ ] Respeta la matriz de permisos (se probó con los tres roles).
- [ ] Tiene un estado vacío ("Aún no hay pacientes registrados" + botón para crear el primero).
- [ ] Se ve bien a 1280×800 y maximizada.

---

*Siguiente documento: [`09-plan-implementacion.md`](./09-plan-implementacion.md) — en qué orden programar todo esto.*
