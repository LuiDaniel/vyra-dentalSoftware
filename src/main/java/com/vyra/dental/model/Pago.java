package com.vyra.dental.model;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pago {
    private int idPago;
    private int idTratamiento;

    @Positive(message = "El monto debe ser mayor a 0")
    private double monto;

    @NotNull(message = "La fecha de pago es obligatoria")
    @PastOrPresent(message = "La fecha de pago no puede ser futura")
    private LocalDate fechaPago;

    @NotNull(message = "Debes seleccionar un método de pago")
    private MetodoPago metodoPago;

    /** Opcional: número de operación de Yape/Plin, tarjeta o transferencia. */
    private String numeroOperacion;
}
