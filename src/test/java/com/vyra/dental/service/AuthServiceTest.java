package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.dao.memoria.ProfesionalDAOMemoria;
import com.vyra.dental.dao.memoria.UsuarioDAOMemoria;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.util.HashUtil;

class AuthServiceTest {

    private UsuarioDAOMemoria usuarioDAO;
    private ProfesionalDAOMemoria profesionalDAO;
    private AuthService authService;

    @BeforeEach
    void preparar() {
        usuarioDAO = new UsuarioDAOMemoria();
        profesionalDAO = new ProfesionalDAOMemoria();
        authService = new AuthService(usuarioDAO, profesionalDAO);
        usuarioDAO.crear(usuario("recepcion1", "Lucía Ramos", "clave123", Rol.RECEPCION, true));
    }

    @AfterEach
    void cerrarSesion() {
        authService.cerrarSesion();
    }

    @Test
    void conCredencialesCorrectasLlenaLaSesion() {
        Usuario usuario = authService.iniciarSesion("recepcion1", "clave123");

        assertEquals("Lucía Ramos", usuario.getNombreCompleto());
        assertTrue(Sesion.get().estaIniciada());
        assertTrue(Sesion.get().tieneRol(Rol.RECEPCION));
        assertFalse(Sesion.get().tieneRol(Rol.ADMIN, Rol.ODONTOLOGO));
        assertTrue(Sesion.get().getProfesional().isEmpty());
    }

    @Test
    void ignoraEspaciosAlrededorDelUsuario() {
        authService.iniciarSesion("  recepcion1 ", "clave123");
        assertTrue(Sesion.get().estaIniciada());
    }

    @Test
    void contrasenaIncorrectaYUsuarioInexistenteDanElMismoMensaje() {
        ReglaNegocioException contrasenaMala = assertThrows(ReglaNegocioException.class,
                () -> authService.iniciarSesion("recepcion1", "otra"));
        ReglaNegocioException usuarioInexistente = assertThrows(ReglaNegocioException.class,
                () -> authService.iniciarSesion("nadie", "clave123"));

        assertEquals(AuthService.CREDENCIALES_INCORRECTAS, contrasenaMala.getMessage());
        assertEquals(AuthService.CREDENCIALES_INCORRECTAS, usuarioInexistente.getMessage());
        assertFalse(Sesion.get().estaIniciada());
    }

    @Test
    void unUsuarioDesactivadoNoPuedeEntrar() {
        usuarioDAO.crear(usuario("antiguo", "Pedro Salas", "clave123", Rol.RECEPCION, false));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> authService.iniciarSesion("antiguo", "clave123"));

        assertEquals(AuthService.USUARIO_DESACTIVADO, error.getMessage());
        assertFalse(Sesion.get().estaIniciada());
    }

    @Test
    void unUsuarioDesactivadoConContrasenaErradaNoRevelaQueEstaDesactivado() {
        usuarioDAO.crear(usuario("antiguo", "Pedro Salas", "clave123", Rol.RECEPCION, false));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> authService.iniciarSesion("antiguo", "otra"));

        assertEquals(AuthService.CREDENCIALES_INCORRECTAS, error.getMessage());
    }

    @Test
    void elOdontologoQuedaConSuProfesionalEnLaSesion() {
        Usuario odontologo = usuarioDAO.crear(usuario("dsalinas", "Diego Salinas", "clave123", Rol.ODONTOLOGO, true));
        profesionalDAO.crear(new Profesional(0, odontologo.getIdUsuario(), "Diego", "Salinas",
                "Endodoncia", "987654321", "Lun-Vie 08:00-14:00"));

        authService.iniciarSesion("dsalinas", "clave123");

        assertEquals("Diego Salinas", Sesion.get().getProfesional().orElseThrow().getNombreCompleto());
    }

    @Test
    void cerrarSesionVaciaLaSesion() {
        authService.iniciarSesion("recepcion1", "clave123");
        authService.cerrarSesion();

        assertFalse(Sesion.get().estaIniciada());
        assertFalse(Sesion.get().tieneRol(Rol.RECEPCION));
    }

    @Test
    void cambiarContrasenaPideLaActualCorrecta() {
        int id = usuarioDAO.buscarPorNombreUsuario("recepcion1").orElseThrow().getIdUsuario();

        assertThrows(ReglaNegocioException.class, () -> authService.cambiarContrasena(id, "otra", "nueva123"));
        assertThrows(ReglaNegocioException.class, () -> authService.cambiarContrasena(id, "clave123", "corta"));

        authService.cambiarContrasena(id, "clave123", "nueva123");
        String hash = usuarioDAO.buscarPorId(id).orElseThrow().getContrasenaHash();
        assertTrue(HashUtil.verificar("nueva123", hash));
        assertFalse(HashUtil.verificar("clave123", hash));
    }

    private static Usuario usuario(String nombreUsuario, String nombre, String clave, Rol rol, boolean activo) {
        return new Usuario(0, nombreUsuario, nombre, HashUtil.hashear(clave), rol, activo);
    }
}
