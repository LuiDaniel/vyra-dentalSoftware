package com.vyra.dental.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pago {
    private int idPago;
    private double monto;
    private LocalDate fechaPago;
    private String metodoPago;
}
