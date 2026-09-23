package com.vyra.dental.dao;

import java.util.List;
import java.util.Optional;

import com.vyra.dental.model.Usuario;

/**
 * Contrato de acceso a datos para Usuario.
 * Cada entidad principal (Paciente, Cita, Tratamiento, ...) debe tener su
 * propio DAO siguiendo este mismo patron: interfaz aqui en dao/,
 * implementacion concreta con JDBC en dao/impl/.
 */
public interface UsuarioDAO {

    Usuario crear(Usuario usuario);

    Optional<Usuario> buscarPorId(int idUsuario);

    List<Usuario> listarTodos();

    boolean actualizar(Usuario usuario);

    boolean eliminar(int idUsuario);
}
