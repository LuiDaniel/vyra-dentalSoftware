package com.vyra.dental.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.PagoResumen;

/**
 * Contrato de acceso a datos para Pago.
 */
public interface PagoDAO {
    Pago crear(Pago pago);
    Optional<Pago> buscarPorId(int idPago);
    List<Pago> listarTodos();

    /** Pagos del tratamiento, del más reciente al más antiguo. */
    List<Pago> listarPorTratamiento(int idTratamiento);

    /** Pagos con fecha entre desde y hasta (ambas incluidas). */
    List<Pago> listarPorPeriodo(LocalDate desde, LocalDate hasta);

    /** Suma de los pagos del tratamiento (0 si no tiene). */
    double totalPagado(int idTratamiento);

    /** Pagos del periodo con concepto y paciente, del más reciente al más antiguo. */
    List<PagoResumen> resumenesEntre(LocalDate desde, LocalDate hasta);

    /** Total cobrado por método en el periodo (docs/03-modelo-base-datos.md §5.5); sin los métodos sin pagos. */
    Map<MetodoPago, Double> totalesPorMetodo(LocalDate desde, LocalDate hasta);

    boolean actualizar(Pago pago);
    boolean eliminar(int idPago);
}
