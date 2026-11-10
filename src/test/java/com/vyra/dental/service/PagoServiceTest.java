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
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.dao.memoria.CitaDAOMemoria;
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
import com.vyra.dental.service.PagoService.Periodo;
import com.vyra.dental.service.PagoService.Rango;

class PagoServiceTest {

    /**
     * Reloj fijo en el pasado: "hoy" es el miércoles 15 de enero de 2025. Tiene
     * que ser pasado porque @PastOrPresent del modelo compara con el reloj real.
     */
    private static final LocalDate HOY = LocalDate.of(2025, 1, 15);
    private static final Clock RELOJ = Clock.fixed(HOY.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    private PagoDAOMemoria pagoDAO;
    private TratamientoDAOMemoria tratamientoDAO;
    private PagoService servicio;
    private Tratamiento endodoncia;

    @BeforeEach
    void preparar() {
        CitaDAOMemoria citaDAO = new CitaDAOMemoria();
        pagoDAO = new PagoDAOMemoria();
        tratamientoDAO = new TratamientoDAOMemoria(citaDAO, pagoDAO);
        servicio = new PagoService(pagoDAO, tratamientoDAO, RELOJ);

        Paciente maria = new Paciente();
        maria.setIdPaciente(1);
        maria.setNombres("María");
        maria.setApellidos("Quispe");
        maria.setDni("72481935");
        Profesional salinas = new Profesional(1, null, "Marco", "Salinas", "Endodoncia", null, null);
        Cita cita = citaDAO.crear(new Cita(0, maria, salinas, HOY.minusDays(10), LocalTime.of(9, 0),
                LocalTime.of(10, 0), "Endodoncia", EstadoCita.ATENDIDA));
        endodoncia = tratamientoDAO.crear(new Tratamiento(0, cita.getIdCita(), "Endodoncia pieza 36", 650,
                EstadoTratamiento.EN_CURSO, HOY.minusDays(10), List.of(36)));
        iniciarSesion(Rol.RECEPCION);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    // Pruebas obligatorias de docs/09-plan-implementacion.md, fase 6

    @Test
    void elSaldoSeCalculaBien() {
        assertEquals(650.00, servicio.saldo(endodoncia.getIdTratamiento()));

        servicio.registrar(pago(250, HOY.minusDays(10)));
        servicio.registrar(pago(0.10, HOY));
        servicio.registrar(pago(0.20, HOY));

        // En céntimos: sin errores de redondeo de double (0.1 + 0.2)
        assertEquals(399.70, servicio.saldo(endodoncia.getIdTratamiento()));
    }

    @Test
    void noSePuedePagarMasQueElSaldo() {
        servicio.registrar(pago(250, HOY));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(pago(400.01, HOY)));

        assertEquals("monto", error.getCampo().orElseThrow());
        assertEquals("El monto supera el saldo pendiente (S/ 400.00).", error.getMessage());
        assertEquals(1, servicio.pagosDe(endodoncia.getIdTratamiento()).size());
        // El saldo exacto sí se acepta, y después ya no se puede pagar nada más
        servicio.registrar(pago(400, HOY));
        assertEquals(0, servicio.saldo(endodoncia.getIdTratamiento()));
        ReglaNegocioException pagado = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(pago(1, HOY)));
        assertEquals("idTratamiento", pagado.getCampo().orElseThrow());
    }

    @Test
    void noSeAceptanFechasFuturas() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(pago(100, HOY.plusDays(1))));

        assertEquals("fechaPago", error.getCampo().orElseThrow());
        assertTrue(servicio.pagosDe(endodoncia.getIdTratamiento()).isEmpty());
    }

    // Más reglas

    @Test
    void sinFechaSeUsaHoyYElNumeroDeOperacionVacioQuedaNull() {
        Pago pago = pago(100, null);
        pago.setNumeroOperacion("   ");

        servicio.registrar(pago);

        Pago guardado = servicio.pagosDe(endodoncia.getIdTratamiento()).get(0);
        assertEquals(HOY, guardado.getFechaPago());
        assertNull(guardado.getNumeroOperacion());
    }

    @Test
    void elMontoDebeSerPositivoYConHastaDosDecimales() {
        assertEquals("monto", assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(pago(0, HOY))).getCampo().orElseThrow());
        assertEquals("monto", assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(pago(-5, HOY))).getCampo().orElseThrow());
        assertEquals("monto", assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(pago(10.005, HOY))).getCampo().orElseThrow());
    }

    @Test
    void necesitaUnTratamientoYUnMetodo() {
        Pago sinTratamiento = pago(10, HOY);
        sinTratamiento.setIdTratamiento(999);
        Pago sinMetodo = pago(10, HOY);
        sinMetodo.setMetodoPago(null);

        assertEquals("idTratamiento", assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(sinTratamiento)).getCampo().orElseThrow());
        assertEquals("metodoPago", assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(sinMetodo)).getCampo().orElseThrow());
    }

    @Test
    void elOdontologoNoRegistraPagos() {
        iniciarSesion(Rol.ODONTOLOGO);

        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(pago(100, HOY)));
        iniciarSesion(Rol.ADMIN);
        servicio.registrar(pago(100, HOY));
        assertEquals(550, servicio.saldo(endodoncia.getIdTratamiento()));
    }

    @Test
    void totalesDelPeriodoYSaldoPendiente() {
        Tratamiento limpieza = tratamientoDAO.crear(new Tratamiento(0, endodoncia.getIdCita(), "Limpieza", 120,
                EstadoTratamiento.FINALIZADO, HOY, List.of()));
        servicio.registrar(pago(250, HOY.minusDays(40)));
        servicio.registrar(pago(100, HOY, MetodoPago.EFECTIVO));
        Pago limpiezaPagada = pago(120, HOY, MetodoPago.YAPE_PLIN);
        limpiezaPagada.setIdTratamiento(limpieza.getIdTratamiento());
        servicio.registrar(limpiezaPagada);

        Rango mes = servicio.rango(Periodo.MES);
        assertEquals(220, servicio.totalCobrado(mes));
        assertEquals(Map.of(MetodoPago.EFECTIVO, 100.0, MetodoPago.YAPE_PLIN, 120.0), servicio.totalesPorMetodo(mes));
        assertEquals(250, servicio.totalCobrado(servicio.rangoAnterior(Periodo.MES)));
        // Solo la endodoncia debe algo: 650 − 250 − 100
        assertEquals(new PagoService.SaldoPendiente(300, 1), servicio.saldoPendiente());
    }

    @Test
    void rangosDeLosPeriodos() {
        assertEquals(new Rango(HOY, HOY), servicio.rango(Periodo.HOY));
        assertEquals(new Rango(HOY.minusDays(1), HOY.minusDays(1)), servicio.rangoAnterior(Periodo.HOY));
        // El 15 de enero de 2025 es miércoles: la semana va del lunes 13 al domingo 19
        assertEquals(new Rango(LocalDate.of(2025, 1, 13), LocalDate.of(2025, 1, 19)), servicio.rango(Periodo.SEMANA));
        assertEquals(new Rango(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12)),
                servicio.rangoAnterior(Periodo.SEMANA));
        assertEquals(new Rango(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)), servicio.rango(Periodo.MES));
        assertEquals(new Rango(LocalDate.of(2024, 12, 1), LocalDate.of(2024, 12, 31)),
                servicio.rangoAnterior(Periodo.MES));
    }

    private Pago pago(double monto, LocalDate fecha) {
        return pago(monto, fecha, MetodoPago.YAPE_PLIN);
    }

    private Pago pago(double monto, LocalDate fecha, MetodoPago metodo) {
        return new Pago(0, endodoncia.getIdTratamiento(), monto, fecha, metodo, null);
    }

    private static void iniciarSesion(Rol rol) {
        Sesion.get().iniciar(new Usuario(3, "prueba", "Usuario de prueba", "pbkdf2$1$a$b", rol, true), null);
    }
}
