package com.vyra.dental.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MetodoPago {
    EFECTIVO("Efectivo"),
    YAPE_PLIN("Yape / Plin"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia");

    /** Texto que ve el usuario. */
    private final String etiqueta;
}
