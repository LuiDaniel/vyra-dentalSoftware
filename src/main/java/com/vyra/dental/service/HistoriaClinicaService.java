package com.vyra.dental.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.vyra.dental.dao.CitaDAO;
import com.vyra.dental.dao.HistoriaClinicaDAO;
import com.vyra.dental.dao.PacienteDAO;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.HistoriaClinicaEntrada;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.util.Validador;

/**
 * Historia clínica del paciente (RF-04, CU-04, RN-09): las entradas de cada
 * visita (diagnóstico y observaciones) y los datos clínicos generales del
 * paciente (alergias y antecedentes). Solo el odontólogo escribe; el
 * administrador la consulta. Las entradas no se editan ni se borran.
 */
public class HistoriaClinicaService {

    private final HistoriaClinicaDAO historiaDAO;
    private final PacienteDAO pacienteDAO;
    private final CitaDAO citaDAO;
    /** Fecha de las entradas; las pruebas usan un reloj fijo. */
    private final Clock reloj;

    public HistoriaClinicaService(HistoriaClinicaDAO historiaDAO, PacienteDAO pacienteDAO, CitaDAO citaDAO) {
        this(historiaDAO, pacienteDAO, citaDAO, Clock.systemDefaultZone());
    }

    public HistoriaClinicaService(HistoriaClinicaDAO historiaDAO, PacienteDAO pacienteDAO, CitaDAO citaDAO,
                                  Clock reloj) {
        this.historiaDAO = historiaDAO;
        this.pacienteDAO = pacienteDAO;
        this.citaDAO = citaDAO;
        this.reloj = reloj;
    }

    /**
     * Agrega una entrada con la fecha de hoy y el profesional de la sesión.
     * Debe tener diagnóstico u observaciones; si indica una cita, debe ser del
     * mismo paciente y no estar cancelada.
     */
    public HistoriaClinicaEntrada registrarEntrada(HistoriaClinicaEntrada entrada) {
        verificarPermiso();
        entrada.setDiagnostico(limpiar(entrada.getDiagnostico()));
        entrada.setObservaciones(limpiar(entrada.getObservaciones()));
        if (entrada.getDiagnostico() == null && entrada.getObservaciones() == null) {
            throw new ReglaNegocioException("diagnostico", "Escribe el diagnóstico o las observaciones de la visita.");
        }
        buscarPaciente(entrada.getIdPaciente());
        if (entrada.getIdCita() != null) {
            verificarCita(entrada);
        }
        // La fecha la pone el sistema: no se registran entradas con otra fecha
        entrada.setFecha(LocalDate.now(reloj));
        entrada.setIdProfesional(Sesion.get().getProfesional().map(Profesional::getIdProfesional).orElse(null));
        Validador.validar(entrada);
        return historiaDAO.crear(entrada);
    }

    /** Entradas del paciente, de la más reciente a la más antigua. */
    public List<HistoriaClinicaEntrada> historial(int idPaciente) {
        return historiaDAO.listarPorPaciente(idPaciente);
    }

    /**
     * Alergias y antecedentes del paciente (CU-04, paso 3). Son datos generales
     * del paciente y se sobrescriben, a diferencia de las entradas.
     */
    public Paciente actualizarDatosClinicos(int idPaciente, String alergias, String antecedentes) {
        verificarPermiso();
        Paciente paciente = buscarPaciente(idPaciente);
        paciente.setAlergias(limpiar(alergias));
        paciente.setAntecedentes(limpiar(antecedentes));
        Validador.validar(paciente);
        if (!pacienteDAO.actualizar(paciente)) {
            throw new ReglaNegocioException("El paciente ya no existe.");
        }
        return paciente;
    }

    public boolean puedeEditar() {
        return Sesion.get().tieneRol(Rol.ODONTOLOGO);
    }

    private void verificarCita(HistoriaClinicaEntrada entrada) {
        Cita cita = citaDAO.buscarPorId(entrada.getIdCita())
                .orElseThrow(() -> new ReglaNegocioException("idCita", "La cita elegida no existe."));
        if (cita.getPaciente().getIdPaciente() != entrada.getIdPaciente()) {
            throw new ReglaNegocioException("idCita", "La cita elegida es de otro paciente.");
        }
        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new ReglaNegocioException("idCita", "La cita elegida está cancelada.");
        }
    }

    private Paciente buscarPaciente(int idPaciente) {
        return pacienteDAO.buscarPorId(idPaciente)
                .orElseThrow(() -> new ReglaNegocioException("El paciente no existe."));
    }

    private void verificarPermiso() {
        if (!puedeEditar()) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.strip();
        return limpio.isEmpty() ? null : limpio;
    }
}
