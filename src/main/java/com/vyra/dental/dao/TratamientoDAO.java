package com.vyra.dental.dao;

import java.util.List;
import java.util.Optional;

import com.vyra.dental.model.Tratamiento;

/**
 * Contrato de acceso a datos para Tratamiento.
 */
public interface TratamientoDAO {
    Tratamiento crear(Tratamiento tratamiento);
    Optional<Tratamiento> buscarPorId(int idTratamiento);
    List<Tratamiento> listarTodos();
    boolean actualizar(Tratamiento tratamiento);
    boolean eliminar(int idTratamiento);
}