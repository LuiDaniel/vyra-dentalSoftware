package com.vyra.dental.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.HistoriaClinicaEntrada;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Profesional;

/**
 * Corre contra SQLite en memoria (vyra.bd.url en pom.xml). Cada prueba crea su
 * propio paciente, así las entradas de otras pruebas no interfieren.
 */
class HistoriaClinicaDAOSqliteTest {

    private static int contador;

    private final HistoriaClinicaDAOSqlite dao = new HistoriaClinicaDAOSqlite();
    private Paciente paciente;

    @BeforeEach
    void preparar() {
        contador++;
        Paciente nuevo = new Paciente();
        nuevo.setNombres("Zulema");
        nuevo.setApellidos("Historia Sql " + contador);
        nuevo.setDni(String.format("9200%04d", contador));
        paciente = new PacienteDAOSqlite().crear(nuevo);
    }

    @Test
    void guardaYLeeLaEntradaSinCita() {
        HistoriaClinicaEntrada creada = dao.crear(entrada(LocalDate.of(2026, 10, 29), "Caries", "Pieza 36"));

        HistoriaClinicaEntrada leida = dao.buscarPorId(creada.getIdEntrada()).orElseThrow();

        assertTrue(creada.getIdEntrada() > 0);
        assertEquals(LocalDate.of(2026, 10, 29), leida.getFecha());
        assertEquals("Caries", leida.getDiagnostico());
        assertEquals("Pieza 36", leida.getObservaciones());
        assertNull(leida.getIdCita());
    }

    @Test
    void guardaLaCitaDeLaEntrada() {
        Profesional profesional = new ProfesionalDAOSqlite().crear(
                new Profesional(0, null, "Prof" + contador, "Historia Sql", "Endodoncia", null, null));
        Cita cita = new CitaDAOSqlite().crear(new Cita(0, paciente, profesional, LocalDate.of(2030, 5, 6),
                LocalTime.of(9, 0), LocalTime.of(9, 30), "Control", EstadoCita.ATENDIDA));
        HistoriaClinicaEntrada nueva = entrada(LocalDate.of(2030, 5, 6), "Control", null);
        nueva.setIdCita(cita.getIdCita());

        HistoriaClinicaEntrada leida = dao.buscarPorId(dao.crear(nueva).getIdEntrada()).orElseThrow();

        assertEquals(cita.getIdCita(), leida.getIdCita());
        assertNull(leida.getObservaciones());
    }

    @Test
    void listaDeLaMasRecienteALaMasAntiguaYSoloLasDelPaciente() {
        dao.crear(entrada(LocalDate.of(2026, 3, 1), "Antigua", null));
        dao.crear(entrada(LocalDate.of(2026, 10, 29), "Primera de hoy", null));
        dao.crear(entrada(LocalDate.of(2026, 10, 29), "Segunda de hoy", null));
        Paciente otro = new Paciente();
        otro.setNombres("Zoraida");
        otro.setApellidos("Historia Sql " + contador);
        otro.setDni(String.format("9300%04d", contador));
        otro = new PacienteDAOSqlite().crear(otro);
        HistoriaClinicaEntrada ajena = entrada(LocalDate.of(2026, 10, 29), "Ajena", null);
        ajena.setIdPaciente(otro.getIdPaciente());
        dao.crear(ajena);

        List<String> diagnosticos = dao.listarPorPaciente(paciente.getIdPaciente()).stream()
                .map(HistoriaClinicaEntrada::getDiagnostico).toList();

        assertEquals(List.of("Segunda de hoy", "Primera de hoy", "Antigua"), diagnosticos);
    }

    private HistoriaClinicaEntrada entrada(LocalDate fecha, String diagnostico, String observaciones) {
        return new HistoriaClinicaEntrada(0, paciente.getIdPaciente(), null, fecha, observaciones, diagnostico);
    }
}
