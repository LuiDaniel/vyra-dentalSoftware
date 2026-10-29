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
 * Implementacion SQLite de UsuarioDAO (tabla usuario).
 */
public class UsuarioDAOSqlite implements UsuarioDAO {

    private static final String COLUMNAS =
            "id_usuario, nombre_usuario, nombre_completo, contrasena_hash, rol, activo";

    @Override
    public Usuario crear(Usuario usuario) {
        String sql = "INSERT INTO usuario (nombre_usuario, nombre_completo, contrasena_hash, rol, activo) "
                + "VALUES (?, ?, ?, ?, ?)";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, usuario.getNombreUsuario());
            ps.setString(2, usuario.getNombreCompleto());
            ps.setString(3, usuario.getContrasenaHash());
            ps.setString(4, usuario.getRol().name());
            ps.setInt(5, usuario.isActivo() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    usuario.setIdUsuario(claves.getInt(1));
                }
            }
            return usuario;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo registrar el usuario", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(int idUsuario) {
        return buscarUno("SELECT " + COLUMNAS + " FROM usuario WHERE id_usuario = ?", idUsuario);
    }

    @Override
    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        return buscarUno("SELECT " + COLUMNAS + " FROM usuario WHERE nombre_usuario = ?", nombreUsuario);
    }

    @Override
    public List<Usuario> listarTodos() {
        String sql = "SELECT " + COLUMNAS + " FROM usuario ORDER BY nombre_completo";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Usuario> usuarios = new ArrayList<>();
            while (rs.next()) {
                usuarios.add(leer(rs));
            }
            return usuarios;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo listar los usuarios", e);
        }
    }

    @Override
    public int contar() {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement("SELECT COUNT(*) FROM usuario");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo contar los usuarios", e);
        }
    }

    @Override
    public boolean actualizar(Usuario usuario) {
        String sql = "UPDATE usuario SET nombre_usuario = ?, nombre_completo = ?, contrasena_hash = ?, "
                + "rol = ?, activo = ? WHERE id_usuario = ?";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, usuario.getNombreUsuario());
            ps.setString(2, usuario.getNombreCompleto());
            ps.setString(3, usuario.getContrasenaHash());
            ps.setString(4, usuario.getRol().name());
            ps.setInt(5, usuario.isActivo() ? 1 : 0);
            ps.setInt(6, usuario.getIdUsuario());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo actualizar el usuario", e);
        }
    }

    @Override
    public boolean eliminar(int idUsuario) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM usuario WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo eliminar el usuario", e);
        }
    }

    private Optional<Usuario> buscarUno(String sql, Object valor) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setObject(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(leer(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo buscar el usuario", e);
        }
    }

    private Usuario leer(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt("id_usuario"),
                rs.getString("nombre_usuario"),
                rs.getString("nombre_completo"),
                rs.getString("contrasena_hash"),
                Rol.valueOf(rs.getString("rol")),
                rs.getInt("activo") == 1);
    }
}
