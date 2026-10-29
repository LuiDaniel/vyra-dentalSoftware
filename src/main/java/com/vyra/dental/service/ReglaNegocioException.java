package com.vyra.dental.service;

/**
 * Se lanza cuando no se cumple una regla de negocio (RN-xx). El mensaje
 * está listo para mostrarse al usuario, por ejemplo:
 * "El Dr. Salinas ya tiene una cita de 09:00 a 10:00".
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
