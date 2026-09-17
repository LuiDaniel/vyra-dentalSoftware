# Vyra — Diagrama de Clases

**Versión:** 0.1
**Fecha:** 2026-09-17

Este diagrama traduce el modelo de base de datos ([`03-modelo-base-datos.md`](./03-modelo-base-datos.md)) a clases Java, organizadas en capas (modelo, DAO, servicio) según RNF-06.

```mermaid
classDiagram
    class Paciente {
        -int idPaciente
        -String nombres
        -String apellidos
        -String dni
        -LocalDate fechaNacimiento
        -String telefono
        -String direccion
        -String contactoEmergencia
        -String alergias
        +getNombreCompleto() String
    }

    class Profesional {
        -int idProfesional
        -String nombres
        -String apellidos
        -String especialidad
        -String horarioAtencion
    }

    class Usuario {
        -int idUsuario
        -String nombreUsuario
        -String contrasenaHash
        -Rol rol
        +autenticar(String intento) boolean
    }

    class Rol {
        <<enumeration>>
        ADMIN
        RECEPCION
        ODONTOLOGO
    }

    class Cita {
        -int idCita
        -Paciente paciente
        -Profesional profesional
        -LocalDate fecha
        -LocalTime horaInicio
        -LocalTime horaFin
        -String motivo
        -EstadoCita estado
    }

    class EstadoCita {
        <<enumeration>>
        PENDIENTE
        CONFIRMADA
        ATENDIDA
        CANCELADA
    }

    class HistoriaClinicaEntrada {
        -int idEntrada
        -LocalDate fecha
        -String observaciones
        -String diagnostico
    }

    class OdontogramaPieza {
        -int numeroPieza
        -EstadoPieza estado
        -LocalDate fechaRegistro
    }

    class EstadoPieza {
        <<enumeration>>
        SANA
        CARIADA
        OBTURADA
        EXTRAIDA
        EN_TRATAMIENTO
    }

    class Tratamiento {
        -int idTratamiento
        -String descripcion
        -double costo
        -EstadoTratamiento estado
        -List~Integer~ piezasDentales
    }

    class Pago {
        -int idPago
        -double monto
        -LocalDate fechaPago
        -String metodoPago
    }

    class PacienteDAO {
        <<interface>>
        +guardar(Paciente) void
        +buscarPorId(int) Paciente
        +buscarPorDni(String) Paciente
        +listarTodos() List~Paciente~
        +actualizar(Paciente) void
    }

    class CitaDAO {
        <<interface>>
        +guardar(Cita) void
        +listarPorFecha(LocalDate) List~Cita~
        +listarPorProfesional(int, LocalDate) List~Cita~
        +actualizarEstado(int, EstadoCita) void
    }

    class TratamientoDAO {
        <<interface>>
        +guardar(Tratamiento) void
        +listarPorPaciente(int) List~Tratamiento~
    }

    class CitaService {
        -CitaDAO citaDAO
        +agendarCita(Cita) void
        +existeCruceDeHorario(Cita) boolean
        +cancelarCita(int) void
    }

    class ReporteService {
        -CitaDAO citaDAO
        -TratamientoDAO tratamientoDAO
        +citasDelDia(LocalDate) List~Cita~
        +ingresosPorPeriodo(LocalDate, LocalDate) double
    }

    Paciente "1" --> "0..*" Cita : agenda
    Profesional "1" --> "0..*" Cita : atiende
    Paciente "1" --> "0..*" HistoriaClinicaEntrada
    Paciente "1" --> "0..*" OdontogramaPieza
    Cita "1" --> "0..*" Tratamiento
    Tratamiento "1" --> "0..*" Pago
    Usuario "1" --> "0..1" Rol
    Cita "1" --> "1" EstadoCita
    OdontogramaPieza "1" --> "1" EstadoPieza

    PacienteDAO ..> Paciente : gestiona
    CitaDAO ..> Cita : gestiona
    TratamientoDAO ..> Tratamiento : gestiona
    CitaService --> CitaDAO
    ReporteService --> CitaDAO
    ReporteService --> TratamientoDAO
```

**Cómo leer esto para programarlo:**
- Las clases sin `<<interface>>` van en el paquete `com.vyra.model`.
- Las que terminan en `DAO` son interfaces en `com.vyra.dao`, con su implementación concreta (ej. `PacienteDAOSqlite implements PacienteDAO`) en el mismo paquete o en `com.vyra.dao.impl`.
- Las que terminan en `Service` van en `com.vyra.service` — ahí vive la lógica de negocio (como la validación de cruce de horario de CU-03), separada de la base de datos y de la interfaz.
- Los controladores de JavaFX (uno por pantalla FXML) usan estos servicios; no hablan directo con los DAO.

---

*Siguiente documento: [`05-arquitectura.md`](./05-arquitectura.md) — cómo se conectan estas capas entre sí.*
