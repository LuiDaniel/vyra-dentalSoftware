package com.vyra.dental.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Tratamiento;

/**
 * Corre contra SQLite en memoria (vyra.bd.url en pom.xml). Cada prueba crea su propio
 * paciente y su cita, así los datos de otras pruebas no interfieren.
 */
class TratamientoPagoDAOSqliteTest {

    private static int contador;

    private final TratamientoDAOSqlite tratamientos = new TratamientoDAOSqlite();
    private final PagoDAOSqlite pagos = new PagoDAOSqlite();
    private Cita cita;

    @BeforeEach
    void preparar() {
        contador++;
        Paciente nuevo = new Paciente();
        nuevo.setNombres("Zulema");
        nuevo.setApellidos("Tratamiento Sql " + contador);
        nuevo.setDni(String.format("9500%04d", contador));
        Paciente paciente = new PacienteDAOSqlite().crear(nuevo);
        Profesional profesional = new ProfesionalDAOSqlite().crear(
                new Profesional(0, null, "Prof" + contador, "Tratamiento Sql", "Endodoncia", null, null));
        cita = new CitaDAOSqlite().crear(new Cita(0, paciente, profesional, LocalDate.of(2030, 5, 6),
                LocalTime.of(9, 0), LocalTime.of(9, 30), "Control", EstadoCita.ATENDIDA));
    }

    @Test
    void guardaElTratamientoConSusPiezas() {
        Tratamiento creado = tratamientos.crear(tratamiento("Endodoncia", 650, 47, 36));

        Tratamiento leido = tratamientos.buscarPorId(creado.getIdTratamiento()).orElseThrow();

        assertTrue(creado.getIdTratamiento() > 0);
        assertEquals("Endodoncia", leido.getDescripcion());
        assertEquals(650, leido.getCosto(), 0.001);
        assertEquals(EstadoTratamiento.EN_CURSO, leido.getEstado());
        assertEquals(LocalDate.of(2030, 5, 6), leido.getFecha());
        assertEquals(List.of(36, 47), leido.getPiezasDentales());
    }

    @Test
    void siUnaPiezaFallaNoQuedaNadaGuardado() {
        // 19 no existe: el CHECK de tratamiento_pieza no lo valida, pero la clave repetida sí falla
        Tratamiento repetido = tratamiento("Con piezas repetidas", 100, 36, 36);

        assertThrows(RuntimeException.class, () -> tratamientos.crear(repetido));

        assertTrue(tratamientos.listarPorCita(cita.getIdCita()).isEmpty());
    }

    @Test
    void listaPorCitaYPorEstado() {
        Tratamiento a = tratamientos.crear(tratamiento("A", 100, 11));
        tratamientos.crear(tratamiento("B", 200));
        a.setEstado(EstadoTratamiento.FINALIZADO);
        tratamientos.actualizar(a);

        assertEquals(2, tratamientos.listarPorCita(cita.getIdCita()).size());
        assertTrue(tratamientos.listarPorEstado(EstadoTratamiento.FINALIZADO).stream()
                .anyMatch(t -> t.getIdTratamiento() == a.getIdTratamiento()));
        assertFalse(tratamientos.listarPorEstado(EstadoTratamiento.EN_CURSO).stream()
                .anyMatch(t -> t.getIdTratamiento() == a.getIdTratamiento()));
    }

    @Test
    void actualizarReemplazaLasPiezas() {
        Tratamiento t = tratamientos.crear(tratamiento("A", 100, 11, 12));
        t.setPiezasDentales(List.of(21));

        assertTrue(tratamientos.actualizar(t));

        assertEquals(List.of(21), tratamientos.buscarPorId(t.getIdTratamiento()).orElseThrow().getPiezasDentales());
    }

    @Test
    void sumaLosPagosYLosListaPorTratamientoYPeriodo() {
        Tratamiento t = tratamientos.crear(tratamiento("Endodoncia", 650, 36));
        assertEquals(0, pagos.totalPagado(t.getIdTratamiento()), 0.001);

        pagos.crear(pago(t, 250, LocalDate.of(2030, 5, 6), MetodoPago.EFECTIVO, null));
        Pago yape = pagos.crear(pago(t, 100.5, LocalDate.of(2030, 6, 1), MetodoPago.YAPE_PLIN, "OP-123"));

        assertEquals(350.5, pagos.totalPagado(t.getIdTratamiento()), 0.001);
        assertEquals(2, pagos.listarPorTratamiento(t.getIdTratamiento()).size());
        assertEquals(yape.getIdPago(), pagos.listarPorTratamiento(t.getIdTratamiento()).get(1).getIdPago());
        List<Pago> junio = pagos.listarPorPeriodo(LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 30));
        assertEquals(1, junio.size());
        assertEquals("OP-123", junio.get(0).getNumeroOperacion());
        assertEquals(MetodoPago.YAPE_PLIN, junio.get(0).getMetodoPago());
    }

    @Test
    void laBaseRechazaMontosNoPositivos() {
        Tratamiento t = tratamientos.crear(tratamiento("A", 100));

        assertThrows(RuntimeException.class,
                () -> pagos.crear(pago(t, 0, LocalDate.of(2030, 5, 6), MetodoPago.EFECTIVO, null)));
    }

    private Tratamiento tratamiento(String descripcion, double costo, Integer... piezas) {
        return new Tratamiento(0, cita.getIdCita(), descripcion, costo, EstadoTratamiento.EN_CURSO,
                LocalDate.of(2030, 5, 6), List.of(piezas));
    }

    private static Pago pago(Tratamiento t, double monto, LocalDate fecha, MetodoPago metodo, String operacion) {
        return new Pago(0, t.getIdTratamiento(), monto, fecha, metodo, operacion);
    }
}
