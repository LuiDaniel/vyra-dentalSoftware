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

    /** Tratamiento que se paga; null si el pago es de la consulta de una cita. */
    private Integer idTratamiento;

    @Positive(message = "El monto debe ser mayor a 0")
    private double monto;

    @NotNull(message = "La fecha de pago es obligatoria")
    @PastOrPresent(message = "La fecha de pago no puede ser futura")
    private LocalDate fechaPago;

    @NotNull(message = "Debes seleccionar un método de pago")
    private MetodoPago metodoPago;

    /** Opcional: número de operación de Yape/Plin, tarjeta, transferencia o Mercado Pago. */
    private String numeroOperacion;

    /** Cita cuya consulta se paga (RN-16); null si el pago es de un tratamiento. */
    private Integer idCita;

    /** Pago de un tratamiento. */
    public Pago(int idPago, int idTratamiento, double monto, LocalDate fechaPago, MetodoPago metodoPago,
                String numeroOperacion) {
        this(idPago, idTratamiento, monto, fechaPago, metodoPago, numeroOperacion, null);
    }

    /** Pago de la consulta de una cita. */
    public static Pago deConsulta(int idCita, double monto, LocalDate fechaPago, MetodoPago metodoPago,
                                  String numeroOperacion) {
        return new Pago(0, null, monto, fechaPago, metodoPago, numeroOperacion, idCita);
    }

    public boolean esDeConsulta() {
        return idCita != null;
    }
}
