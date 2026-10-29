package com.vyra.dental.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
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
}
