package com.vyra.dental.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

/** El saludo de Inicio cambia según la hora (docs/08-guia-frontend.md §7.2). */
class InicioControllerTest {

    @Test
    void saludoSegunLaHora() {
        assertEquals("Buenos días", InicioController.saludo(LocalTime.of(8, 0)));
        assertEquals("Buenas tardes", InicioController.saludo(LocalTime.NOON));
        assertEquals("Buenas tardes", InicioController.saludo(LocalTime.of(18, 59)));
        assertEquals("Buenas noches", InicioController.saludo(LocalTime.of(19, 0)));
    }
}
