package com.vyra.dental.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.vyra.dental.dao.CitaDAO;
import com.vyra.dental.dao.ProfesionalDAO;
import com.vyra.dental.dao.UsuarioDAO;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Usuario;
import com.vyra.dental.util.Validador;

/**
 * Registro de profesionales (RF-07, CU-07). Solo el Administrador escribe;
 * todos los roles pueden listarlos (la agenda los necesita).
 */
public class ProfesionalService {

    private final ProfesionalDAO profesionalDAO;
    private final UsuarioDAO usuarioDAO;
    private final CitaDAO citaDAO;

    public ProfesionalService(ProfesionalDAO profesionalDAO, UsuarioDAO usuarioDAO, CitaDAO citaDAO) {
        this.profesionalDAO = profesionalDAO;
        this.usuarioDAO = usuarioDAO;
        this.citaDAO = citaDAO;
    }

    public Profesional registrar(Profesional profesional) {
        verificarPermiso();
        normalizar(profesional);
        Validador.validar(profesional);
        verificarUsuario(profesional);
        return profesionalDAO.crear(profesional);
    }

    public void actualizar(Profesional profesional) {
        verificarPermiso();
        normalizar(profesional);
        Validador.validar(profesional);
        verificarUsuario(profesional);
        if (!profesionalDAO.actualizar(profesional)) {
            throw new ReglaNegocioException("El profesional ya no existe.");
        }
    }

    /** Solo si no tiene citas: las citas guardan quién las atendió. */
    public void eliminar(int idProfesional) {
        verificarPermiso();
        if (citaDAO.contarPorProfesional(idProfesional) > 0) {
            throw new ReglaNegocioException("No se puede eliminar: el profesional tiene citas registradas.");
        }
        if (!profesionalDAO.eliminar(idProfesional)) {
            throw new ReglaNegocioException("El profesional ya no existe.");
        }
    }

    /** Ordenados por apellidos. */
    public List<Profesional> listarTodos() {
        return profesionalDAO.listarTodos();
    }

    public Optional<Profesional> buscarPorId(int idProfesional) {
        return profesionalDAO.buscarPorId(idProfesional);
    }

    /**
     * Usuarios activos con rol Odontólogo que todavía no tienen profesional,
     * más el que ya tiene el profesional indicado (0 si es nuevo).
     */
    public List<Usuario> usuariosDisponibles(int idProfesional) {
        Set<Integer> ocupados = profesionalDAO.listarTodos().stream()
                .filter(p -> p.getIdUsuario() != null && p.getIdProfesional() != idProfesional)
                .map(Profesional::getIdUsuario)
                .collect(Collectors.toSet());
        return usuarioDAO.listarTodos().stream()
                .filter(u -> u.getRol() == Rol.ODONTOLOGO && u.isActivo() && !ocupados.contains(u.getIdUsuario()))
                .sorted(Comparator.comparing(Usuario::getNombreCompleto))
                .toList();
    }

    /** La cuenta de acceso asociada al profesional, si tiene una. */
    public Optional<Usuario> usuarioDe(Profesional profesional) {
        return profesional.getIdUsuario() == null ? Optional.empty() : usuarioDAO.buscarPorId(profesional.getIdUsuario());
    }

    public boolean puedeEditar() {
        return Sesion.get().tieneRol(Rol.ADMIN);
    }

    private void verificarPermiso() {
        if (!puedeEditar()) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
    }

    /** La cuenta asociada debe ser de un odontólogo y no estar asociada a otro profesional. */
    private void verificarUsuario(Profesional profesional) {
        Integer idUsuario = profesional.getIdUsuario();
        if (idUsuario == null) {
            return;
        }
        Usuario usuario = usuarioDAO.buscarPorId(idUsuario)
                .orElseThrow(() -> new ReglaNegocioException("idUsuario", "El usuario elegido no existe."));
        if (usuario.getRol() != Rol.ODONTOLOGO) {
            throw new ReglaNegocioException("idUsuario", "Solo se puede asociar un usuario con rol Odontólogo.");
        }
        profesionalDAO.buscarPorIdUsuario(idUsuario)
                .filter(otro -> otro.getIdProfesional() != profesional.getIdProfesional())
                .ifPresent(otro -> {
                    throw new ReglaNegocioException("idUsuario",
                            "Ese usuario ya está asociado a " + otro.getNombreCompleto() + ".");
                });
    }

    private static void normalizar(Profesional profesional) {
        profesional.setNombres(limpiar(profesional.getNombres()));
        profesional.setApellidos(limpiar(profesional.getApellidos()));
        profesional.setEspecialidad(limpiar(profesional.getEspecialidad()));
        String telefono = limpiar(profesional.getTelefono());
        profesional.setTelefono(telefono == null ? null : telefono.replace(" ", ""));
        profesional.setHorarioAtencion(limpiar(profesional.getHorarioAtencion()));
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.strip();
        return limpio.isEmpty() ? null : limpio;
    }
}
