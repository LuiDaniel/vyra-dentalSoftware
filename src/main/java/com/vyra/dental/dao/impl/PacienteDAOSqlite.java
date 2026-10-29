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

import com.vyra.dental.dao.PacienteDAO;
import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.util.Busqueda;
import com.vyra.dental.util.ConexionBD;

/**
 * Implementacion SQLite de PacienteDAO (tabla paciente).
 */
public class PacienteDAOSqlite implements PacienteDAO {

    private static final String COLUMNAS = "id_paciente, nombres, apellidos, dni, fecha_nacimiento, telefono, "
            + "direccion, contacto_emergencia, alergias, antecedentes, activo, fecha_registro";

    // Busqueda de docs/03-modelo-base-datos.md §5.6, sin distinguir tildes ni mayusculas.
    // El filtro se agrega con un fragmento fijo de SQL (nunca con texto del usuario)
    private static final String CONDICION_BUSQUEDA = "activo = 1 "
            + "AND (dni LIKE ? ESCAPE '\\' OR " + Busqueda.sinTildesSql("nombres || ' ' || apellidos")
            + " LIKE ? ESCAPE '\\')";

    @Override
    public Paciente crear(Paciente paciente) {
        if (paciente.getFechaRegistro() == null) {
            paciente.setFechaRegistro(LocalDate.now());
        }
        String sql = "INSERT INTO paciente (nombres, apellidos, dni, fecha_nacimiento, telefono, direccion, "
                + "contacto_emergencia, alergias, antecedentes, activo, fecha_registro) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            escribirCampos(ps, paciente);
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (claves.next()) {
                    paciente.setIdPaciente(claves.getInt(1));
                }
            }
            return paciente;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo registrar el paciente", e);
        }
    }

    @Override
    public Optional<Paciente> buscarPorId(int idPaciente) {
        String sql = "SELECT " + COLUMNAS + " FROM paciente WHERE id_paciente = ?";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            return leerUno(ps);
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo buscar el paciente", e);
        }
    }

    @Override
    public Optional<Paciente> buscarPorDni(String dni) {
        String sql = "SELECT " + COLUMNAS + " FROM paciente WHERE dni = ?";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, dni);
            return leerUno(ps);
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo buscar el paciente", e);
        }
    }

    @Override
    public List<Paciente> buscar(String texto, FiltroPaciente filtro, int limite, int desde) {
        String sql = "SELECT " + COLUMNAS + " FROM paciente WHERE " + CONDICION_BUSQUEDA + condicionFiltro(filtro)
                + " ORDER BY apellidos, nombres LIMIT ? OFFSET ?";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            escribirTexto(ps, texto);
            ps.setInt(3, limite);
            ps.setInt(4, desde);
            try (ResultSet rs = ps.executeQuery()) {
                List<Paciente> pacientes = new ArrayList<>();
                while (rs.next()) {
                    pacientes.add(leer(rs));
                }
                return pacientes;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo buscar los pacientes", e);
        }
    }

    @Override
    public int contarActivos(String texto, FiltroPaciente filtro) {
        String sql = "SELECT COUNT(*) FROM paciente WHERE " + CONDICION_BUSQUEDA + condicionFiltro(filtro);
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            escribirTexto(ps, texto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo contar los pacientes", e);
        }
    }

    @Override
    public boolean actualizar(Paciente paciente) {
        String sql = "UPDATE paciente SET nombres = ?, apellidos = ?, dni = ?, fecha_nacimiento = ?, telefono = ?, "
                + "direccion = ?, contacto_emergencia = ?, alergias = ?, antecedentes = ?, activo = ?, "
                + "fecha_registro = ? WHERE id_paciente = ?";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            escribirCampos(ps, paciente);
            ps.setInt(12, paciente.getIdPaciente());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo actualizar el paciente", e);
        }
    }

    @Override
    public boolean darDeBaja(int idPaciente) {
        String sql = "UPDATE paciente SET activo = 0 WHERE id_paciente = ? AND activo = 1";
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPaciente);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo dar de baja al paciente", e);
        }
    }

    private static String condicionFiltro(FiltroPaciente filtro) {
        return switch (filtro) {
            case TODOS -> "";
            case CON_ALERGIAS -> " AND alergias IS NOT NULL AND TRIM(alergias) <> ''";
            case MENORES_DE_EDAD -> " AND fecha_nacimiento > date('now', '-18 years')";
        };
    }

    /** Parametros 1 y 2 de la busqueda: "%texto%", con % y _ escapados para que se busquen tal cual. */
    private static void escribirTexto(PreparedStatement ps, String texto) throws SQLException {
        String limpio = Busqueda.normalizar(texto)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        String patron = "%" + limpio + "%";
        ps.setString(1, patron);
        ps.setString(2, patron);
    }

    /** Llena los parametros 1 a 11 (los mismos en INSERT y UPDATE). */
    private static void escribirCampos(PreparedStatement ps, Paciente paciente) throws SQLException {
        ps.setString(1, paciente.getNombres());
        ps.setString(2, paciente.getApellidos());
        ps.setString(3, paciente.getDni());
        ps.setString(4, fechaATexto(paciente.getFechaNacimiento()));
        ps.setString(5, paciente.getTelefono());
        ps.setString(6, paciente.getDireccion());
        ps.setString(7, paciente.getContactoEmergencia());
        ps.setString(8, paciente.getAlergias());
        ps.setString(9, paciente.getAntecedentes());
        ps.setInt(10, paciente.isActivo() ? 1 : 0);
        ps.setString(11, fechaATexto(paciente.getFechaRegistro()));
    }

    private static Optional<Paciente> leerUno(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(leer(rs)) : Optional.empty();
        }
    }

    private static Paciente leer(ResultSet rs) throws SQLException {
        return new Paciente(
                rs.getInt("id_paciente"),
                rs.getString("nombres"),
                rs.getString("apellidos"),
                rs.getString("dni"),
                textoAFecha(rs.getString("fecha_nacimiento")),
                rs.getString("telefono"),
                rs.getString("direccion"),
                rs.getString("contacto_emergencia"),
                rs.getString("alergias"),
                rs.getString("antecedentes"),
                rs.getInt("activo") == 1,
                textoAFecha(rs.getString("fecha_registro")));
    }

    private static String fechaATexto(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }

    private static LocalDate textoAFecha(String texto) {
        return texto == null || texto.isBlank() ? null : LocalDate.parse(texto);
    }
}
