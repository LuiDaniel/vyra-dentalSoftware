package com.vyra.dental.util;

import com.vyra.dental.dao.UsuarioDAO;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;

/**
 * Datos que la aplicación necesita para arrancar (docs/03-modelo-base-datos.md §4).
 */
public final class DatosIniciales {

    public static final String ADMIN_USUARIO = "admin";
    public static final String ADMIN_CONTRASENA_INICIAL = "admin123";

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
}
