package com.vyra.dental.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.CitaDAO;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.util.ConexionBD;

/**
 * Implementacion SQLite de CitaDAO (tabla cita, con paciente y profesional).
 */
public class CitaDAOSqlite implements CitaDAO {

    /** Las horas se guardan como texto HH:MM para que la comparacion de textos funcione (§5.2). */
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    // Consulta de docs/03-modelo-base-datos.md §5.4
    private static final String SELECT = "SELECT c.id_cita, c.fecha, c.hora_inicio, c.hora_fin, c.motivo, c.estado, "
            + "pa.id_paciente, pa.nombres AS pa_nombres, pa.apellidos AS pa_apellidos, pa.dni, "
            + "pr.id_profesional, pr.nombres AS pr_nombres, pr.apellidos AS pr_apellidos, pr.especialidad "
            + "FROM cita c "
            + "JOIN paciente pa ON pa.id_paciente = c.id_paciente "
            + "JOIN profesional pr ON pr.id_profesional = c.id_profesional ";

    @Override
    public Cita crear(Cita cita) {
        String sql = "INSERT INTO cita (id_paciente, id_profesional, fecha, hora_inicio, hora_fin, motivo, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            escribirCampos(ps, cita);
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    cita.setIdCita(claves.getInt(1));
                }
            }
            return cita;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo registrar la cita", e);
        }
    }

    @Override
    public Optional<Cita> buscarPorId(int idCita) {
        List<Cita> citas = listar(SELECT + "WHERE c.id_cita = ?", idCita);
        return citas.stream().findFirst();
    }

    @Override
    public List<Cita> listarPorFecha(LocalDate fecha) {
        return listar(SELECT + "WHERE c.fecha = ? ORDER BY c.hora_inicio", fecha.toString());
    }

    @Override
    public List<Cita> listarPorProfesionalYFecha(int idProfesional, LocalDate fecha) {
        return listar(SELECT + "WHERE c.id_profesional = ? AND c.fecha = ? ORDER BY c.hora_inicio",
                idProfesional, fecha.toString());
    }

    @Override
    public List<Cita> listarEntre(LocalDate desde, LocalDate hasta) {
        return listar(SELECT + "WHERE c.fecha BETWEEN ? AND ? ORDER BY c.fecha, c.hora_inicio",
                desde.toString(), hasta.toString());
    }

    @Override
    public List<Cita> listarPorPaciente(int idPaciente) {
        return listar(SELECT + "WHERE c.id_paciente = ? ORDER BY c.fecha DESC, c.hora_inicio DESC", idPaciente);
    }

    @Override
    public Optional<Cita> buscarCruce(int idProfesional, LocalDate fecha, LocalTime inicio, LocalTime fin,
                                      int excluirId) {
        // Dos rangos se traslapan si inicioA < finB y inicioB < finA (docs/03-modelo-base-datos.md §5.2)
        String sql = SELECT + "WHERE c.id_profesional = ? AND c.fecha = ? "
                + "AND c.estado IN ('PENDIENTE','CONFIRMADA') "
                + "AND c.hora_inicio < ? AND ? < c.hora_fin AND c.id_cita <> ? "
                + "ORDER BY c.hora_inicio LIMIT 1";
        return listar(sql, idProfesional, fecha.toString(), HORA.format(fin), HORA.format(inicio), excluirId)
                .stream().findFirst();
    }

    @Override
    public Optional<LocalDate> ultimaVisita(int idPaciente) {
        String sql = "SELECT MAX(fecha) FROM cita WHERE id_paciente = ? AND estado = 'ATENDIDA'";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                String fecha = rs.next() ? rs.getString(1) : null;
                return fecha == null ? Optional.empty() : Optional.of(LocalDate.parse(fecha));
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar la última visita", e);
        }
    }

    @Override
    public int contarPorProfesional(int idProfesional) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement("SELECT COUNT(*) FROM cita WHERE id_profesional = ?")) {
            ps.setInt(1, idProfesional);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo contar las citas", e);
        }
    }

    @Override
    public boolean actualizar(Cita cita) {
        String sql = "UPDATE cita SET id_paciente = ?, id_profesional = ?, fecha = ?, hora_inicio = ?, hora_fin = ?, "
                + "motivo = ?, estado = ? WHERE id_cita = ?";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            escribirCampos(ps, cita);
            ps.setInt(8, cita.getIdCita());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo actualizar la cita", e);
        }
    }

    @Override
    public boolean actualizarEstado(int idCita, EstadoCita estado) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement("UPDATE cita SET estado = ? WHERE id_cita = ?")) {
            ps.setString(1, estado.name());
            ps.setInt(2, idCita);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo cambiar el estado de la cita", e);
        }
    }

    private List<Cita> listar(String sql, Object... parametros) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Cita> citas = new ArrayList<>();
                while (rs.next()) {
                    citas.add(leer(rs));
                }
                return citas;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar las citas", e);
        }
    }

    /** Llena los parametros 1 a 7 (los mismos en INSERT y UPDATE). */
    private static void escribirCampos(PreparedStatement ps, Cita cita) throws SQLException {
        ps.setInt(1, cita.getPaciente().getIdPaciente());
        ps.setInt(2, cita.getProfesional().getIdProfesional());
        ps.setString(3, cita.getFecha().toString());
        ps.setString(4, HORA.format(cita.getHoraInicio()));
        ps.setString(5, HORA.format(cita.getHoraFin()));
        ps.setString(6, cita.getMotivo());
        ps.setString(7, cita.getEstado().name());
    }

    private static Cita leer(ResultSet rs) throws SQLException {
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(rs.getInt("id_paciente"));
        paciente.setNombres(rs.getString("pa_nombres"));
        paciente.setApellidos(rs.getString("pa_apellidos"));
        paciente.setDni(rs.getString("dni"));

        Profesional profesional = new Profesional();
        profesional.setIdProfesional(rs.getInt("id_profesional"));
        profesional.setNombres(rs.getString("pr_nombres"));
        profesional.setApellidos(rs.getString("pr_apellidos"));
        profesional.setEspecialidad(rs.getString("especialidad"));

        return new Cita(
                rs.getInt("id_cita"),
                paciente,
                profesional,
                LocalDate.parse(rs.getString("fecha")),
                LocalTime.parse(rs.getString("hora_inicio")),
                LocalTime.parse(rs.getString("hora_fin")),
                rs.getString("motivo"),
                EstadoCita.valueOf(rs.getString("estado")));
    }
}
