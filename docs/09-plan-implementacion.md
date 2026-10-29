# Vyra — Plan de Implementación

**Versión:** 0.1
**Fecha:** 2026-10-29

Este documento ordena el trabajo de programación en fases cortas. Cada fase deja la aplicación **compilando y ejecutable**, termina con un commit y tiene criterios claros para saber que está lista. Está pensado para seguirse en orden, ya sea a mano o pidiéndole cada fase a Claude Code (ver la sección 4).

## 1. Estado actual del código (2026-10-29, fase 5 en curso: historia clínica hecha, falta el odontograma)

| Parte | Estado |
|---|---|
| `pom.xml` | JavaFX 21, SQLite JDBC, Lombok, Hibernate Validator, JUnit 5 + Surefire. Compila con `release 21` (fase 0). |
| `model/` | 8 clases y 5 enums, alineados con la BD (fase 1). Los enums tienen `getEtiqueta()`; `EstadoPieza` además `getCodigo()` y los estados `getClaseCss()`. |
| `dao/` | Usuario, Profesional, Paciente y Cita completos ([`04` §2](./04-diagrama-clases.md#2-acceso-a-datos-comvyradentaldao-y-daoimpl)), en SQLite (`dao/impl/`) y en memoria (`dao/memoria/`). Historia clínica también (`HistoriaClinicaDAO`, solo `crear`, `buscarPorId` y `listarPorPaciente`: RN-09). Tratamiento y Pago solo en memoria y sin los métodos nuevos. Falta Odontograma. |
| `util/` | `ConexionBD` (abre SQLite, activa claves foráneas y ejecuta `schema.sql`), `Validador` (Jakarta), `HashUtil` (PBKDF2), `Formatos`, `Busqueda` (sin tildes) y `DatosIniciales` (admin inicial y datos de ejemplo). `schema.sql` es el de [`03` §3](./03-modelo-base-datos.md#3-script-sql-sqlite). |
| `service/` | `ReglaNegocioException`, `AuthService`, `Sesion`, `PacienteService`, `CitaService`, `ProfesionalService`, `HistoriaClinicaService` y `Servicios` (crea los servicios con sus DAO SQLite). |
| `controller/` | `LoginController`, `MainController` (menú por rol, navegación y contador de citas de hoy), `PacientesController`, `HistoriaClinicaController` (pestaña de la ficha), `ProfesionalesController`, `AgendaController`, enum `Pantalla` e interfaz `RecibeParametro`. |
| `ui/` | `Icono`, `Avatar`, `Pill`, `Aviso`, `Notificacion`, `CampoFormulario`, `Dialogos` (confirmación) y `EstadoVacio`. |
| Vistas FXML, CSS | `LoginView`, `MainView`, `PacientesView` (pestañas Datos, Historia clínica y Citas), `ProfesionalesView`, `AgendaView` (Día, Semana y Lista) y `styles/vyra.css`. Las demás pantallas muestran "En construcción" hasta que exista su FXML. |
| `App.java` | Carga Inter, abre la BD, crea el admin si no hay usuarios y muestra el login; `mostrarLogin()`/`mostrarPrincipal()` cambian la escena de la ventana. |
| `demo/` | 4 CRUDs de prueba + lanzador, hechos con código Java (sin FXML). |
| Diseño | Listo: [`docs/ui/`](./ui) y [`08-guia-frontend.md`](./08-guia-frontend.md). |

## 2. Convenciones

- **Idioma:** clases, métodos, variables y textos en español, sin tildes en los identificadores (`agendarCita`, `contrasenaHash`). Textos visibles con tildes.
- **Nombres:** `XxxView.fxml` + `XxxController`; `XxxDAO` (interfaz) + `XxxDAOSqlite`; `XxxService`.
- **Lombok** en el modelo (`@Data @NoArgsConstructor @AllArgsConstructor`), como ya se usa.
- **Nada de SQL fuera de los DAO, nada de JavaFX fuera de `controller/` y `ui/`.**
- **Commits:** en español, en imperativo y con el módulo: `pacientes: agrega búsqueda por DNI`, `agenda: valida cruce de horario`. Un commit por tarea terminada.
- **Ramas:** una por fase o módulo (`fase-1-base`, `modulo-agenda`), con merge a `main` cuando la fase cumple sus criterios.
- **Datos de prueba:** siempre ficticios (RNF y memoria descriptiva §7). Nunca subir `vyra.db`.

## 3. Fases

### Fase 0 — Preparación del proyecto
**Objetivo:** que el proyecto tenga la estructura y las dependencias finales.

1. `pom.xml`: `maven.compiler.release` y `<release>` a **21**; agregar JUnit 5 (`junit-jupiter`, scope test) y `maven-surefire-plugin`.
2. Crear los paquetes vacíos `service`, `controller`, `ui`, `dao/memoria` y las carpetas de recursos `views/`, `styles/`, `fonts/` (ver [`05-arquitectura.md` §3](./05-arquitectura.md#3-estructura-de-paquetes)).
3. Descargar Inter (Regular, Medium, SemiBold, Bold `.ttf`, licencia OFL) en `resources/com/vyra/fonts/`.
4. `module-info.java`: dejar escritos (comentados) los `opens`/`exports` de los paquetes nuevos (ver arquitectura §3). Java no permite exportar ni abrir un paquete que todavía no tiene clases, así que cada línea se descomenta en la fase que crea la primera clase de ese paquete (fases 1 y 2).

**Listo cuando:** `mvn compile` y `mvn test` pasan, y `mvn javafx:run` abre la ventana actual.

### Fase 1 — Base: modelo, base de datos y utilidades
**Objetivo:** que el modelo y la BD coincidan con [`03`](./03-modelo-base-datos.md) y [`04`](./04-diagrama-clases.md).

1. Actualizar las clases del modelo según [`04` §5](./04-diagrama-clases.md#5-diferencias-con-el-código-actual-a-corregir). Crear el enum `MetodoPago`. Agregar `getEtiqueta()` a todos los enums, `getCodigo()` a `EstadoPieza` y `getClaseCss()` a los estados.
2. Reemplazar `schema.sql` por el script de [`03` §3](./03-modelo-base-datos.md#3-script-sql-sqlite).
3. `ConexionBD`: ejecutar `PRAGMA foreign_keys = ON` al abrir.
4. `util.HashUtil`: `hashear(String)` y `verificar(String, String)` con PBKDF2WithHmacSHA256 (sal aleatoria de 16 bytes, 120 000 iteraciones, formato `pbkdf2$iteraciones$salBase64$hashBase64`). Quitar `autenticar()`/`hashear()` de `Usuario`.
5. `util.Formatos` (sección 9 de la guía de frontend) y `service.ReglaNegocioException`. Descomentar `exports com.vyra.dental.service` en `module-info.java`.
6. Mover los `*DAOImpl` actuales a `dao/memoria/*DAOMemoria` y ajustar los demos para que sigan funcionando. Descomentar `exports com.vyra.dental.dao.memoria`.
7. Pruebas: `HashUtil` (verifica la correcta, rechaza la incorrecta, dos hashes de la misma clave son distintos).

**Listo cuando:** compila, las pruebas pasan, borrar `vyra.db` y arrancar crea todas las tablas nuevas.

### Fase 2 — Esqueleto visual: estilos, login y navegación
**Objetivo:** iniciar sesión y moverse por el menú, aunque las pantallas estén vacías.

1. `styles/vyra.css` con las variables y clases de la [guía §2–4](./08-guia-frontend.md#2-colores-variables-css). Cargar las fuentes en `App`.
2. Componentes `ui/`: `Icono`, `Avatar`, `Pill`, `Aviso`, `Notificacion`, `CampoFormulario`. Descomentar en `module-info.java` los `opens`/`exports` de `ui` y `controller` al crear sus primeras clases.
3. `UsuarioDAOSqlite`, `ProfesionalDAOSqlite`, `AuthService`, `Sesion`, `util.DatosIniciales.crearAdminSiNoExiste()`. Descomentar `exports com.vyra.dental.dao.impl` (el paquete queda vacío en la fase 1, al mover los DAO en memoria).
4. `LoginView.fxml` + `LoginController` ([guía §7.1](./08-guia-frontend.md#71-inicio-de-sesión--loginviewfxml--logincontroller)).
5. `MainView.fxml` + `MainController` con menú por rol, barra superior y navegación ([guía §6](./08-guia-frontend.md#6-estructura-común-mainviewfxml--maincontroller), [arquitectura §4](./05-arquitectura.md#4-navegación-entre-pantallas)). Enum `Pantalla`. Cada pantalla todavía no hecha muestra un marcador "En construcción".
6. `App.start()` abre el login.

**Listo cuando:** `admin` / `admin123` entra; una contraseña errada muestra el error; el menú cambia de pantalla y marca el ítem activo; "salir" vuelve al login; se ve como `docs/ui/Login.html` y como el marco de `docs/ui/Inicio.html`.

### Fase 3 — Pacientes
1. `PacienteDAOSqlite` (incluida la búsqueda paginada de [`03` §5.6](./03-modelo-base-datos.md#56-búsqueda-de-pacientes-pantalla-pacientes)).
2. `PacienteService`: registrar (DNI único), actualizar, dar de baja, buscar. Pruebas con `PacienteDAOMemoria`.
3. `PacientesView.fxml` + `PacientesController`, solo la pestaña Datos ([guía §7.4](./08-guia-frontend.md#74-pacientes--pacientesviewfxml--pacientescontroller)).
4. `DatosIniciales.cargarEjemplos()` con los pacientes del diseño (solo con `-Dvyra.demo=true`; el `pom.xml` lo pasa a la app: `mvn javafx:run -Dvyra.demo=true`).

**Listo cuando:** se puede registrar, buscar, editar y dar de baja; un DNI repetido o mal formado muestra el error bajo el campo; los datos siguen ahí después de cerrar y abrir la app.

### Fase 4 — Profesionales y Agenda
1. `ProfesionalesView` (CRUD simple, [guía §7.10](./08-guia-frontend.md#710-profesionales--profesionalesviewfxml--profesionalescontroller)).
2. `CitaDAOSqlite` (con la consulta de cruce de [`03` §5.2](./03-modelo-base-datos.md#52-cruce-de-horario-cu-03-rn-06)) y `CitaService`.
3. **Pruebas de `CitaService.existeCruceDeHorario()`** (obligatorias): misma hora, traslape parcial al inicio y al final, una cita que contiene a la otra, citas contiguas (09:00–09:30 y 09:30–10:00, que **no** cruzan), otro profesional, otra fecha, cita cancelada (no cruza), reprogramar la misma cita (no cruza consigo misma).
4. `AgendaView` en vista Día con el panel "Nueva cita" y la validación en vivo ([guía §7.3](./08-guia-frontend.md#73-agenda--agendaviewfxml--agendacontroller)). Después, las vistas Semana y Lista.
5. Pestaña Citas en la ficha del paciente y la columna "Última visita" de la lista de pacientes (hoy muestra "—").

**Listo cuando:** se agenda, confirma, reprograma, cancela y marca atendida; el cruce se detecta antes de guardar y los chips de horarios libres funcionan; todas las pruebas pasan.

### Fase 5 — Historia clínica y Odontograma
1. ✔ *Hecho.* `HistoriaClinicaDAOSqlite` + `HistoriaClinicaService` + pestaña Historia clínica ([guía §7.5](./08-guia-frontend.md#75-historia-clínica--pestaña-de-la-ficha--historiaclinicacontroller)).
2. `OdontogramaDAOSqlite` (consulta de estado actual de [`03` §5.1](./03-modelo-base-datos.md#51-estado-actual-del-odontograma-de-un-paciente-cu-05)) + `OdontogramaService` (pruebas: una pieza sin registros es SANA; el estado actual es el último registro; guardar no borra el historial).
3. Componente `PiezaDental` + `OdontogramaView` ([guía §7.6](./08-guia-frontend.md#76-odontograma--odontogramaviewfxml--odontogramacontroller)).

**Listo cuando:** se ve como `docs/ui/Odontograma.html`; los cambios quedan pendientes hasta "Guardar"; el historial de cada pieza muestra todos los cambios con su fecha; la recepcionista no ve estas pantallas.

### Fase 6 — Tratamientos y Pagos
1. `TratamientoDAOSqlite` (con `tratamiento_pieza` en una transacción) + `TratamientoService` + `TratamientosView` ([guía §7.7](./08-guia-frontend.md#77-tratamientos--tratamientosviewfxml--tratamientoscontroller)).
2. `PagoDAOSqlite` + `PagoService` (pruebas: el saldo se calcula bien; no se puede pagar más que el saldo; no se aceptan fechas futuras) + `PagosView` ([guía §7.8](./08-guia-frontend.md#78-pagos--pagosviewfxml--pagoscontroller)).
3. Pestaña Pagos en la ficha del paciente y enlace "Registrar tratamiento para esta pieza" desde el odontograma.

**Listo cuando:** se registra un tratamiento con varias piezas, se cobra en partes y el saldo se actualiza; la pantalla se ve como `docs/ui/Pagos.html`.

### Fase 7 — Inicio, Reportes y Usuarios
1. `InicioView` con datos reales ([guía §7.2](./08-guia-frontend.md#72-inicio-dashboard--inicioviewfxml--iniciocontroller)), incluido el aviso de citas próximas (RF-12).
2. `ReporteService` + `ReportesView` ([guía §7.9](./08-guia-frontend.md#79-reportes--reportesviewfxml--reportescontroller)).
3. `UsuarioService` + `UsuariosView` ([guía §7.11](./08-guia-frontend.md#711-usuarios-y-roles--usuariosviewfxml--usuarioscontroller)).
4. Revisar la matriz de permisos completa con un usuario de cada rol.

**Listo cuando:** Inicio coincide con `docs/ui/Inicio.html` usando datos reales; cada rol ve exactamente lo que dice la matriz.

### Fase 8 — PDF, limpieza y entrega
1. Exportación a PDF de la historia clínica y del odontograma (RF-11) con OpenPDF (agregar la dependencia `com.github.librepdf:openpdf`), dentro de un `Task`.
2. Borrar el paquete `demo/` y su `exports` en `module-info`.
3. Recorrer la lista de verificación de la [guía §10](./08-guia-frontend.md#10-lista-de-verificación-por-pantalla) en cada pantalla.
4. Empaquetar con `jpackage` (instalador para Windows).
5. Completar [`06-manual-usuario.md`](./06-manual-usuario.md) con capturas reales y [`07-memoria-descriptiva.md`](./07-memoria-descriptiva.md).

## 4. Cómo trabajar cada fase con Claude Code

En la raíz del repositorio hay un [`CLAUDE.md`](../CLAUDE.md) que Claude Code lee automáticamente, con las reglas del proyecto. Para cada fase, basta un pedido corto que apunte a este plan, por ejemplo:

> Implementa la **Fase 1** de `docs/09-plan-implementacion.md`. Sigue `docs/03` y `docs/04`. Al terminar, ejecuta `mvn test` y muéstrame qué cambió.

> Implementa la pantalla de **Pacientes** (Fase 3). Tiene que verse como `docs/ui/Pacientes.html` y seguir la sección 7.4 de `docs/08-guia-frontend.md`.

Recomendaciones:
- Una fase por conversación (o una pantalla, si la fase es grande).
- Revisar el resultado con `mvn javafx:run` antes de pasar a la siguiente.
- Si algo del diseño no funciona bien en JavaFX, decidirlo y **actualizar la guía** para que el documento siga siendo la referencia.

## 5. Definición de "terminado"

Una tarea está terminada cuando:
1. Compila y `mvn test` pasa.
2. Cumple los criterios "Listo cuando" de su fase.
3. Respeta las capas (sin SQL en controladores, sin JavaFX en servicios).
4. La pantalla pasa la lista de verificación de la guía de frontend.
5. Tiene su commit con un mensaje descriptivo.
6. Si cambió algo del diseño o del modelo, el documento correspondiente se actualizó en el mismo commit.
