package com.vyra.dental.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OdontogramaPieza {
    private int numeroPieza;
    private EstadoPieza estado;
    private LocalDate fechaRegistro;
}
