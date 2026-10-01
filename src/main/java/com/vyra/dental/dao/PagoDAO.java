package com.vyra.dental.dao;

import java.util.List;
import java.util.Optional;

import com.vyra.dental.model.Pago;

/**
 * Contrato de acceso a datos para Pago.
 */
public interface PagoDAO {
    Pago crear(Pago pago);
    Optional<Pago> buscarPorId(int idPago);
    List<Pago> listarTodos();
    boolean actualizar(Pago pago);
    boolean eliminar(int idPago);
}
