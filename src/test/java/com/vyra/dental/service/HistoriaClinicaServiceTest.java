package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.dao.memoria.CitaDAOMemoria;
import com.vyra.dental.dao.memoria.HistoriaClinicaDAOMemoria;
import com.vyra.dental.dao.memoria.PacienteDAOMemoria;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.HistoriaClinicaEntrada;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;

class HistoriaClinicaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2030, 1, 15);
    private static final Clock RELOJ = Clock.fixed(HOY.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    private final Profesional salinas = new Profesional(7, 3, "Marco", "Salinas", "Endodoncia", null, null);

    private PacienteDAOMemoria pacienteDAO;
    private CitaDAOMemoria citaDAO;
    private HistoriaClinicaDAOMemoria historiaDAO;
    private HistoriaClinicaService servicio;
    private Paciente maria;
    private Paciente jorge;

    @BeforeEach
    void preparar() {
        pacienteDAO = new PacienteDAOMemoria();
        citaDAO = new CitaDAOMemoria();
        historiaDAO = new HistoriaClinicaDAOMemoria();
        servicio = new HistoriaClinicaService(historiaDAO, pacienteDAO, citaDAO, RELOJ);
        maria = pacienteDAO.crear(paciente("María", "Quispe", "72481935"));
        jorge = pacienteDAO.crear(paciente("Jorge", "Mamani", "45812307"));
        iniciarSesion(Rol.ODONTOLOGO, salinas);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void registraLaEntradaConLaFechaDeHoyYElProfesionalDeLaSesion() {
        HistoriaClinicaEntrada entrada = entrada(maria, null, "  Caries oclusal en 36  ", "Se programa obturación");
        entrada.setFecha(HOY.minusDays(30));

        servicio.registrarEntrada(entrada);

        HistoriaClinicaEntrada guardada = servicio.historial(maria.getIdPaciente()).get(0);
        assertEquals(HOY, guardada.getFecha());
        assertEquals(salinas.getIdProfesional(), guardada.getIdProfesional());
        assertEquals("Caries oclusal en 36", guardada.getDiagnostico());
        assertNull(guardada.getIdCita());
    }

    @Test
    void necesitaDiagnosticoUObservaciones() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrarEntrada(entrada(maria, null, "  ", null)));

        assertEquals("diagnostico", error.getCampo().orElseThrow());
        assertTrue(servicio.historial(maria.getIdPaciente()).isEmpty());
        // Con solo uno de los dos basta
        servicio.registrarEntrada(entrada(maria, null, null, "Control sin hallazgos"));
        assertEquals(1, servicio.historial(maria.getIdPaciente()).size());
    }

    @Test
    void seAsociaALaCitaDelPaciente() {
        Cita cita = citaDAO.crear(cita(maria, EstadoCita.CONFIRMADA));

        servicio.registrarEntrada(entrada(maria, cita.getIdCita(), "Pulpitis", null));

        assertEquals(cita.getIdCita(), servicio.historial(maria.getIdPaciente()).get(0).getIdCita());
    }

    @Test
    void rechazaUnaCitaDeOtroPacienteOCancelada() {
        Cita deJorge = citaDAO.crear(cita(jorge, EstadoCita.CONFIRMADA));
        Cita cancelada = citaDAO.crear(cita(maria, EstadoCita.CANCELADA));

        ReglaNegocioException otro = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrarEntrada(entrada(maria, deJorge.getIdCita(), "Pulpitis", null)));
        ReglaNegocioException cancelo = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrarEntrada(entrada(maria, cancelada.getIdCita(), "Pulpitis", null)));

        assertEquals("idCita", otro.getCampo().orElseThrow());
        assertEquals("idCita", cancelo.getCampo().orElseThrow());
        assertTrue(servicio.historial(maria.getIdPaciente()).isEmpty());
    }

    @Test
    void rechazaUnPacienteQueNoExiste() {
        Paciente fantasma = paciente("Nadie", "Ninguno", "00000000");
        fantasma.setIdPaciente(999);

        assertThrows(ReglaNegocioException.class, () -> servicio.registrarEntrada(entrada(fantasma, null, "X", null)));
    }

    @Test
    void elHistorialVaDeLaMasRecienteALaMasAntigua() {
        historiaDAO.crear(new HistoriaClinicaEntrada(0, maria.getIdPaciente(), null, null, HOY.minusDays(40), null, "Primera"));
        historiaDAO.crear(new HistoriaClinicaEntrada(0, maria.getIdPaciente(), null, null, HOY.minusDays(10), null, "Segunda"));
        historiaDAO.crear(new HistoriaClinicaEntrada(0, jorge.getIdPaciente(), null, null, HOY, null, "De Jorge"));
        servicio.registrarEntrada(entrada(maria, null, "Tercera", null));

        List<String> diagnosticos = servicio.historial(maria.getIdPaciente()).stream()
                .map(HistoriaClinicaEntrada::getDiagnostico).toList();

        assertEquals(List.of("Tercera", "Segunda", "Primera"), diagnosticos);
    }

    @Test
    void soloElOdontologoEscribe() {
        for (Rol rol : new Rol[] { Rol.ADMIN, Rol.RECEPCION }) {
            iniciarSesion(rol, null);
            assertThrows(ReglaNegocioException.class,
                    () -> servicio.registrarEntrada(entrada(maria, null, "Pulpitis", null)));
            assertThrows(ReglaNegocioException.class,
                    () -> servicio.actualizarDatosClinicos(maria.getIdPaciente(), "Látex", null));
        }
        assertTrue(servicio.historial(maria.getIdPaciente()).isEmpty());
    }

    @Test
    void actualizaAlergiasYAntecedentesSinTocarLosDemasDatos() {
        servicio.actualizarDatosClinicos(maria.getIdPaciente(), "  Penicilina ", "  ");

        Paciente guardado = pacienteDAO.buscarPorId(maria.getIdPaciente()).orElseThrow();
        assertEquals("Penicilina", guardado.getAlergias());
        assertNull(guardado.getAntecedentes());
        assertEquals("72481935", guardado.getDni());
        assertEquals("María", guardado.getNombres());
    }

    private static HistoriaClinicaEntrada entrada(Paciente paciente, Integer idCita, String diagnostico,
                                                  String observaciones) {
        return new HistoriaClinicaEntrada(0, paciente.getIdPaciente(), idCita, null, null, observaciones, diagnostico);
    }

    private Cita cita(Paciente paciente, EstadoCita estado) {
        return new Cita(0, paciente, salinas, HOY, LocalTime.of(9, 0), LocalTime.of(10, 0), "Control", estado);
    }

    private static Paciente paciente(String nombres, String apellidos, String dni) {
        Paciente paciente = new Paciente();
        paciente.setNombres(nombres);
        paciente.setApellidos(apellidos);
        paciente.setDni(dni);
        return paciente;
    }

    private static void iniciarSesion(Rol rol, Profesional profesional) {
        Sesion.get().iniciar(new Usuario(3, "prueba", "Usuario de prueba", "pbkdf2$1$a$b", rol, true), profesional);
    }
}
