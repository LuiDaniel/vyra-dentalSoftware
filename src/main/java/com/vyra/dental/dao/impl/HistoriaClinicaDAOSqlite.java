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
import java.util.Optional;

import com.vyra.dental.dao.HistoriaClinicaDAO;
import com.vyra.dental.model.HistoriaClinicaEntrada;
import com.vyra.dental.util.ConexionBD;

/**
 * Implementacion SQLite de HistoriaClinicaDAO (tabla historia_clinica_entrada).
 */
public class HistoriaClinicaDAOSqlite implements HistoriaClinicaDAO {

    private static final String SELECT = "SELECT id_entrada, id_paciente, id_cita, id_profesional, fecha, "
            + "observaciones, diagnostico FROM historia_clinica_entrada ";

    @Override
    public HistoriaClinicaEntrada crear(HistoriaClinicaEntrada entrada) {
        String sql = "INSERT INTO historia_clinica_entrada "
                + "(id_paciente, id_cita, id_profesional, fecha, observaciones, diagnostico) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entrada.getIdPaciente());
            ponerEntero(ps, 2, entrada.getIdCita());
            ponerEntero(ps, 3, entrada.getIdProfesional());
            ps.setString(4, entrada.getFecha().toString());
            ps.setString(5, entrada.getObservaciones());
            ps.setString(6, entrada.getDiagnostico());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    entrada.setIdEntrada(claves.getInt(1));
                }
            }
            return entrada;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo registrar la entrada de historia clínica", e);
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

    private List<HistoriaClinicaEntrada> listar(String sql, int parametro) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                List<HistoriaClinicaEntrada> entradas = new ArrayList<>();
                while (rs.next()) {
                    entradas.add(new HistoriaClinicaEntrada(
                            rs.getInt("id_entrada"),
                            rs.getInt("id_paciente"),
                            leerEntero(rs, "id_cita"),
                            leerEntero(rs, "id_profesional"),
                            LocalDate.parse(rs.getString("fecha")),
                            rs.getString("observaciones"),
                            rs.getString("diagnostico")));
                }
                return entradas;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar la historia clínica", e);
        }
    }

    private static void ponerEntero(PreparedStatement ps, int indice, Integer valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setInt(indice, valor);
        }
    }

    /** wasNull() se refiere a la última columna leída: se pregunta justo después. */
    private static Integer leerEntero(ResultSet rs, String columna) throws SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }
}
