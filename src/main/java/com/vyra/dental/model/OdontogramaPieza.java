package com.vyra.dental.model;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un cambio de estado de una pieza dental. Nunca se sobrescribe (RN-09):
 * el estado actual de la pieza es su registro más reciente.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OdontogramaPieza {
    private int idRegistro;
    private int idPaciente;

    // Rango general de la notación FDI (RN-10); la base de datos rechaza además 19, 20, 29...
    @Min(value = 11, message = "Pieza dental inválida (notación FDI de 11 a 48)")
    @Max(value = 48, message = "Pieza dental inválida (notación FDI de 11 a 48)")
    private int numeroPieza;

    @NotNull(message = "Debes seleccionar un estado")
    private EstadoPieza estado;

    private LocalDate fechaRegistro;
    private String observacion;
}
