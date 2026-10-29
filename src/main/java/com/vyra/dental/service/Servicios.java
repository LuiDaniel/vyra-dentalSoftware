package com.vyra.dental.service;

import com.vyra.dental.dao.impl.PacienteDAOSqlite;
import com.vyra.dental.dao.impl.ProfesionalDAOSqlite;
import com.vyra.dental.dao.impl.UsuarioDAOSqlite;

/**
 * Crea los servicios de la aplicación con sus DAO SQLite, una sola vez.
 * Los controladores piden aquí sus servicios (nunca crean DAO); las pruebas
 * crean los servicios directamente con los DAO en memoria.
 */
public final class Servicios {

    private static AuthService auth;
    private static PacienteService pacientes;

    private Servicios() {
    }

    public static synchronized AuthService auth() {
        if (auth == null) {
            auth = new AuthService(new UsuarioDAOSqlite(), new ProfesionalDAOSqlite());
        }
        return auth;
    }

    public static synchronized PacienteService pacientes() {
        if (pacientes == null) {
            pacientes = new PacienteService(new PacienteDAOSqlite());
        }
        return pacientes;
    }
}
