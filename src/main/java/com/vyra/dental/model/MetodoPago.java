package com.vyra.dental.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MetodoPago {
    EFECTIVO("Efectivo", "pill-atendida"),
    YAPE_PLIN("Yape / Plin", "pill-yape"),
    TARJETA("Tarjeta", "pill-confirmada"),
    TRANSFERENCIA("Transferencia", "pill-neutra");

    /** Texto que ve el usuario. */
    private final String etiqueta;
    /** Clase de vyra.css para la píldora del método. */
    private final String claseCss;
}
