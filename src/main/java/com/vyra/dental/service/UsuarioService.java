package com.vyra.dental.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.ProfesionalDAO;
import com.vyra.dental.dao.UsuarioDAO;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.util.HashUtil;
import com.vyra.dental.util.Validador;

/**
 * Usuarios y roles (RF-08, RN-13, RN-14). Solo el Administrador. Los usuarios
 * no se borran: se desactivan, y un usuario desactivado no puede iniciar
 * sesión. Las contraseñas se guardan con HashUtil (PBKDF2), nunca en texto plano.
 */
public class UsuarioService {

    private final UsuarioDAO usuarioDAO;
    private final ProfesionalDAO profesionalDAO;

    public UsuarioService(UsuarioDAO usuarioDAO, ProfesionalDAO profesionalDAO) {
        this.usuarioDAO = usuarioDAO;
        this.profesionalDAO = profesionalDAO;
    }

    /** Ordenados por nombre completo. */
    public List<Usuario> listarTodos() {
        return usuarioDAO.listarTodos().stream()
                .sorted(Comparator.comparing(Usuario::getNombreCompleto, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Crea el usuario activo, con la contraseña escrita guardada como hash. */
    public Usuario crear(Usuario usuario, String contrasena) {
        verificarPermiso();
        normalizar(usuario);
        usuario.setActivo(true);
        // Primero los datos del formulario (todos sus errores juntos); el hash se calcula al final
        usuario.setContrasenaHash("pendiente");
        Validador.validar(usuario);
        verificarNombreLibre(usuario);
        verificarContrasena(contrasena);
        usuario.setContrasenaHash(HashUtil.hashear(contrasena));
        return usuarioDAO.crear(usuario);
    }

    /**
     * Cambia nombre, usuario, rol y estado; la contraseña se conserva. Un
     * administrador no puede desactivarse ni quitarse el rol a sí mismo (así
     * siempre queda al menos un administrador activo), y el rol de un usuario
     * asociado a un profesional no puede dejar de ser Odontólogo.
     */
    public void actualizar(Usuario usuario) {
        verificarPermiso();
        Usuario guardado = usuarioDAO.buscarPorId(usuario.getIdUsuario())
                .orElseThrow(() -> new ReglaNegocioException("El usuario ya no existe."));
        normalizar(usuario);
        usuario.setContrasenaHash(guardado.getContrasenaHash());
        Validador.validar(usuario);
        verificarNombreLibre(usuario);
        if (esUsuarioDeLaSesion(usuario.getIdUsuario())) {
            if (!usuario.isActivo()) {
                throw new ReglaNegocioException("activo", "No puedes desactivar tu propio usuario.");
            }
            if (usuario.getRol() != Rol.ADMIN) {
                throw new ReglaNegocioException("rol", "No puedes quitarte el rol de Administrador.");
            }
        }
        if (usuario.getRol() != Rol.ODONTOLOGO) {
            profesionalDe(usuario.getIdUsuario()).ifPresent(p -> {
                throw new ReglaNegocioException("rol", "Este usuario está asociado al profesional "
                        + p.getNombreCompleto() + ". Quita la asociación en Profesionales antes de cambiar su rol.");
            });
        }
        usuarioDAO.actualizar(usuario);
    }

    /** El administrador asigna una contraseña nueva (por ejemplo, si el usuario la olvidó). */
    public void restablecerContrasena(int idUsuario, String nueva) {
        verificarPermiso();
        Usuario usuario = usuarioDAO.buscarPorId(idUsuario)
                .orElseThrow(() -> new ReglaNegocioException("El usuario ya no existe."));
        verificarContrasena(nueva);
        usuario.setContrasenaHash(HashUtil.hashear(nueva));
        usuarioDAO.actualizar(usuario);
    }

    /** El profesional asociado a la cuenta, si tiene uno. */
    public Optional<Profesional> profesionalDe(int idUsuario) {
        return profesionalDAO.buscarPorIdUsuario(idUsuario);
    }

    public boolean puedeEditar() {
        return Sesion.get().tieneRol(Rol.ADMIN);
    }

    public boolean esUsuarioDeLaSesion(int idUsuario) {
        return Sesion.get().estaIniciada() && Sesion.get().getUsuario().getIdUsuario() == idUsuario;
    }

    private void verificarNombreLibre(Usuario usuario) {
        usuarioDAO.buscarPorNombreUsuario(usuario.getNombreUsuario())
                .filter(otro -> otro.getIdUsuario() != usuario.getIdUsuario())
                .ifPresent(otro -> {
                    throw new ReglaNegocioException("nombreUsuario",
                            "Ya existe el usuario \"" + usuario.getNombreUsuario() + "\".");
                });
    }

    private static void verificarContrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < AuthService.LARGO_MINIMO_CONTRASENA) {
            throw new ReglaNegocioException("contrasena",
                    "La contraseña debe tener al menos " + AuthService.LARGO_MINIMO_CONTRASENA + " caracteres.");
        }
    }

    /**
     * Sin espacios a los lados. El usuario no puede tener espacios internos;
     * se respetan las mayúsculas porque el inicio de sesión las distingue.
     */
    private static void normalizar(Usuario usuario) {
        usuario.setNombreCompleto(usuario.getNombreCompleto() == null ? null : usuario.getNombreCompleto().strip());
        String nombre = usuario.getNombreUsuario() == null ? null : usuario.getNombreUsuario().strip();
        usuario.setNombreUsuario(nombre);
        if (nombre != null && nombre.contains(" ")) {
            throw new ReglaNegocioException("nombreUsuario", "El usuario no puede tener espacios.");
        }
    }

    private void verificarPermiso() {
        if (!puedeEditar()) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
    }
}
