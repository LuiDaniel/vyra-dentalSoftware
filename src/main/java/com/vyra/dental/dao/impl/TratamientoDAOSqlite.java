package com.vyra.dental.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.TratamientoDAO;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.util.ConexionBD;

/**
 * Implementacion SQLite de TratamientoDAO (tablas tratamiento y tratamiento_pieza).
 * El tratamiento y sus piezas se escriben juntos, en una sola transaccion.
 */
public class TratamientoDAOSqlite implements TratamientoDAO {

    private static final String SELECT = "SELECT id_tratamiento, id_cita, descripcion, costo, estado, fecha "
            + "FROM tratamiento ";

    @Override
    public Tratamiento crear(Tratamiento tratamiento) {
        String sql = "INSERT INTO tratamiento (id_cita, descripcion, costo, estado, fecha) VALUES (?, ?, ?, ?, ?)";
        Connection conexion = ConexionBD.obtenerConexion();
        try {
            conexion.setAutoCommit(false);
            try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, tratamiento.getIdCita());
                ps.setString(2, tratamiento.getDescripcion());
                ps.setDouble(3, tratamiento.getCosto());
                ps.setString(4, tratamiento.getEstado().name());
                ps.setString(5, tratamiento.getFecha().toString());
                ps.executeUpdate();
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (claves.next()) {
                        tratamiento.setIdTratamiento(claves.getInt(1));
                    }
                }
            }
            guardarPiezas(conexion, tratamiento);
            conexion.commit();
            return tratamiento;
        } catch (SQLException e) {
            deshacer(conexion);
            throw new RuntimeException("No se pudo registrar el tratamiento", e);
        } finally {
            restaurarAutoCommit(conexion);
        }
    }

    @Override
    public Optional<Tratamiento> buscarPorId(int idTratamiento) {
        return listar(SELECT + "WHERE id_tratamiento = ?", idTratamiento).stream().findFirst();
    }

    @Override
    public List<Tratamiento> listarTodos() {
        return listar(SELECT + "ORDER BY fecha DESC, id_tratamiento DESC");
    }

    @Override
    public List<Tratamiento> listarPorCita(int idCita) {
        return listar(SELECT + "WHERE id_cita = ? ORDER BY id_tratamiento", idCita);
    }

    @Override
    public List<Tratamiento> listarPorEstado(EstadoTratamiento estado) {
        return listar(SELECT + "WHERE estado = ? ORDER BY fecha DESC, id_tratamiento DESC", estado.name());
    }

    @Override
    public boolean actualizar(Tratamiento tratamiento) {
        String sql = "UPDATE tratamiento SET id_cita = ?, descripcion = ?, costo = ?, estado = ?, fecha = ? "
                + "WHERE id_tratamiento = ?";
        Connection conexion = ConexionBD.obtenerConexion();
        try {
            conexion.setAutoCommit(false);
            int filas;
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, tratamiento.getIdCita());
                ps.setString(2, tratamiento.getDescripcion());
                ps.setDouble(3, tratamiento.getCosto());
                ps.setString(4, tratamiento.getEstado().name());
                ps.setString(5, tratamiento.getFecha().toString());
                ps.setInt(6, tratamiento.getIdTratamiento());
                filas = ps.executeUpdate();
            }
            if (filas > 0) {
                borrarPiezas(conexion, tratamiento.getIdTratamiento());
                guardarPiezas(conexion, tratamiento);
            }
            conexion.commit();
            return filas > 0;
        } catch (SQLException e) {
            deshacer(conexion);
            throw new RuntimeException("No se pudo actualizar el tratamiento", e);
        } finally {
            restaurarAutoCommit(conexion);
        }
    }

    @Override
    public boolean eliminar(int idTratamiento) {
        Connection conexion = ConexionBD.obtenerConexion();
        try {
            conexion.setAutoCommit(false);
            borrarPiezas(conexion, idTratamiento);
            int filas;
            try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM tratamiento WHERE id_tratamiento = ?")) {
                ps.setInt(1, idTratamiento);
                filas = ps.executeUpdate();
            }
            conexion.commit();
            return filas > 0;
        } catch (SQLException e) {
            deshacer(conexion);
            throw new RuntimeException("No se pudo eliminar el tratamiento", e);
        } finally {
            restaurarAutoCommit(conexion);
        }
    }

    private static void guardarPiezas(Connection conexion, Tratamiento tratamiento) throws SQLException {
        if (tratamiento.getPiezasDentales() == null) {
            return;
        }
        String sql = "INSERT INTO tratamiento_pieza (id_tratamiento, numero_pieza) VALUES (?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (Integer pieza : tratamiento.getPiezasDentales()) {
                ps.setInt(1, tratamiento.getIdTratamiento());
                ps.setInt(2, pieza);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void borrarPiezas(Connection conexion, int idTratamiento) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM tratamiento_pieza WHERE id_tratamiento = ?")) {
            ps.setInt(1, idTratamiento);
            ps.executeUpdate();
        }
    }

    private List<Tratamiento> listar(String sql, Object... parametros) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            List<Tratamiento> tratamientos = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tratamientos.add(new Tratamiento(
                            rs.getInt("id_tratamiento"),
                            rs.getInt("id_cita"),
                            rs.getString("descripcion"),
                            rs.getDouble("costo"),
                            EstadoTratamiento.valueOf(rs.getString("estado")),
                            LocalDate.parse(rs.getString("fecha")),
                            null));
                }
            }
            for (Tratamiento t : tratamientos) {
                t.setPiezasDentales(leerPiezas(conexion, t.getIdTratamiento()));
            }
            return tratamientos;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar los tratamientos", e);
        }
    }

    private static List<Integer> leerPiezas(Connection conexion, int idTratamiento) throws SQLException {
        String sql = "SELECT numero_pieza FROM tratamiento_pieza WHERE id_tratamiento = ? ORDER BY numero_pieza";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idTratamiento);
            try (ResultSet rs = ps.executeQuery()) {
                List<Integer> piezas = new ArrayList<>();
                while (rs.next()) {
                    piezas.add(rs.getInt(1));
                }
                return piezas;
            }
        }
    }

    private static void deshacer(Connection conexion) {
        try {
            conexion.rollback();
        } catch (SQLException ignorada) {
            // Si el rollback tambien falla, el error original es el que importa
        }
    }

    private static void restaurarAutoCommit(Connection conexion) {
        try {
            conexion.setAutoCommit(true);
        } catch (SQLException ignorada) {
            // La conexion es compartida: si no se restaura, la siguiente operacion fallara con su propio error
        }
    }
}
