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
import java.util.Map;

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

class PagoServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 29);

    private TratamientoDAOMemoria tratamientoDAO;
    private PagoService servicio;
    private Paciente maria;
    private Tratamiento endodoncia;

    @BeforeEach
    void preparar() {
        PacienteDAOMemoria pacienteDAO = new PacienteDAOMemoria();
        CitaDAOMemoria citaDAO = new CitaDAOMemoria();
        tratamientoDAO = new TratamientoDAOMemoria();
        Clock reloj = Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"), ZONA);
        servicio = new PagoService(new PagoDAOMemoria(), tratamientoDAO, citaDAO, reloj);
        maria = pacienteDAO.crear(new Paciente(0, "María", "Quispe", "72481935", LocalDate.of(1990, 1, 1), null, null,
                null, null, null, true, LocalDate.of(2026, 1, 1)));
        Profesional salinas = new Profesional(1, 5, "Marco", "Salinas Vega", "Endodoncia", null, null);
        Cita cita = citaDAO.crear(new Cita(0, maria, salinas, HOY, LocalTime.of(9, 0), LocalTime.of(9, 30), "Control",
                EstadoCita.ATENDIDA));
        endodoncia = tratamientoDAO.crear(new Tratamiento(0, cita.getIdCita(), "Endodoncia", 650,
                EstadoTratamiento.EN_CURSO, HOY, List.of(36)));
        iniciarSesionComo(Rol.RECEPCION);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void sinPagosElSaldoEsElCosto() {
        assertEquals(650, servicio.saldo(endodoncia.getIdTratamiento()), 0.001);
    }

    @Test
    void elSaldoEsCostoMenosLaSumaDeLosPagos() {
        servicio.registrar(pago(250.50, HOY));
        servicio.registrar(pago(100.25, HOY));

        assertEquals(350.75, servicio.pagado(endodoncia.getIdTratamiento()), 0.001);
        assertEquals(299.25, servicio.saldo(endodoncia.getIdTratamiento()), 0.001);
    }

    @Test
    void sePuedeCobrarEnPartesHastaCompletar() {
        servicio.registrar(pago(250, HOY));
        servicio.registrar(pago(400, HOY));

        assertEquals(0, servicio.saldo(endodoncia.getIdTratamiento()), 0.001);
    }

    @Test
    void noSePuedePagarMasQueElSaldo() {
        servicio.registrar(pago(250, HOY));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(pago(400.01, HOY)));

        assertEquals("monto", error.getCampo().orElseThrow());
        assertTrue(error.getMessage().contains("S/ 400.00"), error.getMessage());
        assertEquals(400, servicio.saldo(endodoncia.getIdTratamiento()), 0.001);
    }

    @Test
    void unTratamientoPagadoNoAceptaMasPagos() {
        servicio.registrar(pago(650, HOY));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(pago(1, HOY)));

        assertEquals("monto", error.getCampo().orElseThrow());
    }

    @Test
    void noAceptaFechasFuturas() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(pago(100, HOY.plusDays(1))));

        assertEquals("fechaPago", error.getCampo().orElseThrow());
    }

    @Test
    void elMontoDebeSerMayorACero() {
        assertThrows(RuntimeException.class, () -> servicio.registrar(pago(0, HOY)));
        assertThrows(RuntimeException.class, () -> servicio.registrar(pago(-5, HOY)));
    }

    @Test
    void exigeUnTratamientoExistente() {
        Pago pago = pago(100, HOY);
        pago.setIdTratamiento(999);

        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(pago));
    }

    @Test
    void elNumeroDeOperacionVacioSeGuardaComoNull() {
        Pago pago = pago(100, HOY);
        pago.setNumeroOperacion("   ");

        assertEquals(null, servicio.registrar(pago).getNumeroOperacion());
    }

    @Test
    void sumaLoCobradoEnUnPeriodoYPorMetodo() {
        servicio.registrar(pago(100, HOY.minusDays(40)));
        servicio.registrar(pago(50, HOY));
        Pago yape = pago(30, HOY);
        yape.setMetodoPago(MetodoPago.YAPE_PLIN);
        servicio.registrar(yape);

        LocalDate desde = HOY.minusDays(7);
        assertEquals(80, servicio.cobradoEntre(desde, HOY), 0.001);
        Map<MetodoPago, Double> porMetodo = servicio.cobradoPorMetodo(desde, HOY);
        assertEquals(50, porMetodo.get(MetodoPago.EFECTIVO), 0.001);
        assertEquals(30, porMetodo.get(MetodoPago.YAPE_PLIN), 0.001);
        assertEquals(0, porMetodo.get(MetodoPago.TARJETA), 0.001);
        assertEquals(2, servicio.pagosEntre(desde, HOY).size());
    }

    @Test
    void elSaldoPendienteSumaSoloLosTratamientosConDeuda() {
        Tratamiento limpieza = tratamientoDAO.crear(new Tratamiento(0, endodoncia.getIdCita(), "Limpieza", 80,
                EstadoTratamiento.FINALIZADO, HOY, List.of()));
        servicio.registrar(pago(250, HOY));
        Pago completo = pago(80, HOY);
        completo.setIdTratamiento(limpieza.getIdTratamiento());
        servicio.registrar(completo);

        PagoService.SaldoPendiente pendiente = servicio.saldoPendiente();

        assertEquals(400, pendiente.total(), 0.001);
        assertEquals(1, pendiente.tratamientos());
    }

    @Test
    void listaLosPagosDeUnPacienteConSuTratamiento() {
        servicio.registrar(pago(100, HOY));

        List<PagoDetalle> pagos = servicio.pagosDelPaciente(maria.getIdPaciente());

        assertEquals(1, pagos.size());
        assertEquals("Endodoncia", pagos.get(0).tratamiento().getDescripcion());
        assertTrue(servicio.pagosDelPaciente(999).isEmpty());
    }

    @Test
    void elOdontologoNoTieneAcceso() {
        iniciarSesionComo(Rol.ODONTOLOGO);

        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(pago(100, HOY)));
        assertThrows(ReglaNegocioException.class, () -> servicio.saldo(endodoncia.getIdTratamiento()));
        assertThrows(ReglaNegocioException.class, servicio::saldoPendiente);
    }

    @Test
    void elAdministradorTambienCobra() {
        iniciarSesionComo(Rol.ADMIN);

        servicio.registrar(pago(100, HOY));

        assertEquals(550, servicio.saldo(endodoncia.getIdTratamiento()), 0.001);
    }

    private Pago pago(double monto, LocalDate fecha) {
        return new Pago(0, endodoncia.getIdTratamiento(), monto, fecha, MetodoPago.EFECTIVO, null);
    }

    private static void iniciarSesionComo(Rol rol) {
        Sesion.get().iniciar(new Usuario(1, "usuario", "Usuario de prueba", "pbkdf2$1$a$b", rol, true), null);
    }
}
