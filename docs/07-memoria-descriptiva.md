# Vyra — Memoria Descriptiva
### (documento pensado para el eventual registro de derecho de autor en INDECOPI)

**Versión:** 1.0 (desarrollo terminado; faltan los datos personales de los autores y del trámite)
**Fecha:** 2026-11-10

> Nota: este documento resume lo definido en `01-requerimientos.md` a `05-arquitectura.md`. Lo que queda entre *(paréntesis y cursiva)* lo completan los autores al presentar la solicitud.

## 1. Datos de la obra
- **Nombre de la obra:** Vyra
- **Tipo de obra:** Programa de ordenador (obra literaria, según la clasificación de INDECOPI)
- **Fecha de creación (inicio del desarrollo):** 15 de septiembre de 2026 (primer commit del repositorio)
- **Fecha de terminación:** 10 de noviembre de 2026 (versión 1.0, fin de la fase 8 del plan de implementación)
- **Versión:** 1.0
- **¿Publicada o inédita?:** *(completar según corresponda al momento del registro; mientras solo se use en el curso y en el consultorio de prueba, es inédita)*

## 2. Autores
*(Completar con nombre completo y DNI de cada uno de los 4 integrantes — INDECOPI permite y pide listar a todos los coautores reales de la obra, no solo a quien presenta el trámite)*

| Nombre completo | DNI | Aporte al desarrollo |
|---|---|---|
| Daniel | | |
| Evelyn | | |
| Smith | | |
| Johan | | |

## 3. Descripción general de la obra
*(Se puede completar ya, tomando el contenido de la sección 1 de `01-requerimientos.md`)*

Vyra es un sistema de escritorio desarrollado en JavaFX para la gestión interna de citas y expedientes de un consultorio dental. Lo usan el personal de recepción, los odontólogos y el administrador del consultorio, cada uno con un rol que define qué pantallas puede ver y qué acciones puede realizar.

El sistema reemplaza el registro manual en papel o en hojas de cálculo: centraliza los datos de los pacientes, organiza la agenda de los odontólogos evitando cruces de horario, conserva el historial clínico y el odontograma de cada paciente con fecha de cada cambio, y registra los tratamientos y los pagos asociados.

## 4. Funcionalidades principales
- Registro, búsqueda, edición y baja lógica de pacientes.
- Agenda de citas por odontólogo, con validación automática de cruces de horario y sugerencia de horarios libres.
- Historia clínica por paciente, con entradas fechadas que no se sobrescriben.
- Odontograma interactivo en notación FDI, con el historial de cambios de cada pieza.
- Registro de tratamientos asociados a citas y piezas dentales, con su costo y estado.
- Registro de pagos (efectivo, Yape/Plin, tarjeta, transferencia) y cálculo del saldo pendiente.
- Control de acceso con usuario, contraseña cifrada (PBKDF2) y tres roles: Administrador, Recepción y Odontólogo.
- Pantalla de inicio con las citas de la próxima hora, indicadores del día y búsqueda rápida de pacientes.
- Reportes de citas (con porcentaje de asistencia), historial de tratamientos de un paciente e ingresos por periodo y por método de pago.
- Exportación a PDF de la historia clínica, del odontograma, de los reportes y de los pagos del periodo.
- Gestión de usuarios: alta, desactivación (sin borrar) y restablecimiento de contraseñas.

## 5. Tecnologías utilizadas
- Lenguaje: Java 21
- Interfaz gráfica: JavaFX 21 (FXML + CSS)
- Base de datos: SQLite (JDBC, driver xerial)
- Validación: Jakarta Bean Validation (Hibernate Validator)
- Generación de PDF: OpenPDF
- Reducción de código repetitivo en el modelo: Lombok
- Pruebas automáticas: JUnit 5
- Gestor de dependencias: Maven
- Empaquetado para Windows: jpackage (incluye su propio entorno de Java)
- Control de versiones: Git / GitHub

## 6. Alcance y funcionalidades no incluidas
No forman parte de esta versión:
- Agendamiento de citas por parte del paciente (portal web o app móvil).
- Facturación electrónica integrada con SUNAT.
- Recordatorios automáticos por SMS.
- Telemedicina o videoconsulta.

## 7. Código fuente
Estado del código en la versión 1.0: 104 archivos Java (unas 13 600 líneas) organizados en capas (`model`, `dao`, `service`, `controller`, `ui`, `util`; ver [`05-arquitectura.md`](./05-arquitectura.md)), 13 vistas FXML, una hoja de estilos y 23 archivos de pruebas automáticas.

INDECOPI pide adjuntar el código fuente completo (comprimido en ZIP) junto con la solicitud. Preparar:
- Exportación completa del repositorio (sin la carpeta `.git` si se prefiere, aunque no es obligatorio quitarla).
- Verificar que no incluya credenciales ni datos reales de pacientes (usar datos de prueba/ficticios en cualquier base de datos de ejemplo que se adjunte). El repositorio no incluye `vyra.db` (está en `.gitignore`) y los datos de ejemplo del código son ficticios.
- No incluir la carpeta `target/` (se genera al compilar).

## 8. Documentación técnica anexa
- Manual de usuario ([`06-manual-usuario.md`](./06-manual-usuario.md)), con capturas reales de cada pantalla
- Este documento de memoria descriptiva
- Capturas de pantalla de las funcionalidades principales: carpeta [`docs/manual/`](./manual)

## 9. Trámite (referencial, verificar montos y requisitos vigentes en la web de INDECOPI antes de pagar)
- **Entidad:** Dirección de Derecho de Autor (DDA) - INDECOPI
- **Modalidad:** Mesa de Partes Virtual
- **Documentos a presentar:** formulario de solicitud, código fuente (ZIP), esta memoria descriptiva / manual de usuario, DNI de cada autor, comprobante de pago
- **Costo aproximado:** entre S/195 y S/400 según clasificación (verificar el monto exacto vigente en el TUPA de INDECOPI)
- **Plazo estimado:** 30 a 45 días hábiles

---

*Pendiente antes de presentar: la tabla de autores (sección 2) y verificar los datos del trámite (sección 9) en la web de INDECOPI.*
