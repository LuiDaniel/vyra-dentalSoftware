package com.vyra.dental.service;

import java.util.Optional;

import com.vyra.dental.dao.ProfesionalDAO;
import com.vyra.dental.dao.UsuarioDAO;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.util.HashUtil;

/**
 * Inicio y cierre de sesión (CU-08, RN-13, RN-14).
 */
public class AuthService {

    public static final String CREDENCIALES_INCORRECTAS = "Usuario o contraseña incorrectos.";
    public static final String USUARIO_DESACTIVADO = "Tu usuario está desactivado. Consulta con el administrador.";
    static final int LARGO_MINIMO_CONTRASENA = 6;

    // Si el usuario no existe se verifica contra este hash, para que la respuesta
    // tarde lo mismo y no delate qué nombres de usuario existen
    private static final String HASH_FICTICIO = HashUtil.hashear("vyra-usuario-inexistente");

    private final UsuarioDAO usuarioDAO;
    private final ProfesionalDAO profesionalDAO;

    public AuthService(UsuarioDAO usuarioDAO, ProfesionalDAO profesionalDAO) {
        this.usuarioDAO = usuarioDAO;
        this.profesionalDAO = profesionalDAO;
    }

    /**
     * Valida las credenciales y llena {@link Sesion}. El mensaje de error no
     * dice cuál de los dos datos falló (RN-13).
     */
    public Usuario iniciarSesion(String nombreUsuario, String contrasena) {
        String nombre = nombreUsuario == null ? "" : nombreUsuario.strip();
        Optional<Usuario> encontrado = usuarioDAO.buscarPorNombreUsuario(nombre);
        String hash = encontrado.map(Usuario::getContrasenaHash).orElse(HASH_FICTICIO);
        if (!HashUtil.verificar(contrasena, hash) || encontrado.isEmpty()) {
            throw new ReglaNegocioException(CREDENCIALES_INCORRECTAS);
        }
        Usuario usuario = encontrado.get();
        if (!usuario.isActivo()) {
            throw new ReglaNegocioException(USUARIO_DESACTIVADO);
        }
        Profesional profesional = usuario.getRol() == Rol.ODONTOLOGO
                ? profesionalDAO.buscarPorIdUsuario(usuario.getIdUsuario()).orElse(null)
                : null;
        Sesion.get().iniciar(usuario, profesional);
        return usuario;
    }

    public void cerrarSesion() {
        Sesion.get().cerrar();
    }

    public void cambiarContrasena(int idUsuario, String actual, String nueva) {
        Usuario usuario = usuarioDAO.buscarPorId(idUsuario)
                .orElseThrow(() -> new ReglaNegocioException("El usuario no existe."));
        if (!HashUtil.verificar(actual, usuario.getContrasenaHash())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta.");
        }
        if (nueva == null || nueva.length() < LARGO_MINIMO_CONTRASENA) {
            throw new ReglaNegocioException(
                    "La nueva contraseña debe tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres.");
        }
        usuario.setContrasenaHash(HashUtil.hashear(nueva));
        usuarioDAO.actualizar(usuario);
    }
}
