package com.vyra.dental.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MetodoPago {
    EFECTIVO("Efectivo", "pill-efectivo"),
    YAPE_PLIN("Yape / Plin", "pill-yape-plin"),
    TARJETA("Tarjeta", "pill-tarjeta"),
    TRANSFERENCIA("Transferencia", "pill-transferencia");

    /** Texto que ve el usuario. */
    private final String etiqueta;
    /** Clase de vyra.css para la píldora del método (docs/08-guia-frontend.md §7.8). */
    private final String claseCss;
}
