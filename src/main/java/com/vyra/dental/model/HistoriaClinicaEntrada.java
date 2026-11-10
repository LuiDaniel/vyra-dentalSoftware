package com.vyra.dental.model;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una entrada de la historia clínica. Nunca se edita ni se borra (RN-09):
 * cada visita agrega una entrada nueva.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistoriaClinicaEntrada {
    private int idEntrada;
    private int idPaciente;

    /** Cita en la que se registró; null si se registró fuera de una cita. */
    private Integer idCita;

    /** Profesional que la registró; null si el usuario no tenía profesional asociado. */
    private Integer idProfesional;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    private String observaciones;
    private String diagnostico;
}
