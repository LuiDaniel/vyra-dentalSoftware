package com.vyra.dental.dao.impl;

import com.vyra.dental.dao.PacienteDAO;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PacienteDAOImpl implements PacienteDAO {

    @Override
    public Paciente crear(Paciente paciente) {
        String sql = """
            INSERT INTO paciente (nombres, apellidos, dni, fecha_nacimiento, telefono, direccion, contacto_emergencia, alergias) VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";

        Connection conexion = ConexionBD.obtenerConexion();

        try (PreparedStatement statement =
                     conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, paciente.getNombres());
            statement.setString(2, paciente.getApellidos());
            statement.setString(3, paciente.getDni());

            if (paciente.getFechaNacimiento() != null) {
                statement.setString(4, paciente.getFechaNacimiento().toString());
            } else {
                statement.setNull(4, java.sql.Types.VARCHAR);
            }

            statement.setString(5, paciente.getTelefono());
            statement.setString(6, paciente.getDireccion());
            statement.setString(7, paciente.getContactoEmergencia());
            statement.setString(8, paciente.getAlergias());

            statement.executeUpdate();

            try (ResultSet generadas = statement.getGeneratedKeys()) {
                if (generadas.next()) {
                    paciente.setIdPaciente(generadas.getInt(1));
                }
            }

            return paciente;

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo registrar el paciente", e);
        }
    }

    @Override
    public Optional<Paciente> buscarPorId(int idPaciente) {
        String sql = "SELECT * FROM paciente WHERE id_paciente = ?";
        Connection conexion = ConexionBD.obtenerConexion();

        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, idPaciente);

            try (ResultSet resultado = statement.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapearPaciente(resultado));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo buscar el paciente " + idPaciente, e);
        }
    }

    @Override
    public Optional<Paciente> buscarPorDni(String dni) {
        String sql = "SELECT * FROM paciente WHERE dni = ?";
        Connection conexion = ConexionBD.obtenerConexion();

        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, dni);

            try (ResultSet resultado = statement.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapearPaciente(resultado));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo buscar el paciente con DNI " + dni, e);
        }
    }

    @Override
    public List<Paciente> listarTodos() {
        String sql = "SELECT * FROM paciente ORDER BY apellidos, nombres";
        Connection conexion = ConexionBD.obtenerConexion();
        List<Paciente> pacientes = new ArrayList<>();

        try (Statement statement = conexion.createStatement();
             ResultSet resultado = statement.executeQuery(sql)) {

            while (resultado.next()) {
                pacientes.add(mapearPaciente(resultado));
            }

            return pacientes;

        } catch (SQLException e) {
            throw new RuntimeException("No se pudieron listar los pacientes", e);
        }
    }

    @Override
    public boolean actualizar(Paciente paciente) {
        String sql = """
            UPDATE paciente
            SET nombres = ?, apellidos = ?, dni = ?, fecha_nacimiento = ?,
                telefono = ?, direccion = ?, contacto_emergencia = ?, alergias = ?
            WHERE id_paciente = ?
            """;

        Connection conexion = ConexionBD.obtenerConexion();

        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, paciente.getNombres());
            statement.setString(2, paciente.getApellidos());
            statement.setString(3, paciente.getDni());

            if (paciente.getFechaNacimiento() != null) {
                statement.setString(4, paciente.getFechaNacimiento().toString());
            } else {
                statement.setNull(4, java.sql.Types.VARCHAR);
            }

            statement.setString(5, paciente.getTelefono());
            statement.setString(6, paciente.getDireccion());
            statement.setString(7, paciente.getContactoEmergencia());
            statement.setString(8, paciente.getAlergias());
            statement.setInt(9, paciente.getIdPaciente());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo actualizar el paciente " + paciente.getIdPaciente(), e
            );
        }
    }

    @Override
    public boolean eliminar(int idPaciente) {
        String sql = "DELETE FROM paciente WHERE id_paciente = ?";
        Connection conexion = ConexionBD.obtenerConexion();

        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, idPaciente);
            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo eliminar el paciente " + idPaciente, e);
        }
    }

    private Paciente mapearPaciente(ResultSet resultado) throws SQLException {
        Paciente paciente = new Paciente();

        paciente.setIdPaciente(resultado.getInt("id_paciente"));
        paciente.setNombres(resultado.getString("nombres"));
        paciente.setApellidos(resultado.getString("apellidos"));
        paciente.setDni(resultado.getString("dni"));
        paciente.setTelefono(resultado.getString("telefono"));
        paciente.setDireccion(resultado.getString("direccion"));
        paciente.setContactoEmergencia(resultado.getString("contacto_emergencia"));
        paciente.setAlergias(resultado.getString("alergias"));

        String fechaNacimiento = resultado.getString("fecha_nacimiento");
        if (fechaNacimiento != null && !fechaNacimiento.isBlank()) {
            paciente.setFechaNacimiento(java.time.LocalDate.parse(fechaNacimiento));
        }

        return paciente;
    }
}