package com.vyra.dental.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EstadoCita {
    PENDIENTE("Pendiente", "pill-pendiente"),
    CONFIRMADA("Confirmada", "pill-confirmada"),
    ATENDIDA("Atendida", "pill-atendida"),
    CANCELADA("Cancelada", "pill-cancelada");

    /** Texto que ve el usuario. */
    private final String etiqueta;
    /** Clase de vyra.css para la píldora de estado. */
    private final String claseCss;
}
