package com.vyra.dental.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.vyra.dental.dao.OdontogramaDAO;
import com.vyra.dental.model.EstadoPieza;
import com.vyra.dental.model.OdontogramaPieza;
import com.vyra.dental.util.ConexionBD;

/**
 * Implementacion SQLite de OdontogramaDAO (tabla odontograma_pieza).
 */
public class OdontogramaDAOSqlite implements OdontogramaDAO {

    private static final String SELECT = "SELECT o.id_registro, o.id_paciente, o.numero_pieza, o.estado, "
            + "o.fecha_registro, o.observacion FROM odontograma_pieza o ";

    @Override
    public OdontogramaPieza registrar(OdontogramaPieza registro) {
        String sql = "INSERT INTO odontograma_pieza (id_paciente, numero_pieza, estado, fecha_registro, observacion) "
                + "VALUES (?, ?, ?, ?, ?)";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, registro.getIdPaciente());
            ps.setInt(2, registro.getNumeroPieza());
            ps.setString(3, registro.getEstado().name());
            ps.setString(4, registro.getFechaRegistro().toString());
            ps.setString(5, registro.getObservacion());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    registro.setIdRegistro(claves.getInt(1));
                }
            }
            return registro;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo registrar el cambio en el odontograma", e);
        }
    }

    @Override
    public List<OdontogramaPieza> estadoActual(int idPaciente) {
        return listar(SELECT + "WHERE o.id_paciente = ? AND o.id_registro = ("
                + "SELECT MAX(o2.id_registro) FROM odontograma_pieza o2 "
                + "WHERE o2.id_paciente = o.id_paciente AND o2.numero_pieza = o.numero_pieza) "
                + "ORDER BY o.numero_pieza", idPaciente);
    }

    @Override
    public List<OdontogramaPieza> historialPieza(int idPaciente, int numeroPieza) {
        return listar(SELECT + "WHERE o.id_paciente = ? AND o.numero_pieza = ? ORDER BY o.id_registro DESC",
                idPaciente, numeroPieza);
    }

    private List<OdontogramaPieza> listar(String sql, Object... parametros) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<OdontogramaPieza> registros = new ArrayList<>();
                while (rs.next()) {
                    registros.add(new OdontogramaPieza(
                            rs.getInt("id_registro"),
                            rs.getInt("id_paciente"),
                            rs.getInt("numero_pieza"),
                            EstadoPieza.valueOf(rs.getString("estado")),
                            LocalDate.parse(rs.getString("fecha_registro")),
                            rs.getString("observacion")));
                }
                return registros;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar el odontograma", e);
        }
    }
}
