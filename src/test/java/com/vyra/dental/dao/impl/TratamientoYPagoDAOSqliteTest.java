package com.vyra.dental.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.PagoResumen;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.model.TratamientoResumen;

/**
 * Corre contra SQLite en memoria (vyra.bd.url en pom.xml). Cada prueba crea su
 * propio paciente y usa fechas propias, así los datos de otras pruebas no interfieren.
 */
class TratamientoYPagoDAOSqliteTest {

    private static int contador;

    private final TratamientoDAOSqlite tratamientos = new TratamientoDAOSqlite();
    private final PagoDAOSqlite pagos = new PagoDAOSqlite();
    private Paciente paciente;
    private Cita cita;
    /** Un año distinto por prueba, para que los totales por periodo no se mezclen. */
    private LocalDate fecha;

    @BeforeEach
    void preparar() {
        contador++;
        fecha = LocalDate.of(2010 + contador, 3, 10);
        Paciente nuevo = new Paciente();
        nuevo.setNombres("Teodora");
        nuevo.setApellidos("Pagós Sql " + contador);
        nuevo.setDni(String.format("9500%04d", contador));
        paciente = new PacienteDAOSqlite().crear(nuevo);
        Profesional profesional = new ProfesionalDAOSqlite().crear(
                new Profesional(0, null, "Prof" + contador, "Pago Sql", "Endodoncia", null, null));
        cita = new CitaDAOSqlite().crear(new Cita(0, paciente, profesional, fecha, LocalTime.of(9, 0),
                LocalTime.of(10, 0), "Control", EstadoCita.ATENDIDA));
    }

    @Test
    void guardaElTratamientoConSusPiezasYLasReemplazaAlActualizar() {
        Tratamiento creado = tratamientos.crear(tratamiento("Endodoncia", 650, List.of(47, 36)));

        Tratamiento leido = tratamientos.buscarPorId(creado.getIdTratamiento()).orElseThrow();
        assertEquals(List.of(36, 47), leido.getPiezasDentales());
        assertEquals(fecha, leido.getFecha());

        leido.setPiezasDentales(List.of(11));
        leido.setCosto(700);
        assertTrue(tratamientos.actualizar(leido));
        Tratamiento actualizado = tratamientos.buscarPorId(creado.getIdTratamiento()).orElseThrow();
        assertEquals(List.of(11), actualizado.getPiezasDentales());
        assertEquals(700, actualizado.getCosto());
        assertEquals(List.of(creado.getIdTratamiento()),
                tratamientos.listarPorPaciente(paciente.getIdPaciente()).stream().map(Tratamiento::getIdTratamiento).toList());
    }

    @Test
    void siUnaPiezaFallaNoSeGuardaElTratamiento() {
        // tratamiento_pieza no admite la misma pieza dos veces (clave primaria)
        Tratamiento repetida = tratamiento("Resina", 100, List.of(14, 14));

        assertThrows(RuntimeException.class, () -> tratamientos.crear(repetida));

        assertTrue(tratamientos.listarPorPaciente(paciente.getIdPaciente()).isEmpty());
        // La conexión compartida vuelve a guardar normalmente
        tratamientos.crear(tratamiento("Resina", 100, List.of(14)));
        assertEquals(1, tratamientos.listarPorPaciente(paciente.getIdPaciente()).size());
    }

    @Test
    void elResumenTraeElPacienteLoPagadoYElSaldo() {
        Tratamiento endodoncia = tratamientos.crear(tratamiento("Endodoncia", 650, List.of(36)));
        pagos.crear(new Pago(0, endodoncia.getIdTratamiento(), 250, fecha, MetodoPago.YAPE_PLIN, "123"));
        pagos.crear(new Pago(0, endodoncia.getIdTratamiento(), 100.5, fecha, MetodoPago.EFECTIVO, null));

        TratamientoResumen resumen = tratamientos.buscarResumen(endodoncia.getIdTratamiento()).orElseThrow();

        assertEquals(paciente.getIdPaciente(), resumen.getPaciente().getIdPaciente());
        assertEquals("Pago Sql", resumen.getProfesional().getApellidos());
        assertEquals(350.5, resumen.getPagado());
        assertEquals(299.5, resumen.getSaldo());
        assertEquals(350.5, pagos.totalPagado(endodoncia.getIdTratamiento()));
        // Búsqueda por nombre sin tildes y por estado
        String apellido = "pagos sql " + contador;
        assertEquals(1, tratamientos.buscarResumenes(apellido, EstadoTratamiento.EN_CURSO).size());
        assertEquals(0, tratamientos.buscarResumenes(apellido, EstadoTratamiento.FINALIZADO).size());
        assertEquals(1, tratamientos.resumenesDePaciente(paciente.getIdPaciente()).size());
    }

    @Test
    void pagosDelPeriodoConConceptoYTotalesPorMetodo() {
        Tratamiento endodoncia = tratamientos.crear(tratamiento("Endodoncia pieza 36", 650, List.of(36)));
        pagos.crear(new Pago(0, endodoncia.getIdTratamiento(), 250, fecha, MetodoPago.YAPE_PLIN, null));
        pagos.crear(new Pago(0, endodoncia.getIdTratamiento(), 100, fecha.plusDays(1), MetodoPago.EFECTIVO, null));
        pagos.crear(new Pago(0, endodoncia.getIdTratamiento(), 50, fecha.plusDays(1), MetodoPago.EFECTIVO, null));
        pagos.crear(new Pago(0, endodoncia.getIdTratamiento(), 10, fecha.plusMonths(1), MetodoPago.TARJETA, null));

        List<PagoResumen> delPeriodo = pagos.resumenesEntre(fecha, fecha.plusDays(1));

        assertEquals(List.of(50.0, 100.0, 250.0), delPeriodo.stream().map(r -> r.getPago().getMonto()).toList());
        assertEquals("Endodoncia pieza 36", delPeriodo.get(0).getConcepto());
        assertEquals(paciente.getNombreCompleto(), delPeriodo.get(0).getPaciente().getNombreCompleto());
        assertEquals(Map.of(MetodoPago.YAPE_PLIN, 250.0, MetodoPago.EFECTIVO, 150.0),
                pagos.totalesPorMetodo(fecha, fecha.plusDays(1)));
        assertEquals(3, pagos.listarPorPeriodo(fecha, fecha.plusDays(1)).size());
        assertEquals(4, pagos.listarPorTratamiento(endodoncia.getIdTratamiento()).size());
    }

    private Tratamiento tratamiento(String descripcion, double costo, List<Integer> piezas) {
        return new Tratamiento(0, cita.getIdCita(), descripcion, costo, EstadoTratamiento.EN_CURSO, fecha, piezas);
    }
}
