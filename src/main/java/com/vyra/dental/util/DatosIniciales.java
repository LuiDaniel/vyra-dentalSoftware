package com.vyra.dental.util;

import java.time.LocalDate;

import com.vyra.dental.dao.PacienteDAO;
import com.vyra.dental.dao.UsuarioDAO;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;

/**
 * Datos que la aplicación necesita para arrancar (docs/03-modelo-base-datos.md §4).
 */
public final class DatosIniciales {

    public static final String ADMIN_USUARIO = "admin";
    public static final String ADMIN_CONTRASENA_INICIAL = "admin123";

    /** Propiedad que activa los datos de ejemplo: {@code mvn javafx:run -Dvyra.demo=true}. */
    public static final String PROPIEDAD_DEMO = "vyra.demo";

    private DatosIniciales() {
    }

    /**
     * Crea el usuario administrador solo si la tabla usuario está vacía,
     * para que la primera vez se pueda iniciar sesión. Su contraseña
     * debe cambiarse después del primer ingreso.
     */
    public static void crearAdminSiNoExiste(UsuarioDAO usuarioDAO) {
        if (usuarioDAO.contar() > 0) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNombreUsuario(ADMIN_USUARIO);
        admin.setNombreCompleto("Administrador");
        admin.setContrasenaHash(HashUtil.hashear(ADMIN_CONTRASENA_INICIAL));
        admin.setRol(Rol.ADMIN);
        admin.setActivo(true);
        usuarioDAO.crear(admin);
    }

    public static boolean demoActivada() {
        return Boolean.getBoolean(PROPIEDAD_DEMO);
    }

    /**
     * Pacientes ficticios del diseño (docs/ui/Pacientes.html), solo para
     * probar las pantallas en desarrollo. Se insertan una sola vez: si el DNI
     * ya existe, ese paciente se omite.
     */
    public static void cargarEjemplos(PacienteDAO pacienteDAO) {
        Paciente[] ejemplos = {
            paciente("María Fernanda", "Quispe Huamán", "72481935", LocalDate.of(1992, 3, 14), "987654321",
                    "Penicilina", LocalDate.of(2024, 3, 5)),
            paciente("Jorge", "Mamani Condori", "45812307", LocalDate.of(1974, 5, 20), "951203448",
                    null, LocalDate.of(2023, 8, 11)),
            paciente("Lucía", "Torres Rojas", "70356219", LocalDate.of(1999, 7, 8), "964118502",
                    "Látex", LocalDate.of(2025, 1, 17)),
            paciente("Carlos", "Pérez Salas", "43920185", LocalDate.of(1985, 2, 11), "912776340",
                    null, LocalDate.of(2024, 9, 2)),
            paciente("Ana", "Chávez Flores", "76104352", LocalDate.of(2007, 1, 23), "945330917",
                    null, LocalDate.of(2025, 6, 30)),
            paciente("Rosa", "Apaza Choque", "29874561", LocalDate.of(1963, 6, 30), "973482106",
                    "Ibuprofeno", LocalDate.of(2022, 11, 4)),
            paciente("Diego", "Vargas Lima", "71592846", LocalDate.of(2018, 4, 5), "988210774",
                    null, LocalDate.of(2026, 2, 9)),
        };
        for (Paciente ejemplo : ejemplos) {
            if (pacienteDAO.buscarPorDni(ejemplo.getDni()).isEmpty()) {
                pacienteDAO.crear(ejemplo);
            }
        }
    }

    private static Paciente paciente(String nombres, String apellidos, String dni, LocalDate nacimiento,
                                     String telefono, String alergias, LocalDate registro) {
        return new Paciente(0, nombres, apellidos, dni, nacimiento, telefono, "Av. Ejemplo 123, Tacna",
                null, alergias, null, true, registro);
    }
}
