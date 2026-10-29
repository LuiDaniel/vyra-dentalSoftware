package com.vyra.dental.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Rol {
    ADMIN("Administrador"),
    RECEPCION("Recepción"),
    ODONTOLOGO("Odontólogo");

    /** Texto que ve el usuario. */
    private final String etiqueta;
}
