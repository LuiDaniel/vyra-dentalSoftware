package com.vyra.dental.service;

import com.vyra.dental.model.Pago;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Tratamiento;

/** Un pago con el tratamiento que cubre y el paciente que lo hizo, para las tablas. */
public record PagoDetalle(Pago pago, Tratamiento tratamiento, Paciente paciente) {
}
