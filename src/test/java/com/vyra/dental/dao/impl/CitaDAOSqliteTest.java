package com.vyra.dental.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Profesional;

/**
 * Corre contra SQLite en memoria (vyra.bd.url en pom.xml). Cada prueba crea su
 * propio profesional, así las citas de otras pruebas no interfieren.
 */
class CitaDAOSqliteTest {

    private static final LocalDate FECHA = LocalDate.of(2030, 5, 6);
    private static int contador;

    private final CitaDAOSqlite dao = new CitaDAOSqlite();
    private Profesional profesional;
    private Paciente paciente;

    @BeforeEach
    void preparar() {
        contador++;
        profesional = new ProfesionalDAOSqlite().crear(
                new Profesional(0, null, "Prof" + contador, "Cita Sql", "Endodoncia", null, null));
        Paciente nuevo = new Paciente();
        nuevo.setNombres("María");
        nuevo.setApellidos("Cita Sql " + contador);
        nuevo.setDni(String.format("9100%04d", contador));
        paciente = new PacienteDAOSqlite().crear(nuevo);
    }

    @Test
    void guardaYLeeLaCitaConPacienteYProfesional() {
        Cita creada = dao.crear(cita("09:00", "10:00", EstadoCita.PENDIENTE));

        Cita leida = dao.buscarPorId(creada.getIdCita()).orElseThrow();

        assertEquals(LocalTime.of(9, 0), leida.getHoraInicio());
        assertEquals(LocalTime.of(10, 0), leida.getHoraFin());
        assertEquals("María " + paciente.getApellidos(), leida.getPaciente().getNombreCompleto());
        assertEquals(paciente.getDni(), leida.getPaciente().getDni());
        assertEquals("Endodoncia", leida.getProfesional().getEspecialidad());
        assertEquals(List.of(creada.getIdCita()),
                dao.listarPorProfesionalYFecha(profesional.getIdProfesional(), FECHA).stream().map(Cita::getIdCita).toList());
    }

    @Test
    void laConsultaDeCruceComparaLasHorasComoTexto() {
        Cita existente = dao.crear(cita("09:00", "10:00", EstadoCita.CONFIRMADA));
        int id = profesional.getIdProfesional();

        assertTrue(dao.existeCruce(id, FECHA, LocalTime.of(9, 30), LocalTime.of(10, 30), 0));
        assertTrue(dao.existeCruce(id, FECHA, LocalTime.of(8, 30), LocalTime.of(10, 30), 0));
        assertFalse(dao.existeCruce(id, FECHA, LocalTime.of(10, 0), LocalTime.of(10, 30), 0));
        assertFalse(dao.existeCruce(id, FECHA, LocalTime.of(8, 0), LocalTime.of(9, 0), 0));
        assertFalse(dao.existeCruce(id, FECHA, LocalTime.of(9, 0), LocalTime.of(10, 0), existente.getIdCita()));
        assertEquals(existente.getIdCita(),
                dao.buscarCruce(id, FECHA, LocalTime.of(9, 0), LocalTime.of(9, 30), 0).orElseThrow().getIdCita());

        dao.actualizarEstado(existente.getIdCita(), EstadoCita.CANCELADA);
        assertFalse(dao.existeCruce(id, FECHA, LocalTime.of(9, 0), LocalTime.of(10, 0), 0));
    }

    @Test
    void ultimaVisitaYCitasDelPaciente() {
        Cita vieja = cita("09:00", "09:30", EstadoCita.ATENDIDA);
        vieja.setFecha(FECHA.minusDays(10));
        dao.crear(vieja);
        dao.crear(cita("09:00", "09:30", EstadoCita.ATENDIDA));
        Cita futura = cita("09:00", "09:30", EstadoCita.PENDIENTE);
        futura.setFecha(FECHA.plusDays(3));
        dao.crear(futura);

        assertEquals(FECHA, dao.ultimaVisita(paciente.getIdPaciente()).orElseThrow());
        List<Cita> citas = dao.listarPorPaciente(paciente.getIdPaciente());
        assertEquals(List.of(FECHA.plusDays(3), FECHA, FECHA.minusDays(10)), citas.stream().map(Cita::getFecha).toList());
        assertEquals(3, dao.contarPorProfesional(profesional.getIdProfesional()));
    }

    @Test
    void reprogramarActualizaTodosLosCampos() {
        Cita cita = dao.crear(cita("09:00", "09:30", EstadoCita.PENDIENTE));
        cita.setFecha(FECHA.plusDays(1));
        cita.setHoraInicio(LocalTime.of(15, 30));
        cita.setHoraFin(LocalTime.of(16, 30));
        cita.setMotivo("Endodoncia pieza 36");

        assertTrue(dao.actualizar(cita));
        Cita leida = dao.buscarPorId(cita.getIdCita()).orElseThrow();
        assertEquals(FECHA.plusDays(1), leida.getFecha());
        assertEquals(LocalTime.of(16, 30), leida.getHoraFin());
        assertEquals("Endodoncia pieza 36", leida.getMotivo());
        assertEquals(1, dao.listarEntre(FECHA.plusDays(1), FECHA.plusDays(1)).stream()
                .filter(c -> c.getIdCita() == cita.getIdCita()).count());
    }

    private Cita cita(String inicio, String fin, EstadoCita estado) {
        return new Cita(0, paciente, profesional, FECHA, LocalTime.parse(inicio), LocalTime.parse(fin), "Control", estado);
    }
}
