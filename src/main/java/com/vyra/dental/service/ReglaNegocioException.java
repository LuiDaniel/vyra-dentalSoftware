package com.vyra.dental.service;

import java.util.Optional;

/**
 * Se lanza cuando no se cumple una regla de negocio (RN-xx). El mensaje
 * está listo para mostrarse al usuario, por ejemplo:
 * "El Dr. Salinas ya tiene una cita de 09:00 a 10:00".
 * Si el error corresponde a un campo del formulario (ej. "dni"), se indica
 * para mostrarlo bajo ese campo.
 */
public class ReglaNegocioException extends RuntimeException {

    private final String campo;

    public ReglaNegocioException(String mensaje) {
        this(null, mensaje);
    }

    public ReglaNegocioException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public Optional<String> getCampo() {
        return Optional.ofNullable(campo);
    }
}
