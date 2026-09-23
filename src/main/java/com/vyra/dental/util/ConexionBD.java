package com.vyra.dental.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Punto unico de acceso a la base de datos SQLite (jdbc:sqlite:vyra.db).
 * Los DAO deben usar {@link #obtenerConexion()} en lugar de abrir su propia
 * conexion, para evitar bloqueos del archivo .db (ver docs/05-arquitectura.md).
 */
public class ConexionBD {

    private static final String URL = "jdbc:sqlite:vyra.db";
    private static final String SCHEMA_RESOURCE = "/com/vyra/schema.sql";

    private static Connection conexion;

    private ConexionBD() {
    }

    public static synchronized Connection obtenerConexion() {
        try {
            if (conexion == null || conexion.isClosed()) {
                conexion = DriverManager.getConnection(URL);
                inicializarEsquema(conexion);
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo conectar a la base de datos", e);
        }
        return conexion;
    }

    /**
     * Ejecuta schema.sql (CREATE TABLE IF NOT EXISTS) para que la base de
     * datos quede lista la primera vez que se abre la conexion.
     */
    private static void inicializarEsquema(Connection conexion) {
        String script = leerSchemaSql();
        try (Statement statement = conexion.createStatement()) {
            for (String sentencia : script.split(";")) {
                String limpio = sentencia.trim();
                if (!limpio.isEmpty()) {
                    statement.execute(limpio);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo inicializar el esquema de la base de datos", e);
        }
    }

    private static String leerSchemaSql() {
        try (InputStream input = ConexionBD.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (input == null) {
                throw new IOException("No se encontro el recurso " + SCHEMA_RESOURCE);
            }
            StringBuilder contenido = new StringBuilder();
            try (BufferedReader lector = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    contenido.append(linea).append('\n');
                }
            }
            return contenido.toString();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer schema.sql", e);
        }
    }
}
