package com.vyra.dental.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

/**
 * Fase 9: una vyra.db de antes (pago sin id_cita, sin MERCADO_PAGO, cita y
 * profesional sin costo_consulta) se actualiza sola al abrirla, sin perder pagos.
 * Usa su propia base en memoria, no la de ConexionBD.
 */
class MigracionPagoTest {

    @Test
    void actualizaUnaBaseAntiguaSinPerderLosPagos() throws SQLException {
        try (Connection conexion = DriverManager.getConnection("jdbc:sqlite::memory:");
             Statement st = conexion.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("CREATE TABLE profesional (id_profesional INTEGER PRIMARY KEY, nombres TEXT)");
            st.execute("CREATE TABLE paciente (id_paciente INTEGER PRIMARY KEY, nombres TEXT)");
            st.execute("CREATE TABLE cita (id_cita INTEGER PRIMARY KEY, id_paciente INTEGER, id_profesional INTEGER)");
            st.execute("CREATE TABLE historia_clinica_entrada (id_entrada INTEGER PRIMARY KEY)");
            st.execute("CREATE TABLE odontograma_pieza (id_registro INTEGER PRIMARY KEY)");
            st.execute("CREATE TABLE tratamiento (id_tratamiento INTEGER PRIMARY KEY, id_cita INTEGER)");
            st.execute("CREATE TABLE pago (id_pago INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "id_tratamiento INTEGER NOT NULL REFERENCES tratamiento(id_tratamiento), "
                    + "monto REAL NOT NULL CHECK (monto > 0), fecha_pago TEXT NOT NULL, "
                    + "metodo_pago TEXT NOT NULL CHECK (metodo_pago IN ('EFECTIVO','YAPE_PLIN','TARJETA','TRANSFERENCIA')), "
                    + "numero_operacion TEXT)");
            st.execute("INSERT INTO profesional VALUES (1, 'Marco')");
            st.execute("INSERT INTO paciente VALUES (1, 'María')");
            st.execute("INSERT INTO cita VALUES (1, 1, 1)");
            st.execute("INSERT INTO tratamiento VALUES (1, 1)");
            st.execute("INSERT INTO pago (id_tratamiento, monto, fecha_pago, metodo_pago) VALUES (1, 250, '2025-01-10', 'TARJETA')");

            ConexionBD.actualizarEsquema(conexion);
            // Dos veces no hace daño (la app la llama en cada arranque)
            ConexionBD.actualizarEsquema(conexion);

            try (ResultSet rs = st.executeQuery("SELECT id_tratamiento, monto, metodo_pago, id_cita FROM pago")) {
                rs.next();
                assertEquals(1, rs.getInt("id_tratamiento"));
                assertEquals(250, rs.getDouble("monto"));
                assertEquals("TARJETA", rs.getString("metodo_pago"));
                rs.getInt("id_cita");
                assertEquals(true, rs.wasNull());
            }
            st.execute("INSERT INTO pago (id_cita, monto, fecha_pago, metodo_pago, numero_operacion) "
                    + "VALUES (1, 80, '2025-01-15', 'MERCADO_PAGO', '123')");
            try (ResultSet rs = st.executeQuery("SELECT costo_consulta FROM cita WHERE id_cita = 1")) {
                rs.next();
                assertEquals(0, rs.getDouble(1));
            }
            st.execute("UPDATE profesional SET costo_consulta = 80");
            // De un tratamiento o de una consulta, nunca de los dos ni de ninguno
            assertThrows(SQLException.class, () -> st.execute("INSERT INTO pago (id_tratamiento, id_cita, monto, "
                    + "fecha_pago, metodo_pago) VALUES (1, 1, 10, '2025-01-15', 'EFECTIVO')"));
            assertThrows(SQLException.class, () -> st.execute("INSERT INTO pago (monto, fecha_pago, metodo_pago) "
                    + "VALUES (10, '2025-01-15', 'EFECTIVO')"));
        }
    }
}
