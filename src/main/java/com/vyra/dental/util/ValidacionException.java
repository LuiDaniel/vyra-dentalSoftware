package com.vyra.dental.util;

import java.util.Collections;
import java.util.Map;

/**
 * Error de validación de Jakarta con el mensaje de cada campo, para que el
 * formulario pueda mostrarlo bajo el campo que falló. Hereda de
 * IllegalArgumentException: quien solo necesita el texto completo puede
 * seguir usando getMessage().
 */
public class ValidacionException extends IllegalArgumentException {

    private final Map<String, String> errores;

    public ValidacionException(String mensaje, Map<String, String> errores) {
        super(mensaje);
        this.errores = Collections.unmodifiableMap(errores);
    }

    /** Nombre del campo del modelo ("dni", "telefono"...) → mensaje. */
    public Map<String, String> getErrores() {
        return errores;
    }
}
