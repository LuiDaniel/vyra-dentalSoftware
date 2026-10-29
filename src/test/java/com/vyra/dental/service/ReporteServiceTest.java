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

class ReporteServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 29);

    private CitaDAOMemoria citaDAO;
    private PagoDAOMemoria pagoDAO;
    private TratamientoDAOMemoria tratamientoDAO;
    private ReporteService servicio;
    private Paciente maria;
    private Profesional salinas;
    private Profesional rojas;

    @BeforeEach
    void preparar() {
        PacienteDAOMemoria pacienteDAO = new PacienteDAOMemoria();
        citaDAO = new CitaDAOMemoria();
        pagoDAO = new PagoDAOMemoria();
        tratamientoDAO = new TratamientoDAOMemoria();
        Clock reloj = Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"), ZoneId.of("America/Lima"));
        TratamientoService tratamientos = new TratamientoService(tratamientoDAO, citaDAO, pagoDAO, reloj);
        PagoService pagos = new PagoService(pagoDAO, tratamientoDAO, citaDAO, reloj);
        servicio = new ReporteService(citaDAO, pagoDAO, tratamientos, pagos);
        maria = pacienteDAO.crear(new Paciente(0, "María", "Quispe", "72481935", LocalDate.of(1990, 1, 1), null, null,
                null, null, null, true, LocalDate.of(2026, 1, 1)));
        salinas = new Profesional(1, 5, "Marco", "Salinas Vega", "Endodoncia", null, null);
        rojas = new Profesional(2, 6, "Ana", "Rojas", "Ortodoncia", null, null);
        Sesion.get().iniciar(new Usuario(1, "admin", "Admin", "pbkdf2$1$a$b", Rol.ADMIN, true), null);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void filtraLasCitasPorRangoYProfesionalYLasOrdena() {
        cita(salinas, HOY, LocalTime.of(10, 0), EstadoCita.ATENDIDA);
        cita(salinas, HOY, LocalTime.of(9, 0), EstadoCita.ATENDIDA);
        cita(rojas, HOY, LocalTime.of(9, 0), EstadoCita.PENDIENTE);
        cita(salinas, HOY.plusDays(10), LocalTime.of(9, 0), EstadoCita.PENDIENTE);

        List<Cita> todas = servicio.citas(HOY, HOY, null);
        List<Cita> deSalinas = servicio.citas(HOY, HOY.plusDays(10), 1);

        assertEquals(3, todas.size());
        assertEquals(3, deSalinas.size());
        assertEquals(LocalTime.of(9, 0), deSalinas.get(0).getHoraInicio());
    }

    @Test
    void resumeLasCitasYCalculaLaAsistenciaSinContarLasCanceladas() {
        cita(salinas, HOY, LocalTime.of(8, 0), EstadoCita.ATENDIDA);
        cita(salinas, HOY, LocalTime.of(9, 0), EstadoCita.ATENDIDA);
        cita(salinas, HOY, LocalTime.of(10, 0), EstadoCita.ATENDIDA);
        cita(salinas, HOY, LocalTime.of(11, 0), EstadoCita.CONFIRMADA);
        cita(salinas, HOY, LocalTime.of(12, 0), EstadoCita.CANCELADA);

        ReporteService.ResumenCitas resumen = servicio.resumir(servicio.citas(HOY, HOY, null));

        assertEquals(5, resumen.total());
        assertEquals(3, resumen.atendidas());
        assertEquals(1, resumen.canceladas());
        assertEquals(1, resumen.pendientes());
        assertEquals(75, resumen.asistencia());
    }

    @Test
    void sinCitasLaAsistenciaEsCero() {
        assertEquals(0, servicio.resumir(List.of()).asistencia());
    }

    @Test
    void sumaLosIngresosPorMetodoYPorDia() {
        Tratamiento t = tratamiento(maria, 1000);
        pagar(t, 100.10, HOY, MetodoPago.EFECTIVO);
        pagar(t, 50.20, HOY, MetodoPago.YAPE_PLIN);
        pagar(t, 200, HOY.minusDays(2), MetodoPago.EFECTIVO);
        pagar(t, 300, HOY.minusDays(40), MetodoPago.TARJETA);

        ReporteService.Ingresos ingresos = servicio.ingresos(HOY.minusDays(7), HOY);

        assertEquals(350.30, ingresos.total(), 0.001);
        assertEquals(300.10, ingresos.porMetodo().get(MetodoPago.EFECTIVO), 0.001);
        assertEquals(50.20, ingresos.porMetodo().get(MetodoPago.YAPE_PLIN), 0.001);
        assertEquals(0, ingresos.porMetodo().get(MetodoPago.TARJETA), 0.001);
        assertEquals(2, ingresos.porDia().size());
        assertEquals(150.30, ingresos.porDia().get(HOY), 0.001);
        assertEquals(3, ingresos.pagos().size());
    }

    @Test
    void elHistorialDelPacienteTraeCostoPagadoYSaldo() {
        Tratamiento t = tratamiento(maria, 650);
        pagar(t, 250, HOY, MetodoPago.EFECTIVO);

        List<TratamientoResumen> historial = servicio.historialPaciente(maria.getIdPaciente());

        assertEquals(1, historial.size());
        assertEquals(250, historial.get(0).pagado(), 0.001);
        assertEquals(400, historial.get(0).saldo(), 0.001);
    }

    @Test
    void rechazaRangosInvalidos() {
        assertThrows(ReglaNegocioException.class, () -> servicio.citas(HOY, HOY.minusDays(1), null));
        assertThrows(ReglaNegocioException.class, () -> servicio.ingresos(null, HOY));
    }

    @Test
    void soloElAdministradorGeneraReportes() {
        for (Rol rol : new Rol[] {Rol.RECEPCION, Rol.ODONTOLOGO}) {
            Sesion.get().iniciar(new Usuario(2, "otro", "Otro", "pbkdf2$1$a$b", rol, true), null);

            assertThrows(ReglaNegocioException.class, () -> servicio.citas(HOY, HOY, null));
            assertThrows(ReglaNegocioException.class, () -> servicio.ingresos(HOY, HOY));
            assertThrows(ReglaNegocioException.class, () -> servicio.historialPaciente(maria.getIdPaciente()));
        }
        assertTrue(!servicio.puedeAcceder());
    }

    private Cita cita(Profesional profesional, LocalDate fecha, LocalTime hora, EstadoCita estado) {
        return citaDAO.crear(new Cita(0, maria, profesional, fecha, hora, hora.plusMinutes(30), "Control", estado));
    }

    private Tratamiento tratamiento(Paciente paciente, double costo) {
        Cita cita = cita(salinas, HOY, LocalTime.of(15, 0), EstadoCita.ATENDIDA);
        return tratamientoDAO.crear(new Tratamiento(0, cita.getIdCita(), "Endodoncia", costo,
                EstadoTratamiento.EN_CURSO, HOY, List.of(36)));
    }

    private void pagar(Tratamiento t, double monto, LocalDate fecha, MetodoPago metodo) {
        pagoDAO.crear(new Pago(0, t.getIdTratamiento(), monto, fecha, metodo, null));
    }
}
