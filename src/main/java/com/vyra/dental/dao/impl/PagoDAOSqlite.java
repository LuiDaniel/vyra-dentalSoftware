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

import com.vyra.dental.dao.PagoDAO;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Pago;
import com.vyra.dental.util.ConexionBD;

/**
 * Implementacion SQLite de PagoDAO (tabla pago).
 */
public class PagoDAOSqlite implements PagoDAO {

    private static final String SELECT = "SELECT id_pago, id_tratamiento, monto, fecha_pago, metodo_pago, "
            + "numero_operacion FROM pago ";

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
        return listar(SELECT + "WHERE id_pago = ?", idPago).stream().findFirst();
    }

    @Override
    public List<Pago> listarTodos() {
        return listar(SELECT + "ORDER BY fecha_pago DESC, id_pago DESC");
    }

    @Override
    public List<Pago> listarPorTratamiento(int idTratamiento) {
        return listar(SELECT + "WHERE id_tratamiento = ? ORDER BY fecha_pago, id_pago", idTratamiento);
    }

    @Override
    public List<Pago> listarPorPeriodo(LocalDate desde, LocalDate hasta) {
        return listar(SELECT + "WHERE fecha_pago BETWEEN ? AND ? ORDER BY fecha_pago DESC, id_pago DESC",
                desde.toString(), hasta.toString());
    }

    @Override
    public double totalPagado(int idTratamiento) {
        String sql = "SELECT IFNULL(SUM(monto), 0) FROM pago WHERE id_tratamiento = ?";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idTratamiento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo calcular lo pagado", e);
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

    private static void escribirCampos(PreparedStatement ps, Pago pago) throws SQLException {
        ps.setInt(1, pago.getIdTratamiento());
        ps.setDouble(2, pago.getMonto());
        ps.setString(3, pago.getFechaPago().toString());
        ps.setString(4, pago.getMetodoPago().name());
        ps.setString(5, pago.getNumeroOperacion());
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
                    pagos.add(new Pago(
                            rs.getInt("id_pago"),
                            rs.getInt("id_tratamiento"),
                            rs.getDouble("monto"),
                            LocalDate.parse(rs.getString("fecha_pago")),
                            MetodoPago.valueOf(rs.getString("metodo_pago")),
                            rs.getString("numero_operacion")));
                }
                return pagos;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar los pagos", e);
        }
    }
}
