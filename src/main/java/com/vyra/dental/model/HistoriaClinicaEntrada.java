package com.vyra.dental.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoriaClinicaEntrada {

    private int idEntrada;
    private Paciente paciente;
    private Cita cita;
    private LocalDate fecha;
    private String observaciones;
    private String diagnostico;
}