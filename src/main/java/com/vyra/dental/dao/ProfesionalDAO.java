package com.vyra.dental.dao;

import com.vyra.dental.model.Profesional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ProfesionalDAO {

    // CREATE - Inserta y devuelve el ID generado
    public int insertar(Profesional profesional) {

        String sql = """
                INSERT INTO profesional
                (id_usuario, nombres, apellidos, especialidad, telefono, horario_atencion)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement statement = conexion.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setObject(1, profesional.getIdUsuario());
            statement.setString(2, profesional.getNombres());
            statement.setString(3, profesional.getApellidos());
            statement.setString(4, profesional.getEspecialidad());
            statement.setString(5, profesional.getTelefono());
            statement.setString(6, profesional.getHorarioAtencion());

            int filas = statement.executeUpdate();

            if (filas > 0) {
                try (ResultSet claves = statement.getGeneratedKeys()) {

                    if (claves.next()) {
                        return claves.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println(
                    "Error al insertar profesional: " + e.getMessage()
            );
        }

        return -1;
    }

    // READ - Buscar profesional por ID
    public Profesional buscarPorId(int idProfesional) {

        String sql =
                "SELECT * FROM profesional WHERE id_profesional = ?";

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setInt(1, idProfesional);

            try (ResultSet resultado = statement.executeQuery()) {

                if (resultado.next()) {
                    return new Profesional(
                            resultado.getInt("id_profesional"),
                            resultado.getObject(
                                    "id_usuario",
                                    Integer.class
                            ),
                            resultado.getString("nombres"),
                            resultado.getString("apellidos"),
                            resultado.getString("especialidad"),
                            resultado.getString("telefono"),
                            resultado.getString("horario_atencion")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println(
                    "Error al buscar profesional: " + e.getMessage()
            );
        }

        return null;
    }

    // UPDATE - Actualizar profesional
    public boolean actualizar(Profesional profesional) {

        String sql = """
                UPDATE profesional
                SET id_usuario = ?,
                    nombres = ?,
                    apellidos = ?,
                    especialidad = ?,
                    telefono = ?,
                    horario_atencion = ?
                WHERE id_profesional = ?
                """;

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setObject(1, profesional.getIdUsuario());
            statement.setString(2, profesional.getNombres());
            statement.setString(3, profesional.getApellidos());
            statement.setString(4, profesional.getEspecialidad());
            statement.setString(5, profesional.getTelefono());
            statement.setString(6, profesional.getHorarioAtencion());
            statement.setInt(7, profesional.getIdProfesional());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println(
                    "Error al actualizar profesional: " + e.getMessage()
            );
            return false;
        }
    }

    // DELETE - Eliminar profesional por ID
    public boolean eliminar(int idProfesional) {

        String sql =
                "DELETE FROM profesional WHERE id_profesional = ?";

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setInt(1, idProfesional);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println(
                    "Error al eliminar profesional: " + e.getMessage()
            );
            return false;
        }
    }
}