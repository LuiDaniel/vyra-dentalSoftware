package com.vyra.dental.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Cita {
    private int idCita;

    @NotNull(message = "Debes seleccionar un paciente")
    private Paciente paciente;

    @NotNull(message = "Debes seleccionar un profesional")
    private Profesional profesional;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;

    private String motivo;

    @NotNull(message = "Debes seleccionar un estado")
    private EstadoCita estado;

    /**
     * Lo que cuesta la consulta (RN-16). Se copia del profesional al agendar y
     * se cobra antes de atender; 0 = consulta sin costo (por ejemplo, un control).
     */
    @PositiveOrZero(message = "El costo de la consulta no puede ser negativo")
    private double costoConsulta;

    /** Sin costo de consulta (las citas de antes de la fase 9 y las pruebas). */
    public Cita(int idCita, Paciente paciente, Profesional profesional, LocalDate fecha, LocalTime horaInicio,
                LocalTime horaFin, String motivo, EstadoCita estado) {
        this(idCita, paciente, profesional, fecha, horaInicio, horaFin, motivo, estado, 0);
    }
}
