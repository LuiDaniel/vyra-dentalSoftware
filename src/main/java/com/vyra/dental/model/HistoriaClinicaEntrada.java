package com.vyra.dental.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistoriaClinicaEntrada {
    private int idEntrada;
    private LocalDate fecha;
    private String observaciones;
    private String diagnostico;
}
