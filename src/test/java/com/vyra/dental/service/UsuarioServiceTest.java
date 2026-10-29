package com.vyra.dental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        admin = usuarioDAO.crear(usuario("admin", "Administrador", Rol.ADMIN, "clave-admin"));
        Sesion.get().iniciar(admin, null);
    }

    @AfterEach
    void cerrarSesion() {
        Sesion.get().cerrar();
    }

    @Test
    void creaElUsuarioConLaContrasenaCifrada() {
        Usuario creado = servicio.crear(datos("  rquispe ", " Rosa Quispe ", Rol.RECEPCION), "secreto1");

        assertTrue(creado.getIdUsuario() > 0);
        assertEquals("rquispe", creado.getNombreUsuario());
        assertEquals("Rosa Quispe", creado.getNombreCompleto());
        assertTrue(creado.isActivo());
        assertNotEquals("secreto1", creado.getContrasenaHash());
        assertTrue(HashUtil.verificar("secreto1", creado.getContrasenaHash()));
    }

    @Test
    void exigeContrasenaDeAlMenosSeisCaracteres() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(datos("rquispe", "Rosa Quispe", Rol.RECEPCION), "12345"));

        assertEquals("contrasena", error.getCampo().orElseThrow());
    }

    @Test
    void elNombreDeUsuarioNoSeRepite() {
        servicio.crear(datos("rquispe", "Rosa Quispe", Rol.RECEPCION), "secreto1");

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(datos("rquispe", "Otra Rosa", Rol.RECEPCION), "secreto1"));

        assertEquals("nombreUsuario", error.getCampo().orElseThrow());
    }

    @Test
    void validaNombreYRol() {
        assertThrows(RuntimeException.class, () -> servicio.crear(datos("ab", "Rosa", Rol.RECEPCION), "secreto1"));
        assertThrows(RuntimeException.class, () -> servicio.crear(datos("rquispe", "  ", Rol.RECEPCION), "secreto1"));
        assertThrows(RuntimeException.class, () -> servicio.crear(datos("rquispe", "Rosa", null), "secreto1"));
    }

    @Test
    void actualizaSinTocarLaContrasena() {
        Usuario rosa = servicio.crear(datos("rquispe", "Rosa Quispe", Rol.RECEPCION), "secreto1");
        String hash = rosa.getContrasenaHash();

        Usuario cambios = datos("rquispe2", "Rosa Quispe Huamán", Rol.ODONTOLOGO);
        cambios.setIdUsuario(rosa.getIdUsuario());
        servicio.actualizar(cambios);

        Usuario guardado = usuarioDAO.buscarPorId(rosa.getIdUsuario()).orElseThrow();
        assertEquals("rquispe2", guardado.getNombreUsuario());
        assertEquals(Rol.ODONTOLOGO, guardado.getRol());
        assertEquals(hash, guardado.getContrasenaHash());
    }

    @Test
    void noSePuedeUsarElNombreDeOtroUsuarioAlActualizar() {
        servicio.crear(datos("rquispe", "Rosa Quispe", Rol.RECEPCION), "secreto1");
        Usuario juan = servicio.crear(datos("jperez", "Juan Pérez", Rol.RECEPCION), "secreto1");

        Usuario cambios = datos("rquispe", "Juan Pérez", Rol.RECEPCION);
        cambios.setIdUsuario(juan.getIdUsuario());

        assertThrows(ReglaNegocioException.class, () -> servicio.actualizar(cambios));
    }

    @Test
    void desactivaAOtroUsuarioPeroNoASiMismo() {
        Usuario rosa = servicio.crear(datos("rquispe", "Rosa Quispe", Rol.RECEPCION), "secreto1");
        Usuario desactivada = datos("rquispe", "Rosa Quispe", Rol.RECEPCION);
        desactivada.setIdUsuario(rosa.getIdUsuario());
        desactivada.setActivo(false);
        servicio.actualizar(desactivada);
        assertFalse(usuarioDAO.buscarPorId(rosa.getIdUsuario()).orElseThrow().isActivo());

        Usuario yo = datos("admin", "Administrador", Rol.ADMIN);
        yo.setIdUsuario(admin.getIdUsuario());
        yo.setActivo(false);
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> servicio.actualizar(yo));
        assertEquals("activo", error.getCampo().orElseThrow());
    }

    @Test
    void noPuedeCambiarseElRolASiMismo() {
        Usuario yo = datos("admin", "Administrador", Rol.RECEPCION);
        yo.setIdUsuario(admin.getIdUsuario());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> servicio.actualizar(yo));

        assertEquals("rol", error.getCampo().orElseThrow());
    }

    @Test
    void siempreQuedaUnAdministradorActivo() {
        Usuario otroAdmin = servicio.crear(datos("admin2", "Segundo Admin", Rol.ADMIN), "secreto1");
        // El segundo admin es la única otra cuenta: si el primero inicia sesión no puede quitarle el rol al segundo
        // y dejar al sistema sin administradores; con dos activos, sí puede bajar al segundo.
        Usuario bajar = datos("admin2", "Segundo Admin", Rol.RECEPCION);
        bajar.setIdUsuario(otroAdmin.getIdUsuario());
        servicio.actualizar(bajar);

        // Ahora solo queda el administrador de la sesión: otra cuenta ADMIN inactiva no cuenta
        Usuario inactivo = servicio.crear(datos("admin3", "Tercer Admin", Rol.ADMIN), "secreto1");
        Usuario desactivar = datos("admin3", "Tercer Admin", Rol.ADMIN);
        desactivar.setIdUsuario(inactivo.getIdUsuario());
        desactivar.setActivo(false);
        servicio.actualizar(desactivar);

        // Inicia sesión como un recepcionista "admin" simulado no es posible; se comprueba con otro administrador
        Sesion.get().iniciar(usuarioDAO.buscarPorId(inactivo.getIdUsuario()).orElseThrow(), null);
        Usuario quitarAlUnico = datos("admin", "Administrador", Rol.ODONTOLOGO);
        quitarAlUnico.setIdUsuario(admin.getIdUsuario());

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> servicio.actualizar(quitarAlUnico));
        assertEquals("rol", error.getCampo().orElseThrow());
    }

    @Test
    void noQuitaElRolOdontologoSiTieneProfesionalAsociado() {
        Usuario dr = servicio.crear(datos("msalinas", "Marco Salinas", Rol.ODONTOLOGO), "secreto1");
        profesionalDAO.crear(new Profesional(0, dr.getIdUsuario(), "Marco", "Salinas", "Endodoncia", null, null));

        Usuario cambios = datos("msalinas", "Marco Salinas", Rol.RECEPCION);
        cambios.setIdUsuario(dr.getIdUsuario());

        assertThrows(ReglaNegocioException.class, () -> servicio.actualizar(cambios));
    }

    @Test
    void restableceLaContrasenaYLaVerificaConElHashNuevo() {
        Usuario rosa = servicio.crear(datos("rquispe", "Rosa Quispe", Rol.RECEPCION), "secreto1");

        servicio.restablecerContrasena(rosa.getIdUsuario(), "nueva-clave");

        Usuario guardado = usuarioDAO.buscarPorId(rosa.getIdUsuario()).orElseThrow();
        assertTrue(HashUtil.verificar("nueva-clave", guardado.getContrasenaHash()));
        assertFalse(HashUtil.verificar("secreto1", guardado.getContrasenaHash()));
        assertThrows(ReglaNegocioException.class, () -> servicio.restablecerContrasena(rosa.getIdUsuario(), "corta"));
    }

    @Test
    void soloElAdministradorGestionaUsuarios() {
        for (Rol rol : new Rol[] {Rol.RECEPCION, Rol.ODONTOLOGO}) {
            Sesion.get().iniciar(new Usuario(9, "otro", "Otro", "pbkdf2$1$a$b", rol, true), null);

            assertThrows(ReglaNegocioException.class, servicio::listarTodos);
            assertThrows(ReglaNegocioException.class,
                    () -> servicio.crear(datos("nuevo", "Nuevo", Rol.RECEPCION), "secreto1"));
            assertThrows(ReglaNegocioException.class, () -> servicio.restablecerContrasena(1, "secreto1"));
        }
    }

    private static Usuario datos(String nombreUsuario, String nombreCompleto, Rol rol) {
        return new Usuario(0, nombreUsuario, nombreCompleto, null, rol, true);
    }

    private static Usuario usuario(String nombreUsuario, String nombreCompleto, Rol rol, String contrasena) {
        return new Usuario(0, nombreUsuario, nombreCompleto, HashUtil.hashear(contrasena), rol, true);
    }
}
