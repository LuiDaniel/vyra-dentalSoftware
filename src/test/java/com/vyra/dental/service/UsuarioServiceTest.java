package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vyra.dental.dao.memoria.ProfesionalDAOMemoria;
import com.vyra.dental.dao.memoria.UsuarioDAOMemoria;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.util.HashUtil;

class UsuarioServiceTest {

    private UsuarioDAOMemoria usuarioDAO;
    private ProfesionalDAOMemoria profesionalDAO;
    private UsuarioService servicio;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        usuarioDAO = new UsuarioDAOMemoria();
        profesionalDAO = new ProfesionalDAOMemoria();
        servicio = new UsuarioService(usuarioDAO, profesionalDAO);
        admin = usuarioDAO.crear(new Usuario(0, "admin", "Administrador", HashUtil.hashear("admin123"), Rol.ADMIN, true));
        Sesion.get().iniciar(admin, null);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void creaElUsuarioActivoConLaContrasenaHasheada() {
        Usuario creado = servicio.crear(usuario("  rmendoza ", " Rocío Mendoza Paz ", Rol.RECEPCION), "clave123");

        Usuario guardado = usuarioDAO.buscarPorId(creado.getIdUsuario()).orElseThrow();
        assertEquals("rmendoza", guardado.getNombreUsuario());
        assertEquals("Rocío Mendoza Paz", guardado.getNombreCompleto());
        assertTrue(guardado.isActivo());
        assertNotEquals("clave123", guardado.getContrasenaHash());
        assertTrue(HashUtil.verificar("clave123", guardado.getContrasenaHash()));
    }

    @Test
    void elUsuarioNoSeRepiteNiTieneEspacios() {
        assertEquals("nombreUsuario", assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(usuario("admin", "Otro", Rol.RECEPCION), "clave123")).getCampo().orElseThrow());
        assertEquals("nombreUsuario", assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(usuario("rocio m", "Rocío", Rol.RECEPCION), "clave123")).getCampo().orElseThrow());
    }

    @Test
    void laContrasenaTieneUnLargoMinimo() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(usuario("rocio", "Rocío", Rol.RECEPCION), "12345"));

        assertEquals("contrasena", error.getCampo().orElseThrow());
        assertEquals(1, usuarioDAO.contar());
    }

    @Test
    void actualizarConservaLaContrasenaYRestablecerlaLaCambia() {
        Usuario rocio = servicio.crear(usuario("rocio", "Rocío", Rol.RECEPCION), "clave123");
        Usuario cambios = usuario("rocio", "Rocío Mendoza", Rol.RECEPCION);
        cambios.setIdUsuario(rocio.getIdUsuario());
        cambios.setActivo(false);

        servicio.actualizar(cambios);
        Usuario guardado = usuarioDAO.buscarPorId(rocio.getIdUsuario()).orElseThrow();
        assertEquals("Rocío Mendoza", guardado.getNombreCompleto());
        assertFalse(guardado.isActivo());
        assertTrue(HashUtil.verificar("clave123", guardado.getContrasenaHash()));

        servicio.restablecerContrasena(rocio.getIdUsuario(), "nueva456");
        assertTrue(HashUtil.verificar("nueva456",
                usuarioDAO.buscarPorId(rocio.getIdUsuario()).orElseThrow().getContrasenaHash()));
    }

    @Test
    void unAdministradorNoPuedeDesactivarseNiQuitarseElRol() {
        Usuario desactivado = new Usuario(admin.getIdUsuario(), "admin", "Administrador", null, Rol.ADMIN, false);
        Usuario recepcion = new Usuario(admin.getIdUsuario(), "admin", "Administrador", null, Rol.RECEPCION, true);

        assertEquals("activo", assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizar(desactivado)).getCampo().orElseThrow());
        assertEquals("rol", assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizar(recepcion)).getCampo().orElseThrow());
        Usuario guardado = usuarioDAO.buscarPorId(admin.getIdUsuario()).orElseThrow();
        assertTrue(guardado.isActivo());
        assertEquals(Rol.ADMIN, guardado.getRol());
    }

    @Test
    void elRolDeUnOdontologoAsociadoAUnProfesionalNoCambia() {
        Usuario odontologo = servicio.crear(usuario("msalinas", "Marco Salinas", Rol.ODONTOLOGO), "clave123");
        profesionalDAO.crear(new Profesional(0, odontologo.getIdUsuario(), "Marco", "Salinas", null, null, null));
        Usuario cambios = usuario("msalinas", "Marco Salinas", Rol.RECEPCION);
        cambios.setIdUsuario(odontologo.getIdUsuario());

        assertEquals("rol", assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizar(cambios)).getCampo().orElseThrow());
        assertEquals("Salinas", servicio.profesionalDe(odontologo.getIdUsuario()).orElseThrow().getApellidos());
    }

    @Test
    void soloElAdministradorGestionaUsuarios() {
        Sesion.get().iniciar(new Usuario(9, "rocio", "Rocío", "x", Rol.RECEPCION, true), null);

        assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(usuario("otro", "Otro", Rol.RECEPCION), "clave123"));
        assertThrows(ReglaNegocioException.class, () -> servicio.restablecerContrasena(admin.getIdUsuario(), "x123456"));
    }

    @Test
    void listaPorNombreCompleto() {
        servicio.crear(usuario("zoila", "Zoila Ramos", Rol.RECEPCION), "clave123");
        servicio.crear(usuario("beto", "beto Paz", Rol.ODONTOLOGO), "clave123");

        assertEquals(List.of("Administrador", "beto Paz", "Zoila Ramos"),
                servicio.listarTodos().stream().map(Usuario::getNombreCompleto).toList());
    }

    private static Usuario usuario(String nombreUsuario, String nombreCompleto, Rol rol) {
        return new Usuario(0, nombreUsuario, nombreCompleto, null, rol, true);
    }
}
