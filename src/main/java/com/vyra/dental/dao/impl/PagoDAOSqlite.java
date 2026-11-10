package com.vyra.dental.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vyra.dental.dao.PagoDAO;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.PagoResumen;
import com.vyra.dental.util.ConexionBD;

/**
 * Implementacion SQLite de PagoDAO (tabla pago).
 */
public class PagoDAOSqlite implements PagoDAO {

    private static final String COLUMNAS =
            "p.id_pago, p.id_tratamiento, p.monto, p.fecha_pago, p.metodo_pago, p.numero_operacion ";
    private static final String SELECT = "SELECT " + COLUMNAS + "FROM pago p ";
    private static final String ORDEN = " ORDER BY p.fecha_pago DESC, p.id_pago DESC";

    @Override
    public Pago crear(Pago pago) {
        String sql = "INSERT INTO pago (id_tratamiento, monto, fecha_pago, metodo_pago, numero_operacion) "
                + "VALUES (?, ?, ?, ?, ?)";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            escribirCampos(ps, pago);
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    pago.setIdPago(claves.getInt(1));
                }
            }
            return pago;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo registrar el pago", e);
        }
    }

    @Override
    public Optional<Pago> buscarPorId(int idPago) {
        return listar(SELECT + "WHERE p.id_pago = ?", idPago).stream().findFirst();
    }

    @Override
    public List<Pago> listarTodos() {
        return listar(SELECT + ORDEN.strip());
    }

    @Override
    public List<Pago> listarPorTratamiento(int idTratamiento) {
        return listar(SELECT + "WHERE p.id_tratamiento = ?" + ORDEN, idTratamiento);
    }

    @Override
    public List<Pago> listarPorPeriodo(LocalDate desde, LocalDate hasta) {
        return listar(SELECT + "WHERE p.fecha_pago BETWEEN ? AND ?" + ORDEN, desde.toString(), hasta.toString());
    }

    @Override
    public double totalPagado(int idTratamiento) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT IFNULL(SUM(monto), 0) FROM pago WHERE id_tratamiento = ?")) {
            ps.setInt(1, idTratamiento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo calcular lo pagado", e);
        }
    }

    @Override
    public List<PagoResumen> resumenesEntre(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT " + COLUMNAS + ", t.descripcion, "
                + "pa.id_paciente, pa.nombres, pa.apellidos, pa.dni "
                + "FROM pago p "
                + "JOIN tratamiento t ON t.id_tratamiento = p.id_tratamiento "
                + "JOIN cita c ON c.id_cita = t.id_cita "
                + "JOIN paciente pa ON pa.id_paciente = c.id_paciente "
                + "WHERE p.fecha_pago BETWEEN ? AND ?" + ORDEN;
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<PagoResumen> resumenes = new ArrayList<>();
                while (rs.next()) {
                    Paciente paciente = new Paciente();
                    paciente.setIdPaciente(rs.getInt("id_paciente"));
                    paciente.setNombres(rs.getString("nombres"));
                    paciente.setApellidos(rs.getString("apellidos"));
                    paciente.setDni(rs.getString("dni"));
                    resumenes.add(new PagoResumen(leer(rs), rs.getString("descripcion"), paciente));
                }
                return resumenes;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar los pagos", e);
        }
    }

    @Override
    public Map<MetodoPago, Double> totalesPorMetodo(LocalDate desde, LocalDate hasta) {
        // Consulta de docs/03-modelo-base-datos.md §5.5
        String sql = "SELECT metodo_pago, SUM(monto) AS total FROM pago "
                + "WHERE fecha_pago BETWEEN ? AND ? GROUP BY metodo_pago";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                Map<MetodoPago, Double> totales = new EnumMap<>(MetodoPago.class);
                while (rs.next()) {
                    totales.put(MetodoPago.valueOf(rs.getString("metodo_pago")), rs.getDouble("total"));
                }
                return totales;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo calcular los ingresos", e);
        }
    }

    @Override
    public boolean actualizar(Pago pago) {
        String sql = "UPDATE pago SET id_tratamiento = ?, monto = ?, fecha_pago = ?, metodo_pago = ?, "
                + "numero_operacion = ? WHERE id_pago = ?";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            escribirCampos(ps, pago);
            ps.setInt(6, pago.getIdPago());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo actualizar el pago", e);
        }
    }

    @Override
    public boolean eliminar(int idPago) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM pago WHERE id_pago = ?")) {
            ps.setInt(1, idPago);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo eliminar el pago", e);
        }
    }

    private List<Pago> listar(String sql, Object... parametros) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Pago> pagos = new ArrayList<>();
                while (rs.next()) {
                    pagos.add(leer(rs));
                }
                return pagos;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar los pagos", e);
        }
    }

    /** Llena los parametros 1 a 5 (los mismos en INSERT y UPDATE). */
    private static void escribirCampos(PreparedStatement ps, Pago pago) throws SQLException {
        ps.setInt(1, pago.getIdTratamiento());
        ps.setDouble(2, pago.getMonto());
        ps.setString(3, pago.getFechaPago().toString());
        ps.setString(4, pago.getMetodoPago().name());
        ps.setString(5, pago.getNumeroOperacion());
    }

    private static Pago leer(ResultSet rs) throws SQLException {
        return new Pago(
                rs.getInt("id_pago"),
                rs.getInt("id_tratamiento"),
                rs.getDouble("monto"),
                LocalDate.parse(rs.getString("fecha_pago")),
                MetodoPago.valueOf(rs.getString("metodo_pago")),
                rs.getString("numero_operacion"));
    }
}
