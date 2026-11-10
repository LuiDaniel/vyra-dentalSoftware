package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import com.vyra.dental.service.ReporteService.ReporteCitas;
import com.vyra.dental.service.ReporteService.ReporteHistorial;
import com.vyra.dental.service.ReporteService.ReporteIngresos;

class ReporteServiceTest {

    private static final LocalDate HOY = LocalDate.of(2025, 1, 15);
    private static final Clock RELOJ = Clock.fixed(HOY.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    private final Profesional salinas = new Profesional(1, null, "Marco", "Salinas", null, null, null);
    private final Profesional nunez = new Profesional(2, null, "Patricia", "Núñez", null, null, null);
    private final Paciente maria = paciente(1, "María");

    private CitaDAOMemoria citaDAO;
    private PagoDAOMemoria pagoDAO;
    private TratamientoDAOMemoria tratamientoDAO;
    private ReporteService servicio;

    @BeforeEach
    void preparar() {
        citaDAO = new CitaDAOMemoria();
        pagoDAO = new PagoDAOMemoria();
        tratamientoDAO = new TratamientoDAOMemoria(citaDAO, pagoDAO);
        servicio = new ReporteService(citaDAO, pagoDAO, tratamientoDAO, RELOJ);
        Sesion.get().iniciar(new Usuario(1, "admin", "Administrador", "x", Rol.ADMIN, true), null);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void citasDelPeriodoConSusTotalesYLaAsistencia() {
        cita(salinas, HOY.minusDays(3), EstadoCita.ATENDIDA);
        cita(salinas, HOY.minusDays(2), EstadoCita.ATENDIDA);
        cita(salinas, HOY.minusDays(2), EstadoCita.ATENDIDA);
        cita(salinas, HOY.minusDays(1), EstadoCita.CONFIRMADA);   // no vino
        cita(salinas, HOY.minusDays(1), EstadoCita.CANCELADA);    // no cuenta para la asistencia
        cita(salinas, HOY, EstadoCita.PENDIENTE);                 // todavía puede venir
        cita(nunez, HOY.minusDays(1), EstadoCita.ATENDIDA);
        cita(salinas, HOY.minusDays(10), EstadoCita.ATENDIDA);    // fuera del periodo

        ReporteCitas todas = servicio.citas(HOY.minusDays(3), HOY, null);
        ReporteCitas deSalinas = servicio.citas(HOY.minusDays(3), HOY, salinas.getIdProfesional());

        assertEquals(7, todas.total());
        assertEquals(4, todas.atendidas());
        assertEquals(1, todas.canceladas());
        assertEquals(2, todas.porAtender());
        assertEquals(80, todas.asistencia());   // 4 de 5
        assertEquals(6, deSalinas.total());
        assertEquals(75, deSalinas.asistencia()); // 3 de 4
    }

    @Test
    void sinCitasQueMedirLaAsistenciaEsNull() {
        cita(salinas, HOY.plusDays(1), EstadoCita.PENDIENTE);

        assertNull(servicio.citas(HOY, HOY.plusDays(1), null).asistencia());
    }

    @Test
    void historialDelPacienteConTotales() {
        Cita cita = cita(salinas, HOY.minusDays(3), EstadoCita.ATENDIDA);
        Tratamiento endodoncia = tratamientoDAO.crear(new Tratamiento(0, cita.getIdCita(), "Endodoncia", 650,
                EstadoTratamiento.EN_CURSO, HOY.minusDays(3), List.of(36)));
        tratamientoDAO.crear(new Tratamiento(0, cita.getIdCita(), "Resina", 100.10,
                EstadoTratamiento.FINALIZADO, HOY.minusDays(3), List.of(14)));
        pagoDAO.crear(new Pago(0, endodoncia.getIdTratamiento(), 250.20, HOY, MetodoPago.EFECTIVO, null));

        ReporteHistorial historial = servicio.historialPaciente(maria.getIdPaciente());

        assertEquals(2, historial.tratamientos().size());
        assertEquals(750.10, historial.costo());
        assertEquals(250.20, historial.pagado());
        assertEquals(499.90, historial.saldo());
    }

    @Test
    void ingresosPorMetodoYPorDia() {
        Cita cita = cita(salinas, HOY.minusDays(3), EstadoCita.ATENDIDA);
        Tratamiento ortodoncia = tratamientoDAO.crear(new Tratamiento(0, cita.getIdCita(), "Ortodoncia", 4500,
                EstadoTratamiento.EN_CURSO, HOY.minusDays(3), List.of()));
        int id = ortodoncia.getIdTratamiento();
        pagoDAO.crear(new Pago(0, id, 0.10, HOY.minusDays(2), MetodoPago.EFECTIVO, null));
        pagoDAO.crear(new Pago(0, id, 0.20, HOY.minusDays(2), MetodoPago.EFECTIVO, null));
        pagoDAO.crear(new Pago(0, id, 250, HOY, MetodoPago.TARJETA, null));
        pagoDAO.crear(new Pago(0, id, 999, HOY.minusDays(30), MetodoPago.TARJETA, null));

        ReporteIngresos ingresos = servicio.ingresos(HOY.minusDays(2), HOY);

        assertEquals(250.30, ingresos.total());
        assertEquals(3, ingresos.pagos());
        assertEquals(Map.of(MetodoPago.EFECTIVO, 0.30, MetodoPago.TARJETA, 250.0), ingresos.porMetodo());
        assertEquals(Map.of(MetodoPago.EFECTIVO, 2, MetodoPago.TARJETA, 1), ingresos.pagosPorMetodo());
        assertEquals(List.of(0.30, 0.0, 250.0), List.copyOf(ingresos.porDia().values()));
        assertEquals(ingresos.porDia(), servicio.ingresosPorDia(HOY.minusDays(2), HOY));
    }

    @Test
    void rechazaRangosInvalidosYOtrosRoles() {
        assertEquals("hasta", assertThrows(ReglaNegocioException.class,
                () -> servicio.ingresos(HOY, HOY.minusDays(1))).getCampo().orElseThrow());
        assertEquals("hasta", assertThrows(ReglaNegocioException.class,
                () -> servicio.citas(HOY.minusYears(2), HOY, null)).getCampo().orElseThrow());

        Sesion.get().iniciar(new Usuario(2, "rocio", "Rocío", "x", Rol.RECEPCION, true), null);
        assertThrows(ReglaNegocioException.class, () -> servicio.ingresos(HOY, HOY));
    }

    private Cita cita(Profesional profesional, LocalDate fecha, EstadoCita estado) {
        return citaDAO.crear(new Cita(0, maria, profesional, fecha, LocalTime.of(9, 0), LocalTime.of(9, 30),
                "Control", estado));
    }

    private static Paciente paciente(int id, String nombres) {
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(id);
        paciente.setNombres(nombres);
        paciente.setApellidos("Quispe");
        paciente.setDni("7248193" + id);
        return paciente;
    }
}
