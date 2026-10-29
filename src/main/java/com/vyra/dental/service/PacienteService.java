package com.vyra.dental.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.PacienteDAO;
import com.vyra.dental.model.FiltroPaciente;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Rol;
import com.vyra.dental.util.Validador;

/**
 * Registro, edición, baja y búsqueda de pacientes (RF-01, CU-01, RN-01 a RN-04).
 * Solo Administrador y Recepción pueden escribir; el odontólogo solo consulta.
 */
public class PacienteService {

    public static final int TAMANO_PAGINA = 20;
    static final String SIN_PERMISO = "No tienes permiso para esta acción";

    private final PacienteDAO pacienteDAO;

    public PacienteService(PacienteDAO pacienteDAO) {
        this.pacienteDAO = pacienteDAO;
    }

    public Paciente registrar(Paciente paciente) {
        verificarPermisoDeEscritura();
        normalizar(paciente);
        Validador.validar(paciente);
        verificarDniLibre(paciente);
        paciente.setActivo(true);
        if (paciente.getFechaRegistro() == null) {
            paciente.setFechaRegistro(LocalDate.now());
        }
        return pacienteDAO.crear(paciente);
    }

    public void actualizar(Paciente paciente) {
        verificarPermisoDeEscritura();
        normalizar(paciente);
        Validador.validar(paciente);
        verificarDniLibre(paciente);
        if (!pacienteDAO.actualizar(paciente)) {
            throw new ReglaNegocioException("El paciente ya no existe.");
        }
    }

    /** Baja lógica (RN-04): deja de aparecer en las búsquedas, pero su historial se conserva. */
    public void darDeBaja(int idPaciente) {
        verificarPermisoDeEscritura();
        if (!pacienteDAO.darDeBaja(idPaciente)) {
            throw new ReglaNegocioException("El paciente no existe o ya fue dado de baja.");
        }
    }

    /** Página {@code pagina} (desde 0) de los pacientes activos que coinciden con el texto. */
    public List<Paciente> buscar(String texto, FiltroPaciente filtro, int pagina) {
        int desde = Math.max(pagina, 0) * TAMANO_PAGINA;
        return pacienteDAO.buscar(texto, filtro, TAMANO_PAGINA, desde);
    }

    public int contar(String texto, FiltroPaciente filtro) {
        return pacienteDAO.contarActivos(texto, filtro);
    }

    public Optional<Paciente> buscarPorId(int idPaciente) {
        return pacienteDAO.buscarPorId(idPaciente);
    }

    public boolean puedeEditar() {
        return Sesion.get().tieneRol(Rol.ADMIN, Rol.RECEPCION);
    }

    private void verificarPermisoDeEscritura() {
        if (!puedeEditar()) {
            throw new ReglaNegocioException(SIN_PERMISO);
        }
    }

    /** RN-01: el DNI no se repite, ni siquiera con un paciente dado de baja. */
    private void verificarDniLibre(Paciente paciente) {
        pacienteDAO.buscarPorDni(paciente.getDni())
                .filter(otro -> otro.getIdPaciente() != paciente.getIdPaciente())
                .ifPresent(otro -> {
                    String detalle = otro.isActivo() ? "" : " (dado de baja)";
                    throw new ReglaNegocioException("dni",
                            "Ya existe un paciente con el DNI " + paciente.getDni() + detalle);
                });
    }

    /** Quita espacios sobrantes; el teléfono se acepta con espacios ("987 654 321"). */
    private static void normalizar(Paciente paciente) {
        paciente.setNombres(limpiar(paciente.getNombres()));
        paciente.setApellidos(limpiar(paciente.getApellidos()));
        paciente.setDni(limpiar(paciente.getDni()));
        String telefono = limpiar(paciente.getTelefono());
        paciente.setTelefono(telefono == null ? null : telefono.replace(" ", ""));
        paciente.setDireccion(limpiar(paciente.getDireccion()));
        paciente.setContactoEmergencia(limpiar(paciente.getContactoEmergencia()));
        paciente.setAlergias(limpiar(paciente.getAlergias()));
        paciente.setAntecedentes(limpiar(paciente.getAntecedentes()));
    }

    /** Texto sin espacios a los lados; vacío se guarda como null. */
    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.strip();
        return limpio.isEmpty() ? null : limpio;
    }
}
