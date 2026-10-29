package com.vyra.dental.dao;

import java.util.List;
import java.util.Optional;

import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.Tratamiento;

/**
 * Contrato de acceso a datos para Tratamiento (con sus piezas dentales).
 */
public interface TratamientoDAO {
    /** Guarda el tratamiento y sus piezas en una sola transacción. */
    Tratamiento crear(Tratamiento tratamiento);
    Optional<Tratamiento> buscarPorId(int idTratamiento);
    /** Del más reciente al más antiguo. */
    List<Tratamiento> listarTodos();
    /** Los tratamientos registrados en esa cita. */
    List<Tratamiento> listarPorCita(int idCita);
    List<Tratamiento> listarPorEstado(EstadoTratamiento estado);
    boolean actualizar(Tratamiento tratamiento);
    boolean eliminar(int idTratamiento);
}
