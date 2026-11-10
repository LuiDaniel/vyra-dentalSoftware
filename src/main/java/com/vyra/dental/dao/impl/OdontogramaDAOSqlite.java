package com.vyra.dental.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
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

    private static final String COLUMNAS =
            "o.id_registro, o.id_paciente, o.numero_pieza, o.estado, o.fecha_registro, o.observacion, o.id_profesional ";

    @Override
    public OdontogramaPieza registrar(OdontogramaPieza registro) {
        registrarTodos(List.of(registro));
        return registro;
    }

    @Override
    public void registrarTodos(List<OdontogramaPieza> registros) {
        String sql = "INSERT INTO odontograma_pieza "
                + "(id_paciente, numero_pieza, estado, fecha_registro, observacion, id_profesional) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conexion = ConexionBD.obtenerConexion();
        try {
            conexion.setAutoCommit(false);
            try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (OdontogramaPieza registro : registros) {
                    ps.setInt(1, registro.getIdPaciente());
                    ps.setInt(2, registro.getNumeroPieza());
                    ps.setString(3, registro.getEstado().name());
                    ps.setString(4, registro.getFechaRegistro().toString());
                    ps.setString(5, registro.getObservacion());
                    if (registro.getIdProfesional() == null) {
                        ps.setNull(6, Types.INTEGER);
                    } else {
                        ps.setInt(6, registro.getIdProfesional());
                    }
                    ps.executeUpdate();
                    try (ResultSet claves = ps.getGeneratedKeys()) {
                        if (claves.next()) {
                            registro.setIdRegistro(claves.getInt(1));
                        }
                    }
                }
            }
            conexion.commit();
        } catch (SQLException e) {
            deshacer(conexion);
            throw new RuntimeException("No se pudo guardar el odontograma", e);
        } finally {
            try {
                conexion.setAutoCommit(true);
            } catch (SQLException e) {
                // La conexión es compartida: si no vuelve a autocommit, las demás escrituras no se guardarían
                throw new RuntimeException("No se pudo guardar el odontograma", e);
            }
        }
    }

    @Override
    public List<OdontogramaPieza> estadoActual(int idPaciente) {
        // Consulta de docs/03-modelo-base-datos.md §5.1
        String sql = "SELECT " + COLUMNAS + "FROM odontograma_pieza o "
                + "WHERE o.id_paciente = ? AND o.id_registro = ("
                + "SELECT MAX(o2.id_registro) FROM odontograma_pieza o2 "
                + "WHERE o2.id_paciente = o.id_paciente AND o2.numero_pieza = o.numero_pieza) "
                + "ORDER BY o.numero_pieza";
        return listar(sql, idPaciente);
    }

    @Override
    public List<OdontogramaPieza> historialPieza(int idPaciente, int numeroPieza) {
        String sql = "SELECT " + COLUMNAS + "FROM odontograma_pieza o "
                + "WHERE o.id_paciente = ? AND o.numero_pieza = ? ORDER BY o.id_registro DESC";
        return listar(sql, idPaciente, numeroPieza);
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
                    registros.add(leer(rs));
                }
                return registros;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar el odontograma", e);
        }
    }

    private static OdontogramaPieza leer(ResultSet rs) throws SQLException {
        OdontogramaPieza registro = new OdontogramaPieza();
        registro.setIdRegistro(rs.getInt("id_registro"));
        registro.setIdPaciente(rs.getInt("id_paciente"));
        registro.setNumeroPieza(rs.getInt("numero_pieza"));
        registro.setEstado(EstadoPieza.valueOf(rs.getString("estado")));
        registro.setFechaRegistro(LocalDate.parse(rs.getString("fecha_registro")));
        registro.setObservacion(rs.getString("observacion"));
        // wasNull() se refiere a la última columna leída: se pregunta justo después
        int idProfesional = rs.getInt("id_profesional");
        registro.setIdProfesional(rs.wasNull() ? null : idProfesional);
        return registro;
    }

    private static void deshacer(Connection conexion) {
        try {
            conexion.rollback();
        } catch (SQLException e) {
            // Se informa el error original; el rollback fallido no agrega nada útil
        }
    }
}
