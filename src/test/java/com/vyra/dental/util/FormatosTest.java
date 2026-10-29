package com.vyra.dental.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class FormatosTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 29);

    @Test
    void fechaLargaEmpiezaConMayuscula() {
        assertEquals("Jueves, 29 de octubre de 2026", Formatos.fechaLarga(FECHA));
    }

    @Test
    void fechaCortaSinPuntoEnElMes() {
        assertEquals("29 oct 2026", Formatos.fechaCorta(FECHA));
        assertEquals("05 set 2026", Formatos.fechaCorta(LocalDate.of(2026, 9, 5)));
    }

    @Test
    void formatosDeLaAgenda() {
        assertEquals("Jueves 29 de octubre", Formatos.diaYMes(FECHA));
        assertEquals("Jue", Formatos.diaCorto(FECHA));
        assertEquals("Mié", Formatos.diaCorto(FECHA.minusDays(1)));
        assertEquals("Sáb", Formatos.diaCorto(FECHA.plusDays(2)));
        assertEquals("09:00–10:00", Formatos.rangoHoras(LocalTime.of(9, 0), LocalTime.of(10, 0)));
    }

    @Test
    void busquedaSinTildes() {
        assertEquals("maria pena", Busqueda.normalizar("  María PEÑA "));
        assertEquals("", Busqueda.normalizar(null));
    }

    @Test
    void mesYAnio() {
        assertEquals("marzo 2024", Formatos.mesAnio(LocalDate.of(2024, 3, 5)));
        assertEquals("", Formatos.mesAnio(null));
    }

    @Test
    void inicialesDeUnNombreCompleto() {
        assertEquals("MQ", Formatos.iniciales("María Quispe"));
        assertEquals("A", Formatos.iniciales("Administrador"));
        assertEquals("", Formatos.iniciales("  "));
    }

    @Test
    void hora() {
        assertEquals("09:30", Formatos.hora(LocalTime.of(9, 30)));
    }

    @Test
    void dineroConSeparadorDeMiles() {
        assertEquals("S/ 1,250.00", Formatos.dinero(1250));
        assertEquals("S/ 0.50", Formatos.dinero(0.5));
    }

    @Test
    void telefonoEnGruposDeTres() {
        assertEquals("987 654 321", Formatos.telefono("987654321"));
        assertEquals("12345", Formatos.telefono("12345"));
    }

    @Test
    void edadEnAnios() {
        assertEquals("1 año", Formatos.edad(LocalDate.now().minusYears(1)));
        assertEquals("34 años", Formatos.edad(LocalDate.now().minusYears(34)));
    }

    @Test
    void iniciales() {
        assertEquals("MQ", Formatos.iniciales("María Elena", "Quispe Rojas"));
        assertEquals("M", Formatos.iniciales("maría", null));
    }

    @Test
    void valoresNulosSeMuestranVacios() {
        assertEquals("", Formatos.fechaLarga(null));
        assertEquals("", Formatos.fechaCorta(null));
        assertEquals("", Formatos.hora(null));
        assertEquals("", Formatos.telefono(null));
        assertEquals("", Formatos.edad(null));
    }
}
