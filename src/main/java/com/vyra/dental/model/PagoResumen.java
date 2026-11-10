package com.vyra.dental.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un pago con lo que muestra la tabla "Pagos recientes": el concepto (la
 * descripción del tratamiento) y el paciente. No es una tabla: lo arma PagoDAO
 * con un JOIN.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagoResumen {
    private Pago pago;
    private String concepto;
    /** Id, nombres, apellidos y DNI. */
    private Paciente paciente;
}
