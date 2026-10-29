# Vyra — instrucciones para Claude Code

Aplicación de escritorio JavaFX para gestionar un consultorio dental (pacientes, agenda, historia clínica, odontograma, tratamientos y pagos). Proyecto universitario; el equipo escribe en español.

## Antes de programar, lee

1. `docs/09-plan-implementacion.md`: en qué fase está el proyecto y qué toca ahora. **Trabaja una fase (o una pantalla) a la vez.**
2. `docs/08-guia-frontend.md`: cómo se ve y cómo se comporta cada pantalla. El diseño de referencia está en `docs/ui/*.html`; ábrelos para comparar.
3. `docs/03-modelo-base-datos.md` y `docs/04-diagrama-clases.md`: tablas, clases, DAO y servicios esperados.
4. `docs/01-requerimientos.md` §6: reglas de negocio (RN-01 a RN-15) y dónde se valida cada una.

Si el código y los documentos no coinciden, sigue los documentos y avisa de la diferencia. Si una decisión cambia el diseño o el modelo, actualiza el documento en el mismo cambio.

## Comandos

- Ejecutar: `mvn javafx:run`
- Compilar: `mvn compile`
- Pruebas: `mvn test`
- La base de datos `vyra.db` se crea en la carpeta de ejecución. Si cambias `schema.sql`, bórrala (está en `.gitignore`).

## Stack

Java 21, JavaFX 21 (FXML + CSS), SQLite con JDBC (xerial), Lombok, Jakarta Validation (Hibernate Validator), Maven, módulos Java (`module-info.java`). Pruebas con JUnit 5.

## Reglas de arquitectura

- Paquete raíz `com.vyra.dental`: `model`, `dao` (interfaces), `dao/impl` (`XxxDAOSqlite`), `dao/memoria` (`XxxDAOMemoria`, para pruebas), `service`, `controller`, `ui`, `util`.
- Capas estrictas: Controller → Service → DAO → SQLite. **Sin SQL fuera de los DAO. Sin JavaFX fuera de `controller/` y `ui/`.** Los controladores no llaman a los DAO.
- Los servicios reciben sus DAO por constructor y lanzan `ReglaNegocioException` con mensajes listos para mostrar al usuario.
- Todos los DAO usan `ConexionBD.obtenerConexion()` (conexión compartida: no la cierres) y `PreparedStatement` con `?`.
- Nombres de DAO: `crear`, `buscarPorId` (`Optional`), `listarTodos`, `actualizar` (`boolean`), `eliminar`, como en el código existente.
- Vistas: `resources/com/vyra/views/XxxView.fxml` + `controller/XxxController`. Estilos solo en `resources/com/vyra/styles/vyra.css` con las variables `-vy-*`; no pongas colores a mano en el FXML.
- Nuevos paquetes cargados por FXML necesitan `opens ... to javafx.fxml` en `module-info.java`.

## Convenciones

- Identificadores en español sin tildes (`agendarCita`, `contrasenaHash`); textos visibles en español con tildes; dinero como `S/ 1,250.00`.
- Lombok en el modelo (`@Data @NoArgsConstructor @AllArgsConstructor`) y validaciones Jakarta con mensajes en español.
- Contraseñas con `HashUtil` (PBKDF2). Nunca en texto plano.
- Commits en español, con el módulo como prefijo: `agenda: valida cruce de horario`.
- Datos de prueba siempre ficticios.

## Al terminar una tarea

- `mvn compile` y `mvn test` deben pasar.
- Si tocaste una pantalla, revisa la lista de verificación de `docs/08-guia-frontend.md` §10.
- Resume qué cambió y qué falta de la fase.
