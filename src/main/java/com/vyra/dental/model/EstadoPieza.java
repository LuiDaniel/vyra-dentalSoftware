package com.vyra.dental.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EstadoPieza {
    SANA("Sana", ""),
    CARIADA("Cariada", "C"),
    OBTURADA("Obturada", "O"),
    EXTRAIDA("Extraída", "X"),
    EN_TRATAMIENTO("En tratamiento", "T");

    /** Texto que ve el usuario. */
    private final String etiqueta;
    /** Letra que se dibuja sobre la pieza en el odontograma. */
    private final String codigo;
}
