package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import org.junit.jupiter.api.Test;

import com.vyra.dental.dao.memoria.CitaDAOMemoria;
import com.vyra.dental.dao.memoria.PagoDAOMemoria;
import com.vyra.dental.dao.memoria.TratamientoDAOMemoria;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.Cobro;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.model.Usuario;

/**
 * Fase 9: la consulta se cobra antes de atender (RN-16), un tratamiento con
 * saldo no se finaliza (RN-17) y la deuda del paciente (RN-18).
 */
class ConsultaYPagosTest {

    /** "Hoy" es el miércoles 15 de enero de 2025 a las 10:00 (pasado, por @PastOrPresent). */
    private static final LocalDate HOY = LocalDate.of(2025, 1, 15);
    private static final Clock RELOJ = Clock.fixed(HOY.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    private CitaDAOMemoria citaDAO;
    private PagoDAOMemoria pagoDAO;
    private TratamientoDAOMemoria tratamientoDAO;
    private CitaService citas;
    private PagoService pagos;
    private TratamientoService tratamientos;
    private Paciente maria;
    private Profesional salinas;

    @BeforeEach
    void preparar() {
        citaDAO = new CitaDAOMemoria();
        pagoDAO = new PagoDAOMemoria();
        tratamientoDAO = new TratamientoDAOMemoria(citaDAO, pagoDAO);
        citas = new CitaService(citaDAO, pagoDAO, RELOJ);
        pagos = new PagoService(pagoDAO, tratamientoDAO, citaDAO, RELOJ);
        tratamientos = new TratamientoService(tratamientoDAO, citaDAO, pagoDAO, RELOJ);

        maria = new Paciente();
        maria.setIdPaciente(1);
        maria.setNombres("María Fernanda");
        maria.setApellidos("Quispe Huamán");
        maria.setDni("72481935");
        salinas = new Profesional(1, null, "Marco", "Salinas", "Endodoncia", null, null, 80);
        iniciarSesion(Rol.RECEPCION, null);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void sinPagarLaConsultaNoSeAtiende() {
        Cita cita = citas.agendar(cita(LocalTime.of(11, 0), 80));
        assertFalse(citas.consultaPagada(cita));
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> citas.marcarAtendida(cita.getIdCita()));
        assertTrue(error.getMessage().contains("S/ 80.00"), error.getMessage());

        pagos.registrar(Pago.deConsulta(cita.getIdCita(), 80, HOY, MetodoPago.EFECTIVO, null));

        assertTrue(citas.consultaPagada(cita));
        citas.marcarAtendida(cita.getIdCita());
        assertEquals(EstadoCita.ATENDIDA, citaDAO.buscarPorId(cita.getIdCita()).orElseThrow().getEstado());
    }

    @Test
    void unaConsultaSinCostoSeAtiendeSinCobrar() {
        Cita control = citas.agendar(cita(LocalTime.of(11, 0), 0));
        assertDoesNotThrow(() -> citas.marcarAtendida(control.getIdCita()));
    }

    @Test
    void laConsultaSePuedePagarEnPartesSinPasarseDelCosto() {
        Cita cita = citas.agendar(cita(LocalTime.of(11, 0), 80));
        pagos.registrar(Pago.deConsulta(cita.getIdCita(), 50, HOY, MetodoPago.YAPE_PLIN, "123"));
        assertEquals(30.00, pagos.saldoConsulta(cita.getIdCita()));
        assertThrows(ReglaNegocioException.class, () -> citas.marcarAtendida(cita.getIdCita()));

        ReglaNegocioException pasado = assertThrows(ReglaNegocioException.class,
                () -> pagos.registrar(Pago.deConsulta(cita.getIdCita(), 40, HOY, MetodoPago.EFECTIVO, null)));
        assertEquals("monto", pasado.getCampo().orElseThrow());

        pagos.registrar(Pago.deConsulta(cita.getIdCita(), 30, HOY, MetodoPago.EFECTIVO, null));
        ReglaNegocioException pagada = assertThrows(ReglaNegocioException.class,
                () -> pagos.registrar(Pago.deConsulta(cita.getIdCita(), 1, HOY, MetodoPago.EFECTIVO, null)));
        assertEquals("Esta consulta ya está pagada.", pagada.getMessage());
    }

    @Test
    void laConsultaDeUnaCitaCanceladaNoSeCobra() {
        Cita cita = citas.agendar(cita(LocalTime.of(11, 0), 80));
        citas.cancelar(cita.getIdCita());
        assertThrows(ReglaNegocioException.class,
                () -> pagos.registrar(Pago.deConsulta(cita.getIdCita(), 80, HOY, MetodoPago.EFECTIVO, null)));
        assertTrue(pagos.consultasPorCobrar("").isEmpty());
    }

    @Test
    void unPagoEsDeUnaConsultaODeUnTratamientoNuncaDeLosDos() {
        Cita cita = citas.agendar(cita(LocalTime.of(11, 0), 80));
        Pago ambos = new Pago(0, 1, 10, HOY, MetodoPago.EFECTIVO, null, cita.getIdCita());
        assertThrows(ReglaNegocioException.class, () -> pagos.registrar(ambos));
        Pago ninguno = new Pago(0, null, 10, HOY, MetodoPago.EFECTIVO, null, null);
        assertThrows(ReglaNegocioException.class, () -> pagos.registrar(ninguno));
    }

    @Test
    void conPagosYaNoSeCambiaElPacienteNiSeBajaElCostoPorDebajoDeLoCobrado() {
        Cita cita = citas.agendar(cita(LocalTime.of(11, 0), 80));
        pagos.registrar(Pago.deConsulta(cita.getIdCita(), 50, HOY, MetodoPago.EFECTIVO, null));

        Cita barata = citaDAO.buscarPorId(cita.getIdCita()).orElseThrow();
        barata.setCostoConsulta(40);
        assertEquals("costoConsulta", assertThrows(ReglaNegocioException.class,
                () -> citas.reprogramar(barata)).getCampo().orElseThrow());

        Cita otroPaciente = citaDAO.buscarPorId(cita.getIdCita()).orElseThrow();
        Paciente jorge = new Paciente();
        jorge.setIdPaciente(2);
        otroPaciente.setPaciente(jorge);
        assertThrows(ReglaNegocioException.class, () -> citas.reprogramar(otroPaciente));

        Cita masTarde = citaDAO.buscarPorId(cita.getIdCita()).orElseThrow();
        masTarde.setHoraInicio(LocalTime.of(12, 0));
        masTarde.setHoraFin(LocalTime.of(12, 30));
        assertDoesNotThrow(() -> citas.reprogramar(masTarde));
    }

    @Test
    void porCobrarTraeConsultasPendientesYTratamientosConSaldo() {
        Cita pendiente = citas.agendar(cita(LocalTime.of(11, 0), 80));
        Cita pagada = citas.agendar(cita(LocalTime.of(12, 0), 80));
        pagos.registrar(Pago.deConsulta(pagada.getIdCita(), 80, HOY, MetodoPago.EFECTIVO, null));
        citas.agendar(cita(LocalTime.of(13, 0), 0));
        Tratamiento endodoncia = tratamientoEnCurso(650);

        List<Cobro> cobros = pagos.porCobrar("quispe");

        assertEquals(2, cobros.size());
        assertEquals(Cobro.Tipo.CONSULTA, cobros.get(0).getTipo());
        assertEquals(pendiente.getIdCita(), cobros.get(0).getId());
        assertEquals(80.00, cobros.get(0).getSaldo());
        assertEquals(Cobro.Tipo.TRATAMIENTO, cobros.get(1).getTipo());
        assertEquals(endodoncia.getIdTratamiento(), cobros.get(1).getId());
        assertTrue(pagos.porCobrar("jorge").isEmpty());
    }

    @Test
    void unTratamientoConSaldoNoSeFinaliza() {
        Tratamiento endodoncia = tratamientoEnCurso(650);
        pagos.registrar(new Pago(0, endodoncia.getIdTratamiento(), 250, HOY, MetodoPago.TARJETA, null));
        iniciarSesion(Rol.ODONTOLOGO, salinas);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> tratamientos.finalizar(endodoncia.getIdTratamiento()));
        assertTrue(error.getMessage().contains("S/ 400.00"), error.getMessage());

        iniciarSesion(Rol.RECEPCION, null);
        pagos.registrar(new Pago(0, endodoncia.getIdTratamiento(), 400, HOY, MetodoPago.EFECTIVO, null));
        iniciarSesion(Rol.ODONTOLOGO, salinas);
        tratamientos.finalizar(endodoncia.getIdTratamiento());
        assertEquals(EstadoTratamiento.FINALIZADO,
                tratamientoDAO.buscarPorId(endodoncia.getIdTratamiento()).orElseThrow().getEstado());
    }

    @Test
    void laDeudaDelPacienteSumaLosSaldosDeSusTratamientos() {
        Tratamiento uno = tratamientoEnCurso(650);
        tratamientoEnCurso(120);
        pagos.registrar(new Pago(0, uno.getIdTratamiento(), 600, HOY, MetodoPago.EFECTIVO, null));

        PagoService.SaldoPendiente deuda = pagos.deudaDePaciente(maria.getIdPaciente());

        assertEquals(170.00, deuda.total());
        assertEquals(2, deuda.tratamientos());
    }

    @Test
    void mercadoPagoNecesitaNumeroDeOperacionYNoSeRegistraDosVeces() {
        Cita cita = citas.agendar(cita(LocalTime.of(11, 0), 80));
        assertThrows(ReglaNegocioException.class,
                () -> pagos.registrar(Pago.deConsulta(cita.getIdCita(), 40, HOY, MetodoPago.MERCADO_PAGO, null)));
        pagos.registrar(Pago.deConsulta(cita.getIdCita(), 40, HOY, MetodoPago.MERCADO_PAGO, "MP-1"));
        assertThrows(ReglaNegocioException.class,
                () -> pagos.registrar(Pago.deConsulta(cita.getIdCita(), 40, HOY, MetodoPago.MERCADO_PAGO, "MP-1")));
    }

    private Cita cita(LocalTime inicio, double costo) {
        return new Cita(0, maria, salinas, HOY, inicio, inicio.plusMinutes(30), "Evaluación", EstadoCita.CONFIRMADA,
                costo);
    }

    /** En una cita ya atendida, sin costo de consulta, creada directo en el DAO. */
    private Tratamiento tratamientoEnCurso(double costo) {
        Cita atendida = citaDAO.crear(new Cita(0, maria, salinas, HOY.minusDays(5), LocalTime.of(9, 0),
                LocalTime.of(9, 30), "Evaluación", EstadoCita.ATENDIDA));
        return tratamientoDAO.crear(new Tratamiento(0, atendida.getIdCita(), "Tratamiento de " + costo, costo,
                EstadoTratamiento.EN_CURSO, atendida.getFecha(), List.of()));
    }

    private static void iniciarSesion(Rol rol, Profesional profesional) {
        Sesion.get().cerrar();
        Sesion.get().iniciar(new Usuario(3, "prueba", "Usuario de prueba", "pbkdf2$1$a$b", rol, true), profesional);
    }
}
