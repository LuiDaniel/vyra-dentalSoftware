package com.vyra.dental.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.ProfesionalDAO;
import com.vyra.dental.dao.UsuarioDAO;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.util.HashUtil;
import com.vyra.dental.util.Validador;

/**
 * Usuarios y roles (RF-08). Solo el administrador. Los usuarios no se borran: se desactivan
 * (RN-14). La contraseña llega en texto plano solo hasta aquí y se guarda como hash (RN-13).
 */
public class UsuarioService {

    private final UsuarioDAO usuarioDAO;
    private final ProfesionalDAO profesionalDAO;

    public UsuarioService(UsuarioDAO usuarioDAO, ProfesionalDAO profesionalDAO) {
        this.usuarioDAO = usuarioDAO;
        this.profesionalDAO = profesionalDAO;
    }

    /** Crea un usuario activo con la contraseña dada (mínimo {@value AuthService#LARGO_MINIMO_CONTRASENA} caracteres). */
    public Usuario crear(Usuario usuario, String contrasena) {
        verificarPermiso();
        normalizar(usuario);
        usuario.setActivo(true);
        verificarContrasena(contrasena);
        usuario.setContrasenaHash(HashUtil.hashear(contrasena));
        Validador.validar(usuario);
        verificarNombreLibre(usuario.getNombreUsuario(), 0);
        return usuarioDAO.crear(usuario);
    }

    /**
     * Actualiza nombre, usuario, rol y estado (la contraseña no cambia aquí). No permite: desactivarse
     * o cambiarse el rol a uno mismo, dejar el sistema sin administradores activos, ni quitar el rol
     * Odontólogo a quien tiene un profesional asociado.
     */
    public void actualizar(Usuario datos) {
        verificarPermiso();
        Usuario actual = usuarioDAO.buscarPorId(datos.getIdUsuario())
                .orElseThrow(() -> new ReglaNegocioException("El usuario ya no existe."));
        normalizar(datos);
        datos.setContrasenaHash(actual.getContrasenaHash());
        Validador.validar(datos);
        verificarNombreLibre(datos.getNombreUsuario(), datos.getIdUsuario());

        boolean esUnoMismo = Sesion.get().estaIniciada() && Sesion.get().getUsuario().getIdUsuario() == actual.getIdUsuario();
        if (esUnoMismo && !datos.isActivo()) {
            throw new ReglaNegocioException("activo", "No puedes desactivar tu propio usuario.");
        }
        if (esUnoMismo && datos.getRol() != actual.getRol()) {
            throw new ReglaNegocioException("rol", "No puedes cambiar tu propio rol.");
        }
        boolean dejaDeSerAdminActivo = actual.getRol() == Rol.ADMIN && actual.isActivo()
                && (datos.getRol() != Rol.ADMIN || !datos.isActivo());
        if (dejaDeSerAdminActivo && otrosAdministradoresActivos(actual.getIdUsuario()) == 0) {
            throw new ReglaNegocioException("rol", "Debe quedar al menos un administrador activo.");
        }
        if (actual.getRol() == Rol.ODONTOLOGO && datos.getRol() != Rol.ODONTOLOGO
                && profesionalDAO.buscarPorIdUsuario(actual.getIdUsuario()).isPresent()) {
            throw new ReglaNegocioException("rol",
                    "Este usuario está asociado a un profesional. Quita la asociación en Profesionales primero.");
        }
        usuarioDAO.actualizar(datos);
    }

    /** El administrador define una contraseña nueva para otro usuario. */
    public void restablecerContrasena(int idUsuario, String nueva) {
        verificarPermiso();
        verificarContrasena(nueva);
        Usuario usuario = usuarioDAO.buscarPorId(idUsuario)
                .orElseThrow(() -> new ReglaNegocioException("El usuario ya no existe."));
        usuario.setContrasenaHash(HashUtil.hashear(nueva));
        usuarioDAO.actualizar(usuario);
    }

    /** Por nombre completo. */
    public List<Usuario> listarTodos() {
        verificarPermiso();
        return usuarioDAO.listarTodos().stream()
                .sorted(Comparator.comparing(u -> u.getNombreCompleto().toLowerCase()))
                .toList();
    }

    public Optional<Usuario> buscarPorId(int idUsuario) {
        verificarPermiso();
        return usuarioDAO.buscarPorId(idUsuario);
    }

    public boolean puedeGestionar() {
        return Sesion.get().tieneRol(Rol.ADMIN);
    }

    private int otrosAdministradoresActivos(int idExcluido) {
        return (int) usuarioDAO.listarTodos().stream()
                .filter(u -> u.getIdUsuario() != idExcluido && u.getRol() == Rol.ADMIN && u.isActivo())
                .count();
    }

    private void verificarNombreLibre(String nombreUsuario, int idPropio) {
        boolean ocupado = usuarioDAO.buscarPorNombreUsuario(nombreUsuario)
                .filter(u -> u.getIdUsuario() != idPropio).isPresent();
        if (ocupado) {
            throw new ReglaNegocioException("nombreUsuario", "Ese nombre de usuario ya está en uso.");
        }
    }

    private static void verificarContrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < AuthService.LARGO_MINIMO_CONTRASENA) {
            throw new ReglaNegocioException("contrasena", "La contraseña debe tener al menos "
                    + AuthService.LARGO_MINIMO_CONTRASENA + " caracteres.");
        }
    }

    private static void normalizar(Usuario usuario) {
        usuario.setNombreUsuario(usuario.getNombreUsuario() == null ? null : usuario.getNombreUsuario().strip());
        usuario.setNombreCompleto(usuario.getNombreCompleto() == null ? null : usuario.getNombreCompleto().strip());
    }

    private void verificarPermiso() {
        if (!puedeGestionar()) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
    }
}
