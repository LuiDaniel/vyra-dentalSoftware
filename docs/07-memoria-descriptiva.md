# Vyra — Memoria Descriptiva
### (documento pensado para el eventual registro de derecho de autor en INDECOPI)

**Versión:** 0.1 (plantilla — completar al finalizar el desarrollo)
**Fecha:** 2026-09-17

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
| Daniel Ticona | | Líder de proyecto, base de datos, integración |
| | | |
| | | |
| | | |

## 3. Descripción general de la obra
*(Se puede completar ya, tomando el contenido de la sección 1 de `01-requerimientos.md`)*

Vyra es un sistema de escritorio desarrollado en JavaFX para la gestión interna de citas y expedientes de un consultorio dental...

## 4. Funcionalidades principales
*(Lista resumida de los RF de `01-requerimientos.md` — se puede completar ya)*

## 5. Tecnologías utilizadas
- Lenguaje: Java 21
- Interfaz gráfica: JavaFX
- Base de datos: SQLite (JDBC)
- Gestor de dependencias: Maven
- Control de versiones: Git / GitHub

## 6. Alcance y funcionalidades no incluidas
*(Tomado directamente de la sección 2.2 de `01-requerimientos.md`)*

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

*Este es el último documento de la serie planificada. A medida que avancen con el código, vuelvan a `06-manual-usuario.md` y a esta memoria para completarlos con capturas y datos reales.*
