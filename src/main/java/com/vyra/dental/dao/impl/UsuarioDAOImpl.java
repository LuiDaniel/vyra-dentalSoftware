package com.vyra.dental.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.UsuarioDAO;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.util.ConexionBD;

/**
 * Implementacion JDBC/SQLite de UsuarioDAO.
 * Este es el ejemplo de referencia: para PacienteDAO, CitaDAO, etc.
 * copien esta misma forma (una clase Impl por entidad, usando siempre
 * ConexionBD.obtenerConexion() y PreparedStatement).
 */
public class UsuarioDAOImpl implements UsuarioDAO {

    @Override
    public Usuario crear(Usuario usuario) {
        String sql = "INSERT INTO usuario (nombre_usuario, contrasena_hash, rol) VALUES (?, ?, ?)";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, usuario.getNombreUsuario());
            statement.setString(2, usuario.getContrasenaHash());
            statement.setString(3, usuario.getRol().name());
            statement.executeUpdate();

            try (ResultSet generadas = statement.getGeneratedKeys()) {
                if (generadas.next()) {
                    usuario.setIdUsuario(generadas.getInt(1));
                }
            }
            return usuario;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo crear el usuario", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(int idUsuario) {
        String sql = "SELECT id_usuario, nombre_usuario, contrasena_hash, rol FROM usuario WHERE id_usuario = ?";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            try (ResultSet resultado = statement.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapearUsuario(resultado));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo buscar el usuario " + idUsuario, e);
        }
    }

    @Override
    public List<Usuario> listarTodos() {
        String sql = "SELECT id_usuario, nombre_usuario, contrasena_hash, rol FROM usuario ORDER BY id_usuario";
        Connection conexion = ConexionBD.obtenerConexion();
        List<Usuario> usuarios = new ArrayList<>();
        try (Statement statement = conexion.createStatement();
             ResultSet resultado = statement.executeQuery(sql)) {
            while (resultado.next()) {
                usuarios.add(mapearUsuario(resultado));
            }
            return usuarios;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo listar los usuarios", e);
        }
    }

    @Override
    public boolean actualizar(Usuario usuario) {
        String sql = "UPDATE usuario SET nombre_usuario = ?, contrasena_hash = ?, rol = ? WHERE id_usuario = ?";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, usuario.getNombreUsuario());
            statement.setString(2, usuario.getContrasenaHash());
            statement.setString(3, usuario.getRol().name());
            statement.setInt(4, usuario.getIdUsuario());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo actualizar el usuario " + usuario.getIdUsuario(), e);
        }
    }

    @Override
    public boolean eliminar(int idUsuario) {
        String sql = "DELETE FROM usuario WHERE id_usuario = ?";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo eliminar el usuario " + idUsuario, e);
        }
    }

    private Usuario mapearUsuario(ResultSet resultado) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(resultado.getInt("id_usuario"));
        usuario.setNombreUsuario(resultado.getString("nombre_usuario"));
        usuario.setContrasenaHash(resultado.getString("contrasena_hash"));
        usuario.setRol(Rol.valueOf(resultado.getString("rol")));
        return usuario;
    }
}
