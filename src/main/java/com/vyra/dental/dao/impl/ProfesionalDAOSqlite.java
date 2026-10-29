package com.vyra.dental.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.ProfesionalDAO;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.util.ConexionBD;

/**
 * Implementacion SQLite de ProfesionalDAO (tabla profesional).
 */
public class ProfesionalDAOSqlite implements ProfesionalDAO {

    private static final String COLUMNAS =
            "id_profesional, id_usuario, nombres, apellidos, especialidad, telefono, horario_atencion";

    @Override
    public Profesional crear(Profesional profesional) {
        String sql = "INSERT INTO profesional (id_usuario, nombres, apellidos, especialidad, telefono, horario_atencion) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            escribirCampos(ps, profesional);
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    profesional.setIdProfesional(claves.getInt(1));
                }
            }
            return profesional;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo registrar el profesional", e);
        }
    }

    @Override
    public Optional<Profesional> buscarPorId(int idProfesional) {
        return buscarUno("SELECT " + COLUMNAS + " FROM profesional WHERE id_profesional = ?", idProfesional);
    }

    @Override
    public Optional<Profesional> buscarPorIdUsuario(int idUsuario) {
        return buscarUno("SELECT " + COLUMNAS + " FROM profesional WHERE id_usuario = ?", idUsuario);
    }

    @Override
    public List<Profesional> listarTodos() {
        String sql = "SELECT " + COLUMNAS + " FROM profesional ORDER BY apellidos, nombres";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Profesional> profesionales = new ArrayList<>();
            while (rs.next()) {
                profesionales.add(leer(rs));
            }
            return profesionales;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo listar los profesionales", e);
        }
    }

    @Override
    public boolean actualizar(Profesional profesional) {
        String sql = "UPDATE profesional SET id_usuario = ?, nombres = ?, apellidos = ?, especialidad = ?, "
                + "telefono = ?, horario_atencion = ? WHERE id_profesional = ?";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            escribirCampos(ps, profesional);
            ps.setInt(7, profesional.getIdProfesional());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo actualizar el profesional", e);
        }
    }

    @Override
    public boolean eliminar(int idProfesional) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM profesional WHERE id_profesional = ?")) {
            ps.setInt(1, idProfesional);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo eliminar el profesional", e);
        }
    }

    /** Llena los parametros 1 a 6 (los mismos en INSERT y UPDATE). */
    private void escribirCampos(PreparedStatement ps, Profesional profesional) throws SQLException {
        if (profesional.getIdUsuario() == null) {
            ps.setNull(1, Types.INTEGER);
        } else {
            ps.setInt(1, profesional.getIdUsuario());
        }
        ps.setString(2, profesional.getNombres());
        ps.setString(3, profesional.getApellidos());
        ps.setString(4, profesional.getEspecialidad());
        ps.setString(5, profesional.getTelefono());
        ps.setString(6, profesional.getHorarioAtencion());
    }

    private Optional<Profesional> buscarUno(String sql, int valor) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(leer(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo buscar el profesional", e);
        }
    }

    private Profesional leer(ResultSet rs) throws SQLException {
        // wasNull() se refiere a la última columna leída: hay que preguntarlo justo después
        int valor = rs.getInt("id_usuario");
        Integer idUsuario = rs.wasNull() ? null : valor;
        return new Profesional(
                rs.getInt("id_profesional"),
                idUsuario,
                rs.getString("nombres"),
                rs.getString("apellidos"),
                rs.getString("especialidad"),
                rs.getString("telefono"),
                rs.getString("horario_atencion"));
    }
}
