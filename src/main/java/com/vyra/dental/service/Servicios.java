package com.vyra.dental.service;

import com.vyra.dental.dao.impl.CitaDAOSqlite;
import com.vyra.dental.dao.impl.HistoriaClinicaDAOSqlite;
import com.vyra.dental.dao.impl.OdontogramaDAOSqlite;
import com.vyra.dental.dao.impl.PacienteDAOSqlite;
import com.vyra.dental.dao.impl.PagoDAOSqlite;
import com.vyra.dental.dao.impl.ProfesionalDAOSqlite;
import com.vyra.dental.dao.impl.TratamientoDAOSqlite;
import com.vyra.dental.dao.impl.UsuarioDAOSqlite;

/**
 * Crea los servicios de la aplicación con sus DAO SQLite, una sola vez.
 * Los controladores piden aquí sus servicios (nunca crean DAO); las pruebas
 * crean los servicios directamente con los DAO en memoria.
 */
public final class Servicios {

    private static AuthService auth;
    private static PacienteService pacientes;
    private static CitaService citas;
    private static ProfesionalService profesionales;
    private static HistoriaClinicaService historiaClinica;
    private static OdontogramaService odontograma;
    private static TratamientoService tratamientos;
    private static PagoService pagos;

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

    public static synchronized CitaService citas() {
        if (citas == null) {
            citas = new CitaService(new CitaDAOSqlite());
        }
        return citas;
    }

    public static synchronized HistoriaClinicaService historiaClinica() {
        if (historiaClinica == null) {
            historiaClinica = new HistoriaClinicaService(new HistoriaClinicaDAOSqlite(), new PacienteDAOSqlite(),
                    new CitaDAOSqlite());
        }
        return historiaClinica;
    }

    public static synchronized OdontogramaService odontograma() {
        if (odontograma == null) {
            odontograma = new OdontogramaService(new OdontogramaDAOSqlite(), new PacienteDAOSqlite());
        }
        return odontograma;
    }

    public static synchronized TratamientoService tratamientos() {
        if (tratamientos == null) {
            tratamientos = new TratamientoService(new TratamientoDAOSqlite(), new CitaDAOSqlite(), new PagoDAOSqlite());
        }
        return tratamientos;
    }

    public static synchronized PagoService pagos() {
        if (pagos == null) {
            pagos = new PagoService(new PagoDAOSqlite(), new TratamientoDAOSqlite(), new CitaDAOSqlite());
        }
        return pagos;
    }

    public static synchronized ProfesionalService profesionales() {
        if (profesionales == null) {
            profesionales = new ProfesionalService(new ProfesionalDAOSqlite(), new UsuarioDAOSqlite(),
                    new CitaDAOSqlite());
        }
        return profesionales;
    }
}
