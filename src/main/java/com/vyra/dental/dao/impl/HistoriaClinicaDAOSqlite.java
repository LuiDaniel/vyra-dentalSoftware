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

import com.vyra.dental.dao.HistoriaClinicaDAO;
import com.vyra.dental.model.HistoriaClinicaEntrada;
import com.vyra.dental.util.ConexionBD;

/**
 * Implementacion SQLite de HistoriaClinicaDAO (tabla historia_clinica_entrada).
 */
public class HistoriaClinicaDAOSqlite implements HistoriaClinicaDAO {

    private static final String SELECT = "SELECT id_entrada, id_paciente, id_cita, fecha, observaciones, diagnostico "
            + "FROM historia_clinica_entrada ";

    @Override
    public HistoriaClinicaEntrada crear(HistoriaClinicaEntrada entrada) {
        String sql = "INSERT INTO historia_clinica_entrada (id_paciente, id_cita, fecha, observaciones, diagnostico) "
                + "VALUES (?, ?, ?, ?, ?)";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entrada.getIdPaciente());
            ps.setObject(2, entrada.getIdCita());
            ps.setString(3, entrada.getFecha().toString());
            ps.setString(4, entrada.getObservaciones());
            ps.setString(5, entrada.getDiagnostico());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    entrada.setIdEntrada(claves.getInt(1));
                }
            }
            return entrada;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo registrar la entrada de la historia clínica", e);
        }
    }

    @Override
    public Optional<HistoriaClinicaEntrada> buscarPorId(int idEntrada) {
        return listar(SELECT + "WHERE id_entrada = ?", idEntrada).stream().findFirst();
    }

    @Override
    public List<HistoriaClinicaEntrada> listarPorPaciente(int idPaciente) {
        return listar(SELECT + "WHERE id_paciente = ? ORDER BY fecha DESC, id_entrada DESC", idPaciente);
    }

    private List<HistoriaClinicaEntrada> listar(String sql, Object... parametros) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<HistoriaClinicaEntrada> entradas = new ArrayList<>();
                while (rs.next()) {
                    entradas.add(leer(rs));
                }
                return entradas;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar la historia clínica", e);
        }
    }

    private static HistoriaClinicaEntrada leer(ResultSet rs) throws SQLException {
        int idCita = rs.getInt("id_cita");
        boolean sinCita = rs.wasNull();
        return new HistoriaClinicaEntrada(
                rs.getInt("id_entrada"),
                rs.getInt("id_paciente"),
                sinCita ? null : idCita,
                LocalDate.parse(rs.getString("fecha")),
                rs.getString("observaciones"),
                rs.getString("diagnostico"));
    }
}
