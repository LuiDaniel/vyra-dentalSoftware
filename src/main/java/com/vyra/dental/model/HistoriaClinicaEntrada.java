package com.vyra.dental.model;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistoriaClinicaEntrada {
    private int idEntrada;
    private int idPaciente;

    /** Cita en la que se registró; null si se registró fuera de una cita. */
    private Integer idCita;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    private String observaciones;
    private String diagnostico;
}
