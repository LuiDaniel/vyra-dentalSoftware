package com.vyra.dental.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EstadoPieza {
    SANA("Sana", "", "pieza-sana"),
    CARIADA("Cariada", "C", "pieza-cariada"),
    OBTURADA("Obturada", "O", "pieza-obturada"),
    EXTRAIDA("Extraída", "X", "pieza-extraida"),
    EN_TRATAMIENTO("En tratamiento", "T", "pieza-en-tratamiento");

    /** Texto que ve el usuario. */
    private final String etiqueta;
    /** Letra que se dibuja sobre la pieza en el odontograma. */
    private final String codigo;
    /** Clase de vyra.css con los colores de la pieza (relleno y borde). */
    private final String claseCss;
}
