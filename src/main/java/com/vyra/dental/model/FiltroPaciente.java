package com.vyra.dental.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Filtros de la lista de pacientes (chips de la pantalla Pacientes). */
@Getter
@RequiredArgsConstructor
public enum FiltroPaciente {
    TODOS("Todos"),
    CON_ALERGIAS("Con alergias"),
    MENORES_DE_EDAD("Menores de edad");

    /** Texto que ve el usuario. */
    private final String etiqueta;
}
