package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
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

    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 29);

    private HistoriaClinicaDAOMemoria historiaDAO;
    private PacienteDAOMemoria pacienteDAO;
    private CitaDAOMemoria citaDAO;
    private Clock reloj;
    private HistoriaClinicaService servicio;
    private Paciente maria;
    private Profesional salinas;

    @BeforeEach
    void preparar() {
        historiaDAO = new HistoriaClinicaDAOMemoria();
        pacienteDAO = new PacienteDAOMemoria();
        citaDAO = new CitaDAOMemoria();
        reloj = Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"), ZONA);
        servicio = new HistoriaClinicaService(historiaDAO, pacienteDAO, citaDAO, reloj);
        maria = pacienteDAO.crear(paciente("María", "Quispe", "72481935"));
        salinas = new Profesional(1, 5, "Marco", "Salinas Vega", "Endodoncia", null, null);
        iniciarSesionComo(Rol.ODONTOLOGO, salinas);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void registraLaEntradaConLaFechaDeHoyAunqueTraigaOtra() {
        HistoriaClinicaEntrada entrada = entrada(maria, "Caries en la pieza 36", null);
        entrada.setFecha(LocalDate.of(2020, 1, 1));

        HistoriaClinicaEntrada guardada = servicio.registrarEntrada(entrada);

        assertTrue(guardada.getIdEntrada() > 0);
        assertEquals(HOY, guardada.getFecha());
    }

    @Test
    void exigeDiagnosticoOObservaciones() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrarEntrada(entrada(maria, "   ", "  ")));

        assertEquals("diagnostico", error.getCampo().orElseThrow());
    }

    @Test
    void bastaConLasObservaciones() {
        HistoriaClinicaEntrada guardada = servicio.registrarEntrada(entrada(maria, null, "Control sin novedades"));

        assertNull(guardada.getDiagnostico());
        assertEquals("Control sin novedades", guardada.getObservaciones());
    }

    @Test
    void lasEntradasSeListanDeLaMasRecienteALaMasAntigua() {
        // Misma fecha: la registrada despues va primero
        servicio.registrarEntrada(entrada(maria, "Primera", null));
        servicio.registrarEntrada(entrada(maria, "Segunda", null));
        historiaDAO.crear(new HistoriaClinicaEntrada(0, maria.getIdPaciente(), null, HOY.minusDays(30), null, "Antigua"));

        List<HistoriaClinicaEntrada> lista = servicio.entradasDe(maria.getIdPaciente());

        assertEquals(List.of("Segunda", "Primera", "Antigua"),
                lista.stream().map(HistoriaClinicaEntrada::getDiagnostico).toList());
    }

    @Test
    void agregarUnaEntradaNoCambiaLasAnteriores() {
        HistoriaClinicaEntrada primera = servicio.registrarEntrada(entrada(maria, "Gingivitis", "Dolor al cepillar"));
        servicio.registrarEntrada(entrada(maria, "Gingivitis en mejoría", null));

        HistoriaClinicaEntrada guardada = historiaDAO.buscarPorId(primera.getIdEntrada()).orElseThrow();
        assertEquals("Gingivitis", guardada.getDiagnostico());
        assertEquals("Dolor al cepillar", guardada.getObservaciones());
        assertEquals(2, servicio.entradasDe(maria.getIdPaciente()).size());
    }

    @Test
    void laCitaDebeSerDelMismoPaciente() {
        Paciente jorge = pacienteDAO.crear(paciente("Jorge", "Mamani", "45812307"));
        Cita citaDeJorge = citaDAO.crear(cita(jorge, HOY, EstadoCita.CONFIRMADA));
        HistoriaClinicaEntrada entrada = entrada(maria, "Caries", null);
        entrada.setIdCita(citaDeJorge.getIdCita());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrarEntrada(entrada));

        assertEquals("La cita elegida no es de este paciente.", error.getMessage());
    }

    @Test
    void laCitaDebeExistir() {
        HistoriaClinicaEntrada entrada = entrada(maria, "Caries", null);
        entrada.setIdCita(999);

        assertThrows(ReglaNegocioException.class, () -> servicio.registrarEntrada(entrada));
    }

    @Test
    void noSeRegistraParaUnPacienteDadoDeBaja() {
        pacienteDAO.darDeBaja(maria.getIdPaciente());

        assertThrows(ReglaNegocioException.class, () -> servicio.registrarEntrada(entrada(maria, "Caries", null)));
    }

    @Test
    void laCitaDeHoyEsLaDelPacienteConElProfesionalDeLaSesion() {
        citaDAO.crear(cita(maria, HOY.minusDays(1), EstadoCita.CONFIRMADA));
        citaDAO.crear(cita(maria, HOY, EstadoCita.CANCELADA));
        Cita vigente = citaDAO.crear(cita(maria, HOY, EstadoCita.PENDIENTE));

        assertEquals(vigente.getIdCita(), servicio.citaDeHoy(maria.getIdPaciente()).orElseThrow().getIdCita());
    }

    @Test
    void sinCitaVigenteHoyNoHayCitaDeHoy() {
        citaDAO.crear(cita(maria, HOY.plusDays(1), EstadoCita.PENDIENTE));

        assertTrue(servicio.citaDeHoy(maria.getIdPaciente()).isEmpty());
    }

    @Test
    void actualizaSoloAlergiasYAntecedentes() {
        servicio.actualizarDatosClinicos(maria.getIdPaciente(), "  Penicilina ", "   ");

        Paciente guardada = pacienteDAO.buscarPorId(maria.getIdPaciente()).orElseThrow();
        assertEquals("Penicilina", guardada.getAlergias());
        assertNull(guardada.getAntecedentes());
        assertEquals("72481935", guardada.getDni());
    }

    @Test
    void elAdministradorSoloConsulta() {
        servicio.registrarEntrada(entrada(maria, "Caries", null));
        iniciarSesionComo(Rol.ADMIN, null);

        assertEquals(1, servicio.entradasDe(maria.getIdPaciente()).size());
        assertThrows(ReglaNegocioException.class, () -> servicio.registrarEntrada(entrada(maria, "Otra", null)));
        assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizarDatosClinicos(maria.getIdPaciente(), "Látex", null));
    }

    @Test
    void recepcionNoTieneAcceso() {
        iniciarSesionComo(Rol.RECEPCION, null);

        assertThrows(ReglaNegocioException.class, () -> servicio.entradasDe(maria.getIdPaciente()));
        assertThrows(ReglaNegocioException.class, () -> servicio.registrarEntrada(entrada(maria, "Caries", null)));
    }

    private static void iniciarSesionComo(Rol rol, Profesional profesional) {
        Sesion.get().iniciar(new Usuario(1, "usuario", "Usuario de prueba", "pbkdf2$1$a$b", rol, true), profesional);
    }

    private static HistoriaClinicaEntrada entrada(Paciente paciente, String diagnostico, String observaciones) {
        HistoriaClinicaEntrada entrada = new HistoriaClinicaEntrada();
        entrada.setIdPaciente(paciente.getIdPaciente());
        entrada.setDiagnostico(diagnostico);
        entrada.setObservaciones(observaciones);
        return entrada;
    }

    private Cita cita(Paciente paciente, LocalDate fecha, EstadoCita estado) {
        return new Cita(0, paciente, salinas, fecha, LocalTime.of(9, 0), LocalTime.of(9, 30), "Control", estado);
    }

    private static Paciente paciente(String nombres, String apellidos, String dni) {
        return new Paciente(0, nombres, apellidos, dni, LocalDate.of(1990, 1, 1), null, null, null, null, null, true,
                LocalDate.of(2026, 1, 1));
    }
}
