package com.vyra.dental.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vyra.dental.dao.CitaDAO;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.util.Formatos;
import com.vyra.dental.util.Validador;

/**
 * Agenda: agendar, reprogramar, confirmar, cancelar y marcar atendidas las citas
 * (RF-02, RF-03, CU-02, CU-03, RN-05 a RN-08).
 * Administrador y Recepción gestionan todas las citas; el odontólogo solo ve
 * las suyas y puede marcarlas como atendidas.
 */
public class CitaService {

    /** Horario de atención y tamaño de los bloques (RN-05). */
    public static final LocalTime APERTURA = LocalTime.of(8, 0);
    public static final LocalTime CIERRE = LocalTime.of(20, 0);
    public static final int MINUTOS_BLOQUE = 30;

    private final CitaDAO citaDAO;
    /** Fecha y hora "actuales"; las pruebas usan un reloj fijo. */
    private final Clock reloj;

    public CitaService(CitaDAO citaDAO) {
        this(citaDAO, Clock.systemDefaultZone());
    }

    public CitaService(CitaDAO citaDAO, Clock reloj) {
        this.citaDAO = citaDAO;
        this.reloj = reloj;
    }

    // ===== Escritura =====

    public Cita agendar(Cita cita) {
        verificarGestion();
        if (cita.getEstado() == null) {
            cita.setEstado(EstadoCita.PENDIENTE);
        }
        if (cita.getEstado() != EstadoCita.PENDIENTE && cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new ReglaNegocioException("Una cita nueva solo puede quedar pendiente o confirmada.");
        }
        cita.setMotivo(limpiar(cita.getMotivo()));
        validar(cita);
        return citaDAO.crear(cita);
    }

    /** Cambia paciente, profesional, fecha, horas o motivo de una cita pendiente o confirmada. */
    public void reprogramar(Cita cita) {
        verificarGestion();
        Cita actual = buscarActiva(cita.getIdCita());
        cita.setEstado(actual.getEstado());
        cita.setMotivo(limpiar(cita.getMotivo()));
        validar(cita);
        citaDAO.actualizar(cita);
    }

    public void confirmar(int idCita) {
        verificarGestion();
        Cita cita = buscar(idCita);
        if (cita.getEstado() != EstadoCita.PENDIENTE) {
            throw new ReglaNegocioException("Solo se puede confirmar una cita pendiente.");
        }
        citaDAO.actualizarEstado(idCita, EstadoCita.CONFIRMADA);
    }

    /** RN-07: la cita no se borra, queda cancelada y libera el horario. */
    public void cancelar(int idCita) {
        verificarGestion();
        buscarActiva(idCita);
        citaDAO.actualizarEstado(idCita, EstadoCita.CANCELADA);
    }

    public void marcarAtendida(int idCita) {
        Cita cita = buscarActiva(idCita);
        if (!puedeMarcarAtendida(cita)) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
        if (cita.getFecha().isAfter(hoy())) {
            throw new ReglaNegocioException("No se puede marcar como atendida una cita de una fecha futura.");
        }
        citaDAO.actualizarEstado(idCita, EstadoCita.ATENDIDA);
    }

    // ===== Validación de horario (CU-03) =====

    /** RN-06: true si el profesional ya tiene otra cita pendiente o confirmada que se traslapa. */
    public boolean existeCruceDeHorario(Cita cita) {
        return buscarCruce(cita).isPresent();
    }

    /** La cita con la que se cruza, para explicar el error. */
    public Optional<Cita> buscarCruce(Cita cita) {
        if (cita.getProfesional() == null || cita.getFecha() == null
                || cita.getHoraInicio() == null || cita.getHoraFin() == null) {
            return Optional.empty();
        }
        return citaDAO.buscarCruce(cita.getProfesional().getIdProfesional(), cita.getFecha(),
                cita.getHoraInicio(), cita.getHoraFin(), cita.getIdCita());
    }

    /** "Marco Salinas ya tiene una cita con María Quispe de 09:00 a 10:00." */
    public static String mensajeCruce(Profesional profesional, Cita conflicto) {
        return profesional.getNombreCompleto() + " ya tiene una cita con " + conflicto.getPaciente().getNombreCompleto()
                + " de " + Formatos.hora(conflicto.getHoraInicio()) + " a " + Formatos.hora(conflicto.getHoraFin()) + ".";
    }

    /**
     * Horas de inicio (cada 30 min, de 08:00 a 20:00) en las que el profesional
     * puede atender una cita de la duración dada sin cruzarse con otra. Si la
     * fecha es hoy, se omiten las horas que ya pasaron.
     */
    public List<LocalTime> horariosLibres(int idProfesional, LocalDate fecha, int duracionMinutos) {
        return horariosLibres(idProfesional, fecha, duracionMinutos, 0);
    }

    /** Igual que el anterior, sin contar la cita que se está reprogramando. */
    public List<LocalTime> horariosLibres(int idProfesional, LocalDate fecha, int duracionMinutos, int excluirId) {
        List<LocalTime> libres = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        for (LocalTime inicio = APERTURA; !inicio.plusMinutes(duracionMinutos).isAfter(CIERRE);
             inicio = inicio.plusMinutes(MINUTOS_BLOQUE)) {
            if (fecha.atTime(inicio).isBefore(ahora)) {
                continue;
            }
            if (!citaDAO.existeCruce(idProfesional, fecha, inicio, inicio.plusMinutes(duracionMinutos), excluirId)) {
                libres.add(inicio);
            }
        }
        return libres;
    }

    /** Horas posibles de inicio: 08:00, 08:30 ... 19:30. */
    public static List<LocalTime> horasDeInicio() {
        List<LocalTime> horas = new ArrayList<>();
        for (LocalTime hora = APERTURA; hora.isBefore(CIERRE); hora = hora.plusMinutes(MINUTOS_BLOQUE)) {
            horas.add(hora);
        }
        return horas;
    }

    // ===== Consultas =====

    /** Citas del día; el odontólogo solo ve las suyas. */
    public List<Cita> citasDelDia(LocalDate fecha) {
        return filtrarPorRol(citaDAO.listarPorFecha(fecha));
    }

    /** Citas entre dos fechas (ambas incluidas); el odontólogo solo ve las suyas. */
    public List<Cita> citasEntre(LocalDate desde, LocalDate hasta) {
        return filtrarPorRol(citaDAO.listarEntre(desde, hasta));
    }

    public List<Cita> citasDePaciente(int idPaciente) {
        return filtrarPorRol(citaDAO.listarPorPaciente(idPaciente));
    }

    public Optional<Cita> buscarPorId(int idCita) {
        return citaDAO.buscarPorId(idCita);
    }

    public Optional<LocalDate> ultimaVisita(int idPaciente) {
        return citaDAO.ultimaVisita(idPaciente);
    }

    /** Citas de hoy que no están canceladas (contador del menú). */
    public int citasActivasDeHoy() {
        return (int) citasDelDia(hoy()).stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA)
                .count();
    }

    // ===== Permisos =====

    /** Administrador y Recepción agendan, reprograman, confirman y cancelan. */
    public boolean puedeGestionar() {
        return Sesion.get().tieneRol(Rol.ADMIN, Rol.RECEPCION);
    }

    /** Además de quien gestiona, el odontólogo de la cita puede marcarla como atendida. */
    public boolean puedeMarcarAtendida(Cita cita) {
        if (puedeGestionar()) {
            return true;
        }
        return Sesion.get().tieneRol(Rol.ODONTOLOGO) && Sesion.get().getProfesional()
                .map(p -> p.getIdProfesional() == cita.getProfesional().getIdProfesional())
                .orElse(false);
    }

    // ===== Privados =====

    /**
     * RN-08: ni en una fecha pasada ni, si es hoy, en una hora que ya pasó.
     * Devuelve el problema listo para mostrar, o vacío si la fecha y hora sirven.
     */
    public Optional<String> problemaDeFechaHora(LocalDate fecha, LocalTime inicio) {
        if (fecha.isBefore(hoy())) {
            return Optional.of("No se puede agendar una cita en una fecha pasada.");
        }
        LocalTime ahora = LocalTime.now(reloj);
        if (fecha.equals(hoy()) && inicio.isBefore(ahora)) {
            return Optional.of("Esa hora ya pasó: elige un horario desde las " + Formatos.hora(ahora) + ".");
        }
        return Optional.empty();
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /** Jakarta + RN-05 (horario) + RN-08 (fecha) + RN-06 (cruce). */
    private void validar(Cita cita) {
        Validador.validar(cita);
        validarHorario(cita.getHoraInicio(), cita.getHoraFin());
        problemaDeFechaHora(cita.getFecha(), cita.getHoraInicio()).ifPresent(problema -> {
            throw new ReglaNegocioException(cita.getFecha().isBefore(hoy()) ? "fecha" : "horaInicio", problema);
        });
        buscarCruce(cita).ifPresent(conflicto -> {
            throw new ReglaNegocioException("horaInicio", mensajeCruce(cita.getProfesional(), conflicto));
        });
    }

    /** RN-05: bloques de 30 minutos entre 08:00 y 20:00, con el fin después del inicio. */
    static void validarHorario(LocalTime inicio, LocalTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new ReglaNegocioException("horaFin", "La hora de fin debe ser posterior a la de inicio.");
        }
        if (inicio.isBefore(APERTURA) || fin.isAfter(CIERRE)) {
            throw new ReglaNegocioException("horaInicio", "El horario de atención es de 08:00 a 20:00.");
        }
        if (!esInicioDeBloque(inicio) || !esInicioDeBloque(fin)) {
            throw new ReglaNegocioException("horaInicio", "Las citas van en bloques de 30 minutos (08:00, 08:30...).");
        }
    }

    private static boolean esInicioDeBloque(LocalTime hora) {
        return hora.getSecond() == 0 && hora.getNano() == 0 && hora.getMinute() % MINUTOS_BLOQUE == 0;
    }

    private List<Cita> filtrarPorRol(List<Cita> citas) {
        if (puedeGestionar()) {
            return citas;
        }
        // El odontólogo solo ve sus citas; si su usuario no tiene profesional asociado, no ve ninguna
        int idProfesional = Sesion.get().getProfesional().map(Profesional::getIdProfesional).orElse(-1);
        return citas.stream().filter(c -> c.getProfesional().getIdProfesional() == idProfesional).toList();
    }

    private void verificarGestion() {
        if (!puedeGestionar()) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
    }

    private Cita buscar(int idCita) {
        return citaDAO.buscarPorId(idCita).orElseThrow(() -> new ReglaNegocioException("La cita no existe."));
    }

    /** La cita, solo si todavía está pendiente o confirmada. */
    private Cita buscarActiva(int idCita) {
        Cita cita = buscar(idCita);
        if (cita.getEstado() == EstadoCita.CANCELADA || cita.getEstado() == EstadoCita.ATENDIDA) {
            throw new ReglaNegocioException("La cita ya está " + cita.getEstado().getEtiqueta().toLowerCase(Formatos.ES_PE)
                    + " y no se puede modificar.");
        }
        return cita;
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.strip();
        return limpio.isEmpty() ? null : limpio;
    }
}
