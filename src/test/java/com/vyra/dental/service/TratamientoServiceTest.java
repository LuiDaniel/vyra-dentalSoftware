package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.vyra.dental.dao.memoria.PacienteDAOMemoria;
import com.vyra.dental.dao.memoria.PagoDAOMemoria;
import com.vyra.dental.dao.memoria.TratamientoDAOMemoria;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.model.Usuario;

class TratamientoServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 29);

    private CitaDAOMemoria citaDAO;
    private PagoDAOMemoria pagoDAO;
    private TratamientoService servicio;
    private Paciente maria;
    private Profesional salinas;
    private Profesional otro;

    @BeforeEach
    void preparar() {
        PacienteDAOMemoria pacienteDAO = new PacienteDAOMemoria();
        citaDAO = new CitaDAOMemoria();
        pagoDAO = new PagoDAOMemoria();
        Clock reloj = Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"), ZONA);
        servicio = new TratamientoService(new TratamientoDAOMemoria(), citaDAO, pagoDAO, reloj);
        maria = pacienteDAO.crear(new Paciente(0, "María", "Quispe", "72481935", LocalDate.of(1990, 1, 1), null, null,
                null, null, null, true, LocalDate.of(2026, 1, 1)));
        salinas = new Profesional(1, 5, "Marco", "Salinas Vega", "Endodoncia", null, null);
        otro = new Profesional(2, 6, "Ana", "Rojas", "Ortodoncia", null, null);
        iniciarSesionComo(Rol.ODONTOLOGO, salinas);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void registraEnCursoConLaFechaDeHoyYLasPiezasOrdenadasSinRepetir() {
        Cita cita = cita(salinas, HOY, EstadoCita.CONFIRMADA);

        Tratamiento guardado = servicio.registrar(tratamiento(cita, "Endodoncia", 650, 47, 36, 36));

        assertTrue(guardado.getIdTratamiento() > 0);
        assertEquals(EstadoTratamiento.EN_CURSO, guardado.getEstado());
        assertEquals(HOY, guardado.getFecha());
        assertEquals(List.of(36, 47), guardado.getPiezasDentales());
    }

    @Test
    void elCostoDebeSerMayorACero() {
        Cita cita = cita(salinas, HOY, EstadoCita.CONFIRMADA);

        assertThrows(RuntimeException.class, () -> servicio.registrar(tratamiento(cita, "Limpieza", 0, 36)));
    }

    @Test
    void laDescripcionEsObligatoria() {
        Cita cita = cita(salinas, HOY, EstadoCita.CONFIRMADA);

        assertThrows(RuntimeException.class, () -> servicio.registrar(tratamiento(cita, "   ", 100, 36)));
    }

    @Test
    void rechazaPiezasQueNoExisten() {
        Cita cita = cita(salinas, HOY, EstadoCita.CONFIRMADA);

        for (int pieza : new int[] {19, 20, 29, 49}) {
            assertThrows(RuntimeException.class, () -> servicio.registrar(tratamiento(cita, "Caries", 100, pieza)),
                    "pieza " + pieza);
        }
    }

    @Test
    void noSeRegistraEnUnaCitaCancelada() {
        Cita cita = cita(salinas, HOY, EstadoCita.CANCELADA);

        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(tratamiento(cita, "Caries", 100, 36)));
    }

    @Test
    void noSeRegistraEnLaCitaDeOtroProfesional() {
        Cita cita = cita(otro, HOY, EstadoCita.CONFIRMADA);

        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(tratamiento(cita, "Caries", 100, 36)));
    }

    @Test
    void finalizaUnTratamientoEnCursoYNoDosVeces() {
        Cita cita = cita(salinas, HOY, EstadoCita.ATENDIDA);
        Tratamiento t = servicio.registrar(tratamiento(cita, "Caries", 100, 36));

        servicio.finalizar(t.getIdTratamiento());

        assertEquals(EstadoTratamiento.FINALIZADO,
                servicio.buscarPorId(t.getIdTratamiento()).orElseThrow().tratamiento().getEstado());
        assertThrows(ReglaNegocioException.class, () -> servicio.finalizar(t.getIdTratamiento()));
    }

    @Test
    void elResumenCalculaPagadoYSaldo() {
        Cita cita = cita(salinas, HOY, EstadoCita.ATENDIDA);
        Tratamiento t = servicio.registrar(tratamiento(cita, "Endodoncia", 650, 36));
        pagoDAO.crear(new Pago(0, t.getIdTratamiento(), 250, HOY, MetodoPago.EFECTIVO, null));

        TratamientoResumen resumen = servicio.buscarPorId(t.getIdTratamiento()).orElseThrow();

        assertEquals(250, resumen.pagado(), 0.001);
        assertEquals(400, resumen.saldo(), 0.001);
        assertEquals("María", resumen.paciente().getNombres());
    }

    @Test
    void filtraPorEstadoYPorNombreOdniSinTildes() {
        Cita cita = cita(salinas, HOY, EstadoCita.ATENDIDA);
        Tratamiento a = servicio.registrar(tratamiento(cita, "Endodoncia", 650, 36));
        servicio.registrar(tratamiento(cita, "Limpieza", 80));
        servicio.finalizar(a.getIdTratamiento());

        assertEquals(2, servicio.listar(null, null).size());
        assertEquals(1, servicio.listar(EstadoTratamiento.EN_CURSO, null).size());
        assertEquals(1, servicio.listar(EstadoTratamiento.FINALIZADO, "maria").size());
        assertEquals(2, servicio.listar(null, "72481935").size());
        assertTrue(servicio.listar(null, "pedro").isEmpty());
    }

    @Test
    void listaLosTratamientosDeUnPaciente() {
        Cita cita = cita(salinas, HOY, EstadoCita.ATENDIDA);
        servicio.registrar(tratamiento(cita, "Endodoncia", 650, 36));

        assertEquals(1, servicio.listarPorPaciente(maria.getIdPaciente()).size());
        assertTrue(servicio.listarPorPaciente(999).isEmpty());
    }

    @Test
    void lasCitasParaRegistrarSonLasRecientesNoCanceladasYPropias() {
        Cita hoy = cita(salinas, HOY, EstadoCita.CONFIRMADA);
        Cita reciente = cita(salinas, HOY.minusDays(10), EstadoCita.ATENDIDA);
        cita(salinas, HOY.minusDays(60), EstadoCita.ATENDIDA);
        cita(salinas, HOY, EstadoCita.CANCELADA);
        cita(otro, HOY, EstadoCita.CONFIRMADA);

        List<Cita> citas = servicio.citasParaRegistrar();

        assertEquals(List.of(hoy.getIdCita(), reciente.getIdCita()), citas.stream().map(Cita::getIdCita).toList());
    }

    @Test
    void soloElOdontologoRegistra() {
        Cita cita = cita(salinas, HOY, EstadoCita.CONFIRMADA);
        iniciarSesionComo(Rol.RECEPCION, null);

        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(tratamiento(cita, "Caries", 100, 36)));
        assertThrows(ReglaNegocioException.class, servicio::citasParaRegistrar);
    }

    @Test
    void adminYRecepcionSoloConsultan() {
        Cita cita = cita(salinas, HOY, EstadoCita.ATENDIDA);
        Tratamiento t = servicio.registrar(tratamiento(cita, "Caries", 100, 36));
        for (Rol rol : new Rol[] {Rol.ADMIN, Rol.RECEPCION}) {
            iniciarSesionComo(rol, null);

            assertEquals(1, servicio.listar(null, null).size());
            assertThrows(ReglaNegocioException.class, () -> servicio.finalizar(t.getIdTratamiento()));
        }
    }

    private Cita cita(Profesional profesional, LocalDate fecha, EstadoCita estado) {
        return citaDAO.crear(new Cita(0, maria, profesional, fecha, LocalTime.of(9, 0), LocalTime.of(9, 30), "Control",
                estado));
    }

    private static Tratamiento tratamiento(Cita cita, String descripcion, double costo, Integer... piezas) {
        Tratamiento t = new Tratamiento();
        t.setIdCita(cita.getIdCita());
        t.setDescripcion(descripcion);
        t.setCosto(costo);
        t.setPiezasDentales(List.of(piezas));
        return t;
    }

    private static void iniciarSesionComo(Rol rol, Profesional profesional) {
        Sesion.get().iniciar(new Usuario(1, "usuario", "Usuario de prueba", "pbkdf2$1$a$b", rol, true), profesional);
    }
}
