package com.vyra.dental.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vyra.dental.dao.TratamientoDAO;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.util.ConexionBD;

public class TratamientoDAOImpl implements TratamientoDAO {

    @Override
    public Tratamiento crear(Tratamiento tratamiento) {
        String sql = "INSERT INTO tratamiento (descripcion, costo, estado, piezas_dentales) VALUES (?, ?, ?, ?)";
        Connection conexion = ConexionBD.obtenerConexion();

        try (PreparedStatement statement = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, tratamiento.getDescripcion());
            statement.setDouble(2, tratamiento.getCosto());
            statement.setString(3, tratamiento.getEstado().name());
            statement.setString(4, piezasATexto(tratamiento.getPiezasDentales()));
            statement.executeUpdate();

            try (ResultSet generadas = statement.getGeneratedKeys()) {
                if (generadas.next()) {
                    tratamiento.setIdTratamiento(generadas.getInt(1));
                }
            }
            return tratamiento;

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo crear el tratamiento", e);
        }
    }

    @Override
    public Optional<Tratamiento> buscarPorId(int idTratamiento) {
        String sql = "SELECT * FROM tratamiento WHERE id_tratamiento = ?";
        Connection conexion = ConexionBD.obtenerConexion();

        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, idTratamiento);
            try (ResultSet resultado = statement.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapearTratamiento(resultado));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo buscar el tratamiento " + idTratamiento, e);
        }
    }

    @Override
    public List<Tratamiento> listarTodos() {
        String sql = "SELECT * FROM tratamiento ORDER BY id_tratamiento";
        Connection conexion = ConexionBD.obtenerConexion();
        List<Tratamiento> tratamientos = new ArrayList<>();

        try (Statement statement = conexion.createStatement();
             ResultSet resultado = statement.executeQuery(sql)) {
            while (resultado.next()) {
                tratamientos.add(mapearTratamiento(resultado));
            }
            return tratamientos;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudieron listar los tratamientos", e);
        }
    }

    @Override
    public boolean actualizar(Tratamiento tratamiento) {
        String sql = "UPDATE tratamiento SET descripcion = ?, costo = ?, estado = ?, piezas_dentales = ? WHERE id_tratamiento = ?";
        Connection conexion = ConexionBD.obtenerConexion();

        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, tratamiento.getDescripcion());
            statement.setDouble(2, tratamiento.getCosto());
            statement.setString(3, tratamiento.getEstado().name());
            statement.setString(4, piezasATexto(tratamiento.getPiezasDentales()));
            statement.setInt(5, tratamiento.getIdTratamiento());
            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo actualizar el tratamiento " + tratamiento.getIdTratamiento(), e);
        }
    }

    @Override
    public boolean eliminar(int idTratamiento) {
        String sql = "DELETE FROM tratamiento WHERE id_tratamiento = ?";
        Connection conexion = ConexionBD.obtenerConexion();

        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, idTratamiento);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo eliminar el tratamiento " + idTratamiento, e);
        }
    }

    private Tratamiento mapearTratamiento(ResultSet resultado) throws SQLException {
        Tratamiento tratamiento = new Tratamiento();
        tratamiento.setIdTratamiento(resultado.getInt("id_tratamiento"));
        tratamiento.setDescripcion(resultado.getString("descripcion"));
        tratamiento.setCosto(resultado.getDouble("costo"));
        tratamiento.setEstado(EstadoTratamiento.valueOf(resultado.getString("estado")));
        tratamiento.setPiezasDentales(textoAPiezas(resultado.getString("piezas_dentales")));
        return tratamiento;
    }

    private String piezasATexto(List<Integer> piezas) {
        if (piezas == null || piezas.isEmpty()) {
            return "";
        }
        return piezas.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Integer> textoAPiezas(String texto) {
        List<Integer> piezas = new ArrayList<>();
        if (texto != null && !texto.isBlank()) {
            for (String parte : texto.split(",")) {
                piezas.add(Integer.parseInt(parte.trim()));
            }
        }
        return piezas;
    }
}