package com.vyra.dental.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;

/**
 * Corre contra SQLite en memoria (vyra.bd.url en pom.xml), nunca contra vyra.db.
 * La base se comparte entre pruebas, por eso cada una usa nombres de usuario propios.
 */
class UsuarioYProfesionalDAOSqliteTest {

    private final UsuarioDAOSqlite usuarioDAO = new UsuarioDAOSqlite();
    private final ProfesionalDAOSqlite profesionalDAO = new ProfesionalDAOSqlite();

    @Test
    void creaYBuscaUnUsuario() {
        int antes = usuarioDAO.contar();
        Usuario creado = usuarioDAO.crear(new Usuario(0, "lramos", "Lucía Ramos", "pbkdf2$1$a$b", Rol.RECEPCION, true));

        assertTrue(creado.getIdUsuario() > 0);
        assertEquals(antes + 1, usuarioDAO.contar());
        Usuario leido = usuarioDAO.buscarPorNombreUsuario("lramos").orElseThrow();
        assertEquals(creado, leido);
        assertEquals(leido, usuarioDAO.buscarPorId(creado.getIdUsuario()).orElseThrow());
        assertTrue(usuarioDAO.buscarPorNombreUsuario("no-existe").isEmpty());
    }

    @Test
    void actualizaYEliminaUnUsuario() {
        Usuario usuario = usuarioDAO.crear(new Usuario(0, "psalas", "Pedro Salas", "pbkdf2$1$a$b", Rol.RECEPCION, true));
        usuario.setActivo(false);
        usuario.setRol(Rol.ADMIN);

        assertTrue(usuarioDAO.actualizar(usuario));
        Usuario leido = usuarioDAO.buscarPorId(usuario.getIdUsuario()).orElseThrow();
        assertFalse(leido.isActivo());
        assertEquals(Rol.ADMIN, leido.getRol());

        assertTrue(usuarioDAO.eliminar(usuario.getIdUsuario()));
        assertTrue(usuarioDAO.buscarPorId(usuario.getIdUsuario()).isEmpty());
    }

    @Test
    void elNombreDeUsuarioNoSePuedeRepetir() {
        usuarioDAO.crear(new Usuario(0, "repetido", "Ana Torres", "pbkdf2$1$a$b", Rol.RECEPCION, true));
        assertThrows(RuntimeException.class,
                () -> usuarioDAO.crear(new Usuario(0, "repetido", "Otra Persona", "pbkdf2$1$a$b", Rol.ADMIN, true)));
    }

    @Test
    void profesionalConYSinUsuario() {
        Usuario cuenta = usuarioDAO.crear(new Usuario(0, "dsalinas", "Diego Salinas", "pbkdf2$1$a$b", Rol.ODONTOLOGO, true));
        Profesional conCuenta = profesionalDAO.crear(new Profesional(0, cuenta.getIdUsuario(), "Diego", "Salinas",
                "Endodoncia", "987654321", "Lun-Vie 08:00-14:00"));
        Profesional sinCuenta = profesionalDAO.crear(new Profesional(0, null, "Rosa", "Mendoza",
                "Ortodoncia", null, null));

        assertEquals(conCuenta, profesionalDAO.buscarPorIdUsuario(cuenta.getIdUsuario()).orElseThrow());
        assertNull(profesionalDAO.buscarPorId(sinCuenta.getIdProfesional()).orElseThrow().getIdUsuario());

        sinCuenta.setTelefono("912345678");
        assertTrue(profesionalDAO.actualizar(sinCuenta));
        assertEquals("912345678", profesionalDAO.buscarPorId(sinCuenta.getIdProfesional()).orElseThrow().getTelefono());
        assertTrue(profesionalDAO.eliminar(sinCuenta.getIdProfesional()));
    }

    @Test
    void lasClavesForaneasEstanActivas() {
        assertThrows(RuntimeException.class, () -> profesionalDAO.crear(
                new Profesional(0, 99999, "Sin", "Cuenta", null, null, null)));
    }
}
