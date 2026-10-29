package com.vyra.dental.service;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Tratamiento;

/**
 * Un tratamiento con lo que las pantallas necesitan mostrar: la cita (de ahí salen el
 * paciente y el profesional) y lo pagado. El saldo no se guarda: es costo − pagado (RN-12).
 */
public record TratamientoResumen(Tratamiento tratamiento, Cita cita, double pagado) {

    public Paciente paciente() {
        return cita.getPaciente();
    }

    /** Nunca negativo, aunque la suma de pagos tenga error de redondeo. */
    public double saldo() {
        return PagoService.aCentimos(tratamiento.getCosto() - pagado) / 100.0;
    }

    public boolean estaPagado() {
        return saldo() <= 0;
    }
}
