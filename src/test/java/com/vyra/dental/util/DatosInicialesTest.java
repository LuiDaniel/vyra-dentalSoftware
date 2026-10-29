package com.vyra.dental.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.vyra.dental.dao.memoria.UsuarioDAOMemoria;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;

class DatosInicialesTest {

    @Test
    void creaElAdminCuandoNoHayUsuarios() {
        UsuarioDAOMemoria dao = new UsuarioDAOMemoria();

        DatosIniciales.crearAdminSiNoExiste(dao);

        Usuario admin = dao.buscarPorNombreUsuario("admin").orElseThrow();
        assertEquals(Rol.ADMIN, admin.getRol());
        assertTrue(admin.isActivo());
        assertTrue(HashUtil.verificar("admin123", admin.getContrasenaHash()));
    }

    @Test
    void noCreaNadaSiYaHayUsuarios() {
        UsuarioDAOMemoria dao = new UsuarioDAOMemoria();
        dao.crear(new Usuario(0, "recepcion1", "Lucía Ramos", HashUtil.hashear("clave123"), Rol.RECEPCION, true));

        DatosIniciales.crearAdminSiNoExiste(dao);
        DatosIniciales.crearAdminSiNoExiste(dao);

        assertEquals(1, dao.contar());
        assertTrue(dao.buscarPorNombreUsuario("admin").isEmpty());
    }
}
