package com.vyra.dental.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.TratamientoDAO;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.model.TratamientoResumen;
import com.vyra.dental.util.Busqueda;
import com.vyra.dental.util.ConexionBD;

/**
 * Implementacion SQLite de TratamientoDAO (tablas tratamiento y tratamiento_pieza).
 */
public class TratamientoDAOSqlite implements TratamientoDAO {

    /** Las piezas vienen como "36,47"; group_concat no garantiza el orden, se ordenan al leer. */
    private static final String COLUMNAS = "t.id_tratamiento, t.id_cita, t.descripcion, t.costo, t.estado, t.fecha, "
            + "(SELECT group_concat(tp.numero_pieza) FROM tratamiento_pieza tp "
            + "WHERE tp.id_tratamiento = t.id_tratamiento) AS piezas ";

    private static final String SELECT = "SELECT " + COLUMNAS + "FROM tratamiento t ";

    // Saldo de docs/03-modelo-base-datos.md §5.3: lo pagado se suma aquí y el saldo se calcula al leer
    private static final String SELECT_RESUMEN = "SELECT " + COLUMNAS + ", "
            + "pa.id_paciente, pa.nombres AS pa_nombres, pa.apellidos AS pa_apellidos, pa.dni, "
            + "pr.id_profesional, pr.nombres AS pr_nombres, pr.apellidos AS pr_apellidos, "
            + "(SELECT IFNULL(SUM(p.monto), 0) FROM pago p WHERE p.id_tratamiento = t.id_tratamiento) AS pagado "
            + "FROM tratamiento t "
            + "JOIN cita c ON c.id_cita = t.id_cita "
            + "JOIN paciente pa ON pa.id_paciente = c.id_paciente "
            + "JOIN profesional pr ON pr.id_profesional = c.id_profesional ";

    private static final String ORDEN = " ORDER BY t.fecha DESC, t.id_tratamiento DESC";

    @Override
    public Tratamiento crear(Tratamiento tratamiento) {
        String sql = "INSERT INTO tratamiento (id_cita, descripcion, costo, estado, fecha) VALUES (?, ?, ?, ?, ?)";
        Connection conexion = ConexionBD.obtenerConexion();
        enTransaccion(conexion, "No se pudo registrar el tratamiento", () -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                escribirCampos(ps, tratamiento);
                ps.executeUpdate();
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (claves.next()) {
                        tratamiento.setIdTratamiento(claves.getInt(1));
                    }
                }
            }
            guardarPiezas(conexion, tratamiento);
        });
        return tratamiento;
    }

    @Override
    public Optional<Tratamiento> buscarPorId(int idTratamiento) {
        return listar(SELECT + "WHERE t.id_tratamiento = ?", idTratamiento).stream().findFirst();
    }

    @Override
    public List<Tratamiento> listarTodos() {
        return listar(SELECT + ORDEN.strip());
    }

    @Override
    public List<Tratamiento> listarPorPaciente(int idPaciente) {
        return listar(SELECT + "JOIN cita c ON c.id_cita = t.id_cita WHERE c.id_paciente = ?" + ORDEN, idPaciente);
    }

    @Override
    public List<Tratamiento> listarPorEstado(EstadoTratamiento estado) {
        return listar(SELECT + "WHERE t.estado = ?" + ORDEN, estado.name());
    }

    @Override
    public boolean actualizar(Tratamiento tratamiento) {
        String sql = "UPDATE tratamiento SET id_cita = ?, descripcion = ?, costo = ?, estado = ?, fecha = ? "
                + "WHERE id_tratamiento = ?";
        Connection conexion = ConexionBD.obtenerConexion();
        boolean[] actualizado = { false };
        enTransaccion(conexion, "No se pudo actualizar el tratamiento", () -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                escribirCampos(ps, tratamiento);
                ps.setInt(6, tratamiento.getIdTratamiento());
                actualizado[0] = ps.executeUpdate() > 0;
            }
            if (actualizado[0]) {
                borrarPiezas(conexion, tratamiento.getIdTratamiento());
                guardarPiezas(conexion, tratamiento);
            }
        });
        return actualizado[0];
    }

    @Override
    public boolean actualizarEstado(int idTratamiento, EstadoTratamiento estado) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = conexion.prepareStatement("UPDATE tratamiento SET estado = ? WHERE id_tratamiento = ?")) {
            ps.setString(1, estado.name());
            ps.setInt(2, idTratamiento);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo cambiar el estado del tratamiento", e);
        }
    }

    /** Lo usa el demo; si el tratamiento tiene pagos, la clave foránea lo impide. */
    @Override
    public boolean eliminar(int idTratamiento) {
        Connection conexion = ConexionBD.obtenerConexion();
        boolean[] eliminado = { false };
        enTransaccion(conexion, "No se pudo eliminar el tratamiento", () -> {
            borrarPiezas(conexion, idTratamiento);
            try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM tratamiento WHERE id_tratamiento = ?")) {
                ps.setInt(1, idTratamiento);
                eliminado[0] = ps.executeUpdate() > 0;
            }
        });
        return eliminado[0];
    }

    @Override
    public List<TratamientoResumen> buscarResumenes(String textoPaciente, EstadoTratamiento estado) {
        StringBuilder sql = new StringBuilder(SELECT_RESUMEN).append("WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        String texto = Busqueda.normalizar(textoPaciente);
        if (!texto.isEmpty()) {
            sql.append(" AND (pa.dni LIKE ? ESCAPE '\\' OR ")
                    .append(Busqueda.sinTildesSql("pa.nombres || ' ' || pa.apellidos")).append(" LIKE ? ESCAPE '\\')");
            String patron = "%" + texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            parametros.add(patron);
            parametros.add(patron);
        }
        if (estado != null) {
            sql.append(" AND t.estado = ?");
            parametros.add(estado.name());
        }
        return listarResumenes(sql.append(ORDEN).toString(), parametros.toArray());
    }

    @Override
    public Optional<TratamientoResumen> buscarResumen(int idTratamiento) {
        return listarResumenes(SELECT_RESUMEN + "WHERE t.id_tratamiento = ?", idTratamiento).stream().findFirst();
    }

    @Override
    public List<TratamientoResumen> resumenesDePaciente(int idPaciente) {
        return listarResumenes(SELECT_RESUMEN + "WHERE pa.id_paciente = ?" + ORDEN, idPaciente);
    }

    // ===== Privados =====

    private List<Tratamiento> listar(String sql, Object... parametros) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = preparar(conexion, sql, parametros); ResultSet rs = ps.executeQuery()) {
            List<Tratamiento> tratamientos = new ArrayList<>();
            while (rs.next()) {
                tratamientos.add(leer(rs));
            }
            return tratamientos;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar los tratamientos", e);
        }
    }

    private List<TratamientoResumen> listarResumenes(String sql, Object... parametros) {
        Connection conexion = ConexionBD.obtenerConexion();
        try (PreparedStatement ps = preparar(conexion, sql, parametros); ResultSet rs = ps.executeQuery()) {
            List<TratamientoResumen> resumenes = new ArrayList<>();
            while (rs.next()) {
                Paciente paciente = new Paciente();
                paciente.setIdPaciente(rs.getInt("id_paciente"));
                paciente.setNombres(rs.getString("pa_nombres"));
                paciente.setApellidos(rs.getString("pa_apellidos"));
                paciente.setDni(rs.getString("dni"));
                Profesional profesional = new Profesional();
                profesional.setIdProfesional(rs.getInt("id_profesional"));
                profesional.setNombres(rs.getString("pr_nombres"));
                profesional.setApellidos(rs.getString("pr_apellidos"));
                resumenes.add(new TratamientoResumen(leer(rs), paciente, profesional, rs.getDouble("pagado")));
            }
            return resumenes;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar los tratamientos", e);
        }
    }

    private static PreparedStatement preparar(Connection conexion, String sql, Object... parametros)
            throws SQLException {
        PreparedStatement ps = conexion.prepareStatement(sql);
        for (int i = 0; i < parametros.length; i++) {
            ps.setObject(i + 1, parametros[i]);
        }
        return ps;
    }

    private static Tratamiento leer(ResultSet rs) throws SQLException {
        String piezas = rs.getString("piezas");
        List<Integer> numeros = piezas == null ? new ArrayList<>()
                : new ArrayList<>(Arrays.stream(piezas.split(",")).map(String::strip).map(Integer::valueOf).sorted().toList());
        return new Tratamiento(
                rs.getInt("id_tratamiento"),
                rs.getInt("id_cita"),
                rs.getString("descripcion"),
                rs.getDouble("costo"),
                EstadoTratamiento.valueOf(rs.getString("estado")),
                LocalDate.parse(rs.getString("fecha")),
                numeros);
    }

    /** Llena los parametros 1 a 5 (los mismos en INSERT y UPDATE). */
    private static void escribirCampos(PreparedStatement ps, Tratamiento tratamiento) throws SQLException {
        ps.setInt(1, tratamiento.getIdCita());
        ps.setString(2, tratamiento.getDescripcion());
        ps.setDouble(3, tratamiento.getCosto());
        ps.setString(4, tratamiento.getEstado().name());
        ps.setString(5, tratamiento.getFecha().toString());
    }

    private static void guardarPiezas(Connection conexion, Tratamiento tratamiento) throws SQLException {
        if (tratamiento.getPiezasDentales() == null) {
            return;
        }
        try (PreparedStatement ps = conexion.prepareStatement(
                "INSERT INTO tratamiento_pieza (id_tratamiento, numero_pieza) VALUES (?, ?)")) {
            for (int pieza : tratamiento.getPiezasDentales()) {
                ps.setInt(1, tratamiento.getIdTratamiento());
                ps.setInt(2, pieza);
                ps.executeUpdate();
            }
        }
    }

    private static void borrarPiezas(Connection conexion, int idTratamiento) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM tratamiento_pieza WHERE id_tratamiento = ?")) {
            ps.setInt(1, idTratamiento);
            ps.executeUpdate();
        }
    }

    @FunctionalInterface
    private interface PasoSql {
        void ejecutar() throws SQLException;
    }

    /** Ejecuta los pasos en una transacción: se guardan todos o ninguno. */
    private static void enTransaccion(Connection conexion, String error, PasoSql pasos) {
        try {
            conexion.setAutoCommit(false);
            pasos.ejecutar();
            conexion.commit();
        } catch (SQLException e) {
            try {
                conexion.rollback();
            } catch (SQLException ignorada) {
                // Se informa el error original; el rollback fallido no agrega nada útil
            }
            throw new RuntimeException(error, e);
        } finally {
            try {
                conexion.setAutoCommit(true);
            } catch (SQLException e) {
                // La conexión es compartida: si no vuelve a autocommit, las demás escrituras no se guardarían
                throw new RuntimeException(error, e);
            }
        }
    }
}
