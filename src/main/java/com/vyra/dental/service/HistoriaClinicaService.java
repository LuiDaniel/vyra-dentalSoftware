package com.vyra.dental.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.CitaDAO;
import com.vyra.dental.dao.HistoriaClinicaDAO;
import com.vyra.dental.dao.PacienteDAO;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.HistoriaClinicaEntrada;
import com.vyra.dental.model.Paciente;
import com.vyra.dental.model.Rol;
import com.vyra.dental.util.Validador;

/**
 * Historia clínica del paciente (RF-04, CU-04, RN-09). El odontólogo escribe;
 * el administrador solo consulta; recepción no tiene acceso.
 * Las entradas no se editan ni se borran: cada cambio es una entrada nueva.
 */
public class HistoriaClinicaService {

    private final HistoriaClinicaDAO historiaDAO;
    private final PacienteDAO pacienteDAO;
    private final CitaDAO citaDAO;
    /** Fecha "actual"; las pruebas usan un reloj fijo. */
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
     * Agrega una entrada con la fecha de hoy. Se necesita al menos un
     * diagnóstico o unas observaciones; si trae cita, debe ser del mismo paciente.
     */
    public HistoriaClinicaEntrada registrarEntrada(HistoriaClinicaEntrada entrada) {
        verificarEscritura();
        entrada.setFecha(LocalDate.now(reloj));
        entrada.setDiagnostico(limpiar(entrada.getDiagnostico()));
        entrada.setObservaciones(limpiar(entrada.getObservaciones()));
        Validador.validar(entrada);
        if (entrada.getDiagnostico() == null && entrada.getObservaciones() == null) {
            throw new ReglaNegocioException("diagnostico", "Escribe un diagnóstico o unas observaciones.");
        }
        Paciente paciente = pacienteActivo(entrada.getIdPaciente());
        if (entrada.getIdCita() != null) {
            Cita cita = citaDAO.buscarPorId(entrada.getIdCita())
                    .orElseThrow(() -> new ReglaNegocioException("La cita elegida no existe."));
            if (cita.getPaciente().getIdPaciente() != paciente.getIdPaciente()) {
                throw new ReglaNegocioException("La cita elegida no es de este paciente.");
            }
        }
        return historiaDAO.crear(entrada);
    }

    /** Actualiza solo las alergias y los antecedentes del paciente (el odontólogo no edita el resto de la ficha). */
    public void actualizarDatosClinicos(int idPaciente, String alergias, String antecedentes) {
        verificarEscritura();
        Paciente paciente = pacienteActivo(idPaciente);
        paciente.setAlergias(limpiar(alergias));
        paciente.setAntecedentes(limpiar(antecedentes));
        Validador.validar(paciente);
        if (!pacienteDAO.actualizar(paciente)) {
            throw new ReglaNegocioException("El paciente ya no existe.");
        }
    }

    /** De la más reciente a la más antigua. */
    public List<HistoriaClinicaEntrada> entradasDe(int idPaciente) {
        verificarLectura();
        return historiaDAO.listarPorPaciente(idPaciente);
    }

    /** La cita en la que se registró una entrada, para mostrar quién atendió. */
    public Optional<Cita> citaDe(HistoriaClinicaEntrada entrada) {
        return entrada.getIdCita() == null ? Optional.empty() : citaDAO.buscarPorId(entrada.getIdCita());
    }

    /** La cita del paciente que se está atendiendo hoy, si hay una pendiente o confirmada. */
    public Optional<Cita> citaDeHoy(int idPaciente) {
        LocalDate hoy = LocalDate.now(reloj);
        return citaDAO.listarPorPaciente(idPaciente).stream()
                .filter(c -> c.getFecha().equals(hoy))
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA)
                .filter(c -> Sesion.get().getProfesional()
                        .map(p -> p.getIdProfesional() == c.getProfesional().getIdProfesional())
                        .orElse(true))
                .findFirst();
    }

    public boolean puedeVer() {
        return Sesion.get().tieneRol(Rol.ADMIN, Rol.ODONTOLOGO);
    }

    public boolean puedeEscribir() {
        return Sesion.get().tieneRol(Rol.ODONTOLOGO);
    }

    private void verificarLectura() {
        if (!puedeVer()) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
    }

    private void verificarEscritura() {
        if (!puedeEscribir()) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
    }

    private Paciente pacienteActivo(int idPaciente) {
        return pacienteDAO.buscarPorId(idPaciente)
                .filter(Paciente::isActivo)
                .orElseThrow(() -> new ReglaNegocioException("El paciente no existe o fue dado de baja."));
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.strip();
        return limpio.isEmpty() ? null : limpio;
    }
}
