package com.vyra.dental.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.vyra.dental.model.Rol;

/** El menú debe coincidir con la matriz de permisos de docs/02-casos-de-uso.md §3. */
class PantallaTest {

    @Test
    void administradorVeTodo() {
        assertEquals(EnumSet.allOf(Pantalla.class), visiblesPara(Rol.ADMIN));
    }

    @Test
    void recepcionNoVeLaParteClinicaNiLaAdministrativa() {
        assertEquals(EnumSet.of(Pantalla.INICIO, Pantalla.AGENDA, Pantalla.PACIENTES,
                Pantalla.TRATAMIENTOS, Pantalla.PAGOS), visiblesPara(Rol.RECEPCION));
    }

    @Test
    void odontologoNoVePagosNiAdministracion() {
        assertEquals(EnumSet.of(Pantalla.INICIO, Pantalla.AGENDA, Pantalla.PACIENTES,
                Pantalla.ODONTOGRAMA, Pantalla.HISTORIA_CLINICA, Pantalla.TRATAMIENTOS),
                visiblesPara(Rol.ODONTOLOGO));
    }

    private static Set<Pantalla> visiblesPara(Rol rol) {
        return Arrays.stream(Pantalla.values())
                .filter(p -> p.permitidaPara(rol))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Pantalla.class)));
    }
}
