package com.vyra.dental.dao;

import java.util.List;
import java.util.Optional;

import com.vyra.dental.model.Profesional;

/**
 * Contrato de acceso a datos para Profesional.
 */
public interface ProfesionalDAO {
    Profesional crear(Profesional profesional);
    Optional<Profesional> buscarPorId(int idProfesional);
    Optional<Profesional> buscarPorIdUsuario(int idUsuario);
    List<Profesional> listarTodos();
    boolean actualizar(Profesional profesional);
    boolean eliminar(int idProfesional);
}
