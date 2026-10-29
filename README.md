# vyra-dentalSoftware

Sistema de gestión de citas y expedientes para un consultorio dental. Aplicación de escritorio en JavaFX + base de datos relacional, desarrollada como proyecto universitario (UPeU - Ingeniería de Sistemas).

## Documentación

Toda la documentación del proyecto vive en la carpeta [`docs/`](./docs), en orden:

1. [Requerimientos](./docs/01-requerimientos.md): descripción, alcance, actores, requerimientos funcionales y no funcionales, reglas de negocio.
2. [Casos de uso](./docs/02-casos-de-uso.md): flujos de cada caso de uso y matriz de permisos por rol.
3. [Modelo de base de datos](./docs/03-modelo-base-datos.md): diagrama entidad-relación, diccionario de datos, script SQL y consultas clave.
4. [Diagrama de clases](./docs/04-diagrama-clases.md): modelo, DAO y servicios.
5. [Arquitectura](./docs/05-arquitectura.md): capas, paquetes, navegación, sesión y cómo ejecutar.
6. [Manual de usuario](./docs/06-manual-usuario.md) *(plantilla; se completa al final)*.
7. [Memoria descriptiva](./docs/07-memoria-descriptiva.md) *(para el registro en INDECOPI)*.
8. [Guía de frontend](./docs/08-guia-frontend.md): colores, tipografía, íconos y especificación de cada pantalla.
9. [Plan de implementación](./docs/09-plan-implementacion.md): fases de programación con criterios de aceptación.

**Diseño de la interfaz:** abre [`docs/ui/Inicio.html`](./docs/ui) en el navegador.

## Cómo ejecutar

```bash
mvn javafx:run
```

Requiere JDK 21 y Maven. La base de datos `vyra.db` se crea sola la primera vez.

## Equipo

- Daniel
- Evelyn
- Smith
- Johan

## Tecnologías

- Java 21 + JavaFX 21 (FXML + CSS)
- Maven
- SQLite (base de datos)
- Lombok y Jakarta Bean Validation
