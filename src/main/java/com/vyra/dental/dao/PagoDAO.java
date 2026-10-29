package com.vyra.dental.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.model.Pago;

/**
 * Contrato de acceso a datos para Pago.
 */
public interface PagoDAO {
    Pago crear(Pago pago);
    Optional<Pago> buscarPorId(int idPago);
    /** Del más reciente al más antiguo. */
    List<Pago> listarTodos();
    /** Del más antiguo al más reciente. */
    List<Pago> listarPorTratamiento(int idTratamiento);
    /** Pagos con fecha entre desde y hasta (ambas incluidas), del más reciente al más antiguo. */
    List<Pago> listarPorPeriodo(LocalDate desde, LocalDate hasta);
    /** Suma de los pagos del tratamiento (0 si no tiene). */
    double totalPagado(int idTratamiento);
    boolean actualizar(Pago pago);
    boolean eliminar(int idPago);
}
