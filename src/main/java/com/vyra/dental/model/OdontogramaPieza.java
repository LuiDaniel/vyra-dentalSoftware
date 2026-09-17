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
public class OdontogramaPieza {

    private int idRegistro;
    private Paciente paciente;
    private int numeroPieza;
    private EstadoPieza estado;
    private LocalDate fechaRegistro;
}