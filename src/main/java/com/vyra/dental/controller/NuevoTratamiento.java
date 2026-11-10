package com.vyra.dental.controller;

import com.vyra.dental.model.Paciente;

/**
 * Parámetro para abrir Tratamientos con el formulario "Nuevo tratamiento" ya
 * dirigido a un paciente y una pieza: el enlace "Registrar tratamiento para
 * esta pieza" del odontograma.
 */
public record NuevoTratamiento(Paciente paciente, int pieza) {
}
