package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vyra.dental.dao.memoria.CitaDAOMemoria;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;

class CitaServiceTest {

    /** Reloj fijo: "hoy" es el martes 15 de enero de 2030 a las 10:00. */
    private static final LocalDate HOY = LocalDate.of(2030, 1, 15);
    private static final Clock RELOJ = Clock.fixed(HOY.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());
    private static final LocalDate FECHA = HOY.plusDays(7);

    private final Profesional salinas = profesional(1, "Marco", "Salinas");
    private final Profesional nunez = profesional(2, "Patricia", "Núñez");
    private final Paciente maria = paciente(1, "María", "Quispe");
    private final Paciente jorge = paciente(2, "Jorge", "Mamani");

    private CitaDAOMemoria citaDAO;
    private CitaService servicio;

    @BeforeEach
    void preparar() {
        citaDAO = new CitaDAOMemoria();
        servicio = new CitaService(citaDAO, RELOJ);
        iniciarSesion(Rol.RECEPCION, null);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    /** Pruebas obligatorias de docs/09-plan-implementacion.md, fase 4. */
    @Nested
    class CruceDeHorario {

        @BeforeEach
        void citaExistente() {
            // Salinas atiende a María de 09:00 a 10:00
            servicio.agendar(cita(salinas, maria, FECHA, "09:00", "10:00"));
        }

        @Test
        void mismaHora() {
            assertTrue(servicio.existeCruceDeHorario(cita(salinas, jorge, FECHA, "09:00", "10:00")));
        }

        @Test
        void traslapeParcialAlInicio() {
            assertTrue(servicio.existeCruceDeHorario(cita(salinas, jorge, FECHA, "08:30", "09:30")));
        }

        @Test
        void traslapeParcialAlFinal() {
            assertTrue(servicio.existeCruceDeHorario(cita(salinas, jorge, FECHA, "09:30", "10:30")));
        }

        @Test
        void unaCitaQueContieneALaOtra() {
            assertTrue(servicio.existeCruceDeHorario(cita(salinas, jorge, FECHA, "08:30", "10:30")));
            assertTrue(servicio.existeCruceDeHorario(cita(salinas, jorge, FECHA, "09:00", "09:30")));
        }

        @Test
        void citasContiguasNoCruzan() {
            assertFalse(servicio.existeCruceDeHorario(cita(salinas, jorge, FECHA, "08:30", "09:00")));
            assertFalse(servicio.existeCruceDeHorario(cita(salinas, jorge, FECHA, "10:00", "10:30")));
        }

        @Test
        void otroProfesionalNoCruza() {
            assertFalse(servicio.existeCruceDeHorario(cita(nunez, jorge, FECHA, "09:00", "10:00")));
        }

        @Test
        void otraFechaNoCruza() {
            assertFalse(servicio.existeCruceDeHorario(cita(salinas, jorge, FECHA.plusDays(1), "09:00", "10:00")));
        }

        @Test
        void unaCitaCanceladaNoCruza() {
            int id = citaDAO.listarPorFecha(FECHA).get(0).getIdCita();
            servicio.cancelar(id);
            assertFalse(servicio.existeCruceDeHorario(cita(salinas, jorge, FECHA, "09:00", "10:00")));
        }

        @Test
        void unaCitaAtendidaTampocoCruza() {
            // Se atiende hoy: una cita futura no se puede marcar como atendida
            Cita deHoy = servicio.agendar(cita(nunez, maria, HOY, "19:00", "19:30"));
            servicio.marcarAtendida(deHoy.getIdCita());
            assertFalse(servicio.existeCruceDeHorario(cita(nunez, jorge, HOY, "19:00", "19:30")));
        }

        @Test
        void reprogramarLaMismaCitaNoCruzaConsigoMisma() {
            Cita existente = citaDAO.listarPorFecha(FECHA).get(0);
            existente.setHoraInicio(LocalTime.of(9, 30));
            existente.setHoraFin(LocalTime.of(10, 30));

            assertFalse(servicio.existeCruceDeHorario(existente));
            servicio.reprogramar(existente);
            assertEquals(LocalTime.of(9, 30), citaDAO.buscarPorId(existente.getIdCita()).orElseThrow().getHoraInicio());
        }

        @Test
        void agendarConCruceExplicaConQuienSeCruza() {
            ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                    () -> servicio.agendar(cita(salinas, jorge, FECHA, "09:30", "10:00")));

            assertEquals("Marco Salinas ya tiene una cita con María Quispe de 09:00 a 10:00.", error.getMessage());
            assertEquals("horaInicio", error.getCampo().orElseThrow());
            assertEquals(1, citaDAO.listarPorFecha(FECHA).size());
        }
    }

    @Test
    void horariosLibresSaltaLosOcupados() {
        servicio.agendar(cita(salinas, maria, FECHA, "09:00", "10:00"));
        servicio.agendar(cita(salinas, jorge, FECHA, "11:00", "11:30"));

        List<LocalTime> libres = servicio.horariosLibres(salinas.getIdProfesional(), FECHA, 30);

        assertEquals(LocalTime.of(8, 0), libres.get(0));
        assertTrue(libres.contains(LocalTime.of(8, 30)));
        assertFalse(libres.contains(LocalTime.of(9, 0)));
        assertFalse(libres.contains(LocalTime.of(9, 30)));
        assertTrue(libres.contains(LocalTime.of(10, 0)));
        assertFalse(libres.contains(LocalTime.of(11, 0)));
        assertEquals(LocalTime.of(19, 30), libres.get(libres.size() - 1));
        // 24 bloques de 08:00 a 19:30, menos 3 ocupados
        assertEquals(21, libres.size());
    }

    @Test
    void horariosLibresConCitasDeUnaHora() {
        servicio.agendar(cita(salinas, maria, FECHA, "09:00", "10:00"));

        List<LocalTime> libres = servicio.horariosLibres(salinas.getIdProfesional(), FECHA, 60);

        // 08:30-09:30 se cruza; 08:00-09:00 no
        assertTrue(libres.contains(LocalTime.of(8, 0)));
        assertFalse(libres.contains(LocalTime.of(8, 30)));
        assertTrue(libres.contains(LocalTime.of(10, 0)));
        // La última que termina a las 20:00
        assertEquals(LocalTime.of(19, 0), libres.get(libres.size() - 1));
    }

    @Test
    void reglasDeHorario() {
        assertEquals("La hora de fin debe ser posterior a la de inicio.", assertThrows(ReglaNegocioException.class,
                () -> servicio.agendar(cita(salinas, maria, FECHA, "10:00", "10:00"))).getMessage());
        assertEquals("El horario de atención es de 08:00 a 20:00.", assertThrows(ReglaNegocioException.class,
                () -> servicio.agendar(cita(salinas, maria, FECHA, "07:30", "08:00"))).getMessage());
        assertEquals("El horario de atención es de 08:00 a 20:00.", assertThrows(ReglaNegocioException.class,
                () -> servicio.agendar(cita(salinas, maria, FECHA, "19:30", "20:30"))).getMessage());
        assertThrows(ReglaNegocioException.class,
                () -> servicio.agendar(cita(salinas, maria, FECHA, "09:15", "09:45")));
        // 19:30 a 20:00 sí se puede
        servicio.agendar(cita(salinas, maria, FECHA, "19:30", "20:00"));
    }

    @Test
    void noSeAgendaEnUnaFechaPasada() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.agendar(cita(salinas, maria, HOY.minusDays(1), "09:00", "09:30")));
        assertEquals("fecha", error.getCampo().orElseThrow());
    }

    @Test
    void hoyNoSeAgendaEnUnaHoraQueYaPaso() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.agendar(cita(salinas, maria, HOY, "09:30", "10:00")));
        assertEquals("Esa hora ya pasó: elige un horario desde las 10:00.", error.getMessage());
        assertEquals("horaInicio", error.getCampo().orElseThrow());

        servicio.agendar(cita(salinas, maria, HOY, "10:00", "10:30"));
    }

    @Test
    void hoyLosHorariosLibresEmpiezanDesdeAhora() {
        List<LocalTime> libres = servicio.horariosLibres(salinas.getIdProfesional(), HOY, 30);
        assertEquals(LocalTime.of(10, 0), libres.get(0));
    }

    @Test
    void unaCitaNuevaQuedaPendienteSiNoSeIndica() {
        Cita cita = cita(salinas, maria, FECHA, "09:00", "09:30");
        cita.setEstado(null);
        assertEquals(EstadoCita.PENDIENTE, servicio.agendar(cita).getEstado());

        Cita atendida = cita(salinas, jorge, FECHA, "10:00", "10:30");
        atendida.setEstado(EstadoCita.ATENDIDA);
        assertThrows(ReglaNegocioException.class, () -> servicio.agendar(atendida));
    }

    @Test
    void cambiosDeEstado() {
        Cita cita = servicio.agendar(cita(salinas, maria, FECHA, "09:00", "09:30"));

        servicio.confirmar(cita.getIdCita());
        assertEquals(EstadoCita.CONFIRMADA, citaDAO.buscarPorId(cita.getIdCita()).orElseThrow().getEstado());
        assertThrows(ReglaNegocioException.class, () -> servicio.confirmar(cita.getIdCita()));

        servicio.cancelar(cita.getIdCita());
        assertEquals(EstadoCita.CANCELADA, citaDAO.buscarPorId(cita.getIdCita()).orElseThrow().getEstado());
        // RN-07: la cita sigue existiendo, pero ya no se puede cambiar
        assertThrows(ReglaNegocioException.class, () -> servicio.cancelar(cita.getIdCita()));
        assertThrows(ReglaNegocioException.class, () -> servicio.reprogramar(cita));
    }

    @Test
    void noSeMarcaAtendidaUnaCitaFutura() {
        Cita cita = servicio.agendar(cita(salinas, maria, FECHA, "09:00", "09:30"));
        assertThrows(ReglaNegocioException.class, () -> servicio.marcarAtendida(cita.getIdCita()));
    }

    @Test
    void elOdontologoSoloVeYAtiendeSusCitas() {
        LocalDate hoy = HOY;
        Cita deSalinas = servicio.agendar(cita(salinas, maria, hoy, "19:00", "19:30"));
        Cita deNunez = servicio.agendar(cita(nunez, jorge, hoy, "19:00", "19:30"));
        iniciarSesion(Rol.ODONTOLOGO, salinas);

        assertFalse(servicio.puedeGestionar());
        assertEquals(List.of(deSalinas.getIdCita()), servicio.citasDelDia(hoy).stream().map(Cita::getIdCita).toList());
        assertThrows(ReglaNegocioException.class, () -> servicio.agendar(cita(salinas, jorge, FECHA, "09:00", "09:30")));
        assertThrows(ReglaNegocioException.class, () -> servicio.cancelar(deSalinas.getIdCita()));
        assertThrows(ReglaNegocioException.class, () -> servicio.marcarAtendida(deNunez.getIdCita()));

        servicio.marcarAtendida(deSalinas.getIdCita());
        assertEquals(EstadoCita.ATENDIDA, citaDAO.buscarPorId(deSalinas.getIdCita()).orElseThrow().getEstado());
    }

    @Test
    void ultimaVisitaYContadorDeHoy() {
        LocalDate hoy = HOY;
        Cita atendida = servicio.agendar(cita(salinas, maria, hoy, "19:00", "19:30"));
        servicio.marcarAtendida(atendida.getIdCita());
        Cita cancelada = servicio.agendar(cita(salinas, jorge, hoy, "19:30", "20:00"));
        servicio.cancelar(cancelada.getIdCita());
        servicio.agendar(cita(salinas, jorge, FECHA, "09:00", "09:30"));

        assertEquals(hoy, servicio.ultimaVisita(maria.getIdPaciente()).orElseThrow());
        assertTrue(servicio.ultimaVisita(jorge.getIdPaciente()).isEmpty());
        assertEquals(1, servicio.citasActivasDeHoy());
    }

    private static Cita cita(Profesional profesional, Paciente paciente, LocalDate fecha, String inicio, String fin) {
        return new Cita(0, paciente, profesional, fecha, LocalTime.parse(inicio), LocalTime.parse(fin),
                "Control", EstadoCita.PENDIENTE);
    }

    private static Profesional profesional(int id, String nombres, String apellidos) {
        return new Profesional(id, null, nombres, apellidos, "Odontología general", null, null);
    }

    private static Paciente paciente(int id, String nombres, String apellidos) {
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(id);
        paciente.setNombres(nombres);
        paciente.setApellidos(apellidos);
        paciente.setDni("1000000" + id);
        return paciente;
    }

    private static void iniciarSesion(Rol rol, Profesional profesional) {
        Sesion.get().iniciar(new Usuario(1, "prueba", "Usuario de prueba", "pbkdf2$1$a$b", rol, true), profesional);
    }
}
