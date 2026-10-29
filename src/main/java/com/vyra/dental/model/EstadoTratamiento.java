package com.vyra.dental.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EstadoTratamiento {
    EN_CURSO("En curso", "pill-pendiente"),
    FINALIZADO("Finalizado", "pill-atendida");

    /** Texto que ve el usuario. */
    private final String etiqueta;
    /** Clase de vyra.css para la píldora de estado. */
    private final String claseCss;
}
