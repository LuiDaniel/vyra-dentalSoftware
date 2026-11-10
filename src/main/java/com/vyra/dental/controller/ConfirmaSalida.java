package com.vyra.dental.controller;

/**
 * La implementa el controlador de una pantalla que puede tener cambios sin
 * guardar (el odontograma). MainController la consulta antes de cambiar de
 * pantalla o cerrar sesión.
 */
public interface ConfirmaSalida {

    /** true si se puede salir: no hay cambios pendientes o el usuario acepta perderlos. */
    boolean confirmarSalida();
}
