package com.vyra.dental.controller;

/**
 * La implementa el controlador de una pantalla con cambios sin guardar. El marco
 * la consulta antes de cambiar de pantalla o cerrar sesión; si devuelve false, no sale.
 */
public interface ConfirmaSalida {

    /** Si hay cambios pendientes, pregunta al usuario y devuelve su respuesta. */
    boolean puedeSalir();
}
