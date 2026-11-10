package com.vyra.dental.controller;

/**
 * Parámetros sin datos para abrir una pantalla de una forma concreta, por
 * ejemplo desde los botones de Inicio:
 * {@code navegar(Pantalla.PACIENTES, Solicitud.NUEVO)}.
 */
public enum Solicitud {
    /** Abrir con el formulario de registro vacío ("Nuevo paciente", "Nueva cita"). */
    NUEVO
}
