# Vyra — Memoria Descriptiva
### (documento pensado para el eventual registro de derecho de autor en INDECOPI)

**Versión:** 0.2 (secciones 3 a 6 completadas; el resto se completa al finalizar el desarrollo)
**Fecha:** 2026-10-29

> Nota: este documento reutiliza y resume lo ya definido en `01-requerimientos.md` a `05-arquitectura.md`. Se deja aquí como plantilla para no partir de cero cuando llegue el momento de registrar el software; varias secciones ya se pueden completar ahora, otras solo cuando el sistema esté terminado.

## 1. Datos de la obra
- **Nombre de la obra:** Vyra
- **Tipo de obra:** Programa de ordenador (obra literaria, según la clasificación de INDECOPI)
- **Fecha de creación (inicio del desarrollo):** *(completar)*
- **Fecha de terminación:** *(completar)*
- **¿Publicada o inédita?:** *(completar según corresponda al momento del registro)*

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
- Reportes de citas del día y de la semana, historial de un paciente e ingresos por periodo.
- Exportación a PDF de la historia clínica y del odontograma.

## 5. Tecnologías utilizadas
- Lenguaje: Java 21
- Interfaz gráfica: JavaFX 21 (FXML + CSS)
- Base de datos: SQLite (JDBC, driver xerial)
- Validación: Jakarta Bean Validation (Hibernate Validator)
- Gestor de dependencias: Maven
- Control de versiones: Git / GitHub

## 6. Alcance y funcionalidades no incluidas
No forman parte de esta versión:
- Agendamiento de citas por parte del paciente (portal web o app móvil).
- Facturación electrónica integrada con SUNAT.
- Recordatorios automáticos por SMS.
- Telemedicina o videoconsulta.

## 7. Código fuente
INDECOPI pide adjuntar el código fuente completo (comprimido en ZIP) junto con la solicitud. Preparar:
- Exportación completa del repositorio (sin la carpeta `.git` si se prefiere, aunque no es obligatorio quitarla).
- Verificar que no incluya credenciales ni datos reales de pacientes (usar datos de prueba/ficticios en cualquier base de datos de ejemplo que se adjunte).

## 8. Documentación técnica anexa
- Manual de usuario ([`06-manual-usuario.md`](./06-manual-usuario.md), ya con capturas reales)
- Este documento de memoria descriptiva
- Capturas de pantalla de las funcionalidades principales

## 9. Trámite (referencial, verificar montos y requisitos vigentes en la web de INDECOPI antes de pagar)
- **Entidad:** Dirección de Derecho de Autor (DDA) - INDECOPI
- **Modalidad:** Mesa de Partes Virtual
- **Documentos a presentar:** formulario de solicitud, código fuente (ZIP), esta memoria descriptiva / manual de usuario, DNI de cada autor, comprobante de pago
- **Costo aproximado:** entre S/195 y S/400 según clasificación (verificar el monto exacto vigente en el TUPA de INDECOPI)
- **Plazo estimado:** 30 a 45 días hábiles

---

*A medida que avancen con el código, vuelvan a `06-manual-usuario.md` y a esta memoria para completarlos con capturas y datos reales. Para programar, los documentos de trabajo son [`08-guia-frontend.md`](./08-guia-frontend.md) y [`09-plan-implementacion.md`](./09-plan-implementacion.md).*
