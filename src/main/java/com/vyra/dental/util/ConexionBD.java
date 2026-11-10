package com.vyra.dental.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Punto unico de acceso a la base de datos SQLite (jdbc:sqlite:vyra.db).
 * Los DAO deben usar {@link #obtenerConexion()} en lugar de abrir su propia
 * conexion, para evitar bloqueos del archivo .db (ver docs/05-arquitectura.md).
 */
public class ConexionBD {

    // Las pruebas usan -Dvyra.bd.url=jdbc:sqlite::memory: (ver pom.xml) para no tocar vyra.db
    private static final String URL = System.getProperty("vyra.bd.url", urlPorDefecto());
    private static final String SCHEMA_RESOURCE = "/com/vyra/schema.sql";

    private static Connection conexion;

    private ConexionBD() {
    }

    /**
     * En desarrollo, vyra.db en la carpeta de ejecución. En la versión
     * instalada (jpackage define jpackage.app-path), la carpeta del programa
     * no se puede escribir: la base va en %LOCALAPPDATA%\Vyra\vyra.db.
     */
    private static String urlPorDefecto() {
        if (System.getProperty("jpackage.app-path") == null) {
            return "jdbc:sqlite:vyra.db";
        }
        String base = System.getenv("LOCALAPPDATA");
        Path carpeta = base != null ? Path.of(base, "Vyra") : Path.of(System.getProperty("user.home"), ".vyra");
        try {
            Files.createDirectories(carpeta);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de datos " + carpeta, e);
        }
        return "jdbc:sqlite:" + carpeta.resolve("vyra.db");
    }

    public static synchronized Connection obtenerConexion() {
        try {
            if (conexion == null || conexion.isClosed()) {
                conexion = DriverManager.getConnection(URL);
                activarClavesForaneas(conexion);
                inicializarEsquema(conexion);
                actualizarEsquema(conexion);
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo conectar a la base de datos", e);
        }
        return conexion;
    }

    /**
     * SQLite no verifica las claves foraneas si no se activa en cada conexion.
     */
    private static void activarClavesForaneas(Connection conexion) throws SQLException {
        try (Statement statement = conexion.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
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

    /**
     * CREATE TABLE IF NOT EXISTS no agrega columnas a una tabla que ya existe:
     * las columnas nuevas se agregan aquí con ALTER TABLE, para que una vyra.db
     * de una fase anterior siga funcionando (docs/03-modelo-base-datos.md §3).
     */
    private static void actualizarEsquema(Connection conexion) throws SQLException {
        // Fase 5: quién registró cada entrada de historia clínica y cada cambio del odontograma
        agregarColumnaSiFalta(conexion, "historia_clinica_entrada", "id_profesional",
                "INTEGER REFERENCES profesional(id_profesional)");
        agregarColumnaSiFalta(conexion, "odontograma_pieza", "id_profesional",
                "INTEGER REFERENCES profesional(id_profesional)");
    }

    private static void agregarColumnaSiFalta(Connection conexion, String tabla, String columna, String definicion)
            throws SQLException {
        try (Statement statement = conexion.createStatement()) {
            // Los nombres son constantes de esta clase, nunca texto del usuario
            try (ResultSet columnas = statement.executeQuery("PRAGMA table_info(" + tabla + ")")) {
                while (columnas.next()) {
                    if (columnas.getString("name").equals(columna)) {
                        return;
                    }
                }
            }
            statement.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion);
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
