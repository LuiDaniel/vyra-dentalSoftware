package com.vyra.dental.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un tratamiento con lo que muestran las listas: el paciente (a través de su
 * cita), el profesional de la cita y lo pagado hasta hoy. No es una tabla: lo
 * arma TratamientoDAO con un JOIN. El saldo no se guarda (CU-10): se calcula.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TratamientoResumen {
    private Tratamiento tratamiento;
    /** Id, nombres, apellidos y DNI. */
    private Paciente paciente;
    /** Id, nombres y apellidos del profesional de la cita. */
    private Profesional profesional;
    private double pagado;

    /** Costo − pagado, redondeado a céntimos. */
    public double getSaldo() {
        return Math.round((tratamiento.getCosto() - pagado) * 100) / 100.0;
    }

    /** Fracción pagada, de 0 a 1 (para la barra de progreso). */
    public double getAvance() {
        return tratamiento.getCosto() <= 0 ? 0 : Math.min(1, pagado / tratamiento.getCosto());
    }
}
