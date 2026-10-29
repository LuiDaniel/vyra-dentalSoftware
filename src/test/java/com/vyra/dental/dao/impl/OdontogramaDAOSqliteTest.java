package com.vyra.dental.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.model.EstadoPieza;
import com.vyra.dental.model.OdontogramaPieza;
import com.vyra.dental.model.Paciente;

/**
 * Corre contra SQLite en memoria (vyra.bd.url en pom.xml). Cada prueba crea su
 * propio paciente, así los registros de otras pruebas no interfieren.
 */
class OdontogramaDAOSqliteTest {

    private static int contador;

    private final OdontogramaDAOSqlite dao = new OdontogramaDAOSqlite();
    private Paciente paciente;

    @BeforeEach
    void preparar() {
        contador++;
        Paciente nuevo = new Paciente();
        nuevo.setNombres("Zulema");
        nuevo.setApellidos("Odontograma Sql " + contador);
        nuevo.setDni(String.format("9400%04d", contador));
        paciente = new PacienteDAOSqlite().crear(nuevo);
    }

    @Test
    void guardaYLeeElRegistro() {
        OdontogramaPieza creado = dao.registrar(registro(36, EstadoPieza.CARIADA, LocalDate.of(2026, 10, 29), "Oclusal"));

        OdontogramaPieza leido = dao.historialPieza(paciente.getIdPaciente(), 36).get(0);

        assertTrue(creado.getIdRegistro() > 0);
        assertEquals(creado.getIdRegistro(), leido.getIdRegistro());
        assertEquals(EstadoPieza.CARIADA, leido.getEstado());
        assertEquals(LocalDate.of(2026, 10, 29), leido.getFechaRegistro());
        assertEquals("Oclusal", leido.getObservacion());
    }

    @Test
    void sinRegistrosElEstadoActualEstaVacio() {
        assertTrue(dao.estadoActual(paciente.getIdPaciente()).isEmpty());
    }

    @Test
    void elEstadoActualEsElUltimoRegistroDeCadaPieza() {
        dao.registrar(registro(36, EstadoPieza.CARIADA, LocalDate.of(2026, 1, 10), null));
        dao.registrar(registro(11, EstadoPieza.EXTRAIDA, LocalDate.of(2026, 2, 5), null));
        dao.registrar(registro(36, EstadoPieza.OBTURADA, LocalDate.of(2026, 3, 1), null));

        List<OdontogramaPieza> actual = dao.estadoActual(paciente.getIdPaciente());

        assertEquals(2, actual.size());
        assertEquals(11, actual.get(0).getNumeroPieza());
        assertEquals(EstadoPieza.EXTRAIDA, actual.get(0).getEstado());
        assertEquals(36, actual.get(1).getNumeroPieza());
        assertEquals(EstadoPieza.OBTURADA, actual.get(1).getEstado());
        assertNull(actual.get(1).getObservacion());
    }

    @Test
    void elHistorialConservaTodosLosCambiosDelMasRecienteAlMasAntiguo() {
        dao.registrar(registro(36, EstadoPieza.CARIADA, LocalDate.of(2026, 1, 10), null));
        dao.registrar(registro(36, EstadoPieza.EN_TRATAMIENTO, LocalDate.of(2026, 2, 10), null));
        dao.registrar(registro(36, EstadoPieza.OBTURADA, LocalDate.of(2026, 3, 10), null));

        List<OdontogramaPieza> historial = dao.historialPieza(paciente.getIdPaciente(), 36);

        assertEquals(List.of(EstadoPieza.OBTURADA, EstadoPieza.EN_TRATAMIENTO, EstadoPieza.CARIADA),
                historial.stream().map(OdontogramaPieza::getEstado).toList());
    }

    @Test
    void noMezclaPacientes() {
        Paciente otro = new Paciente();
        otro.setNombres("Otro");
        otro.setApellidos("Odontograma Sql " + contador);
        otro.setDni(String.format("9401%04d", contador));
        otro = new PacienteDAOSqlite().crear(otro);
        dao.registrar(registro(36, EstadoPieza.CARIADA, LocalDate.of(2026, 1, 10), null));

        assertTrue(dao.estadoActual(otro.getIdPaciente()).isEmpty());
        assertTrue(dao.historialPieza(otro.getIdPaciente(), 36).isEmpty());
    }

    @Test
    void labaseDeDatosRechazaPiezasInvalidas() {
        assertThrows(RuntimeException.class,
                () -> dao.registrar(registro(19, EstadoPieza.CARIADA, LocalDate.of(2026, 1, 10), null)));
    }

    private OdontogramaPieza registro(int pieza, EstadoPieza estado, LocalDate fecha, String observacion) {
        return new OdontogramaPieza(0, paciente.getIdPaciente(), pieza, estado, fecha, observacion);
    }
}
