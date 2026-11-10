package com.vyra.dental.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Rol {
    ADMIN("Administrador", "pill-rol-admin"),
    RECEPCION("Recepción", "pill-rol-recepcion"),
    ODONTOLOGO("Odontólogo", "pill-rol-odontologo");

    /** Texto que ve el usuario. */
    private final String etiqueta;
    /** Clase de vyra.css para la píldora del rol (pantalla Usuarios). */
    private final String claseCss;
}
