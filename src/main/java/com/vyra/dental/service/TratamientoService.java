package com.vyra.dental.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import com.vyra.dental.dao.CitaDAO;
import com.vyra.dental.dao.TratamientoDAO;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.Profesional;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.model.TratamientoResumen;
import com.vyra.dental.util.Validador;

/**
 * Tratamientos realizados en una cita, con sus piezas dentales y su costo
 * (RF-06, CU-06, RN-10, RN-11). Solo el odontólogo registra y finaliza;
 * Administrador y Recepción los consultan (la recepción, para cobrar).
 */
public class TratamientoService {

    /** Además de las de hoy, se puede registrar un tratamiento en una cita atendida en estos últimos días. */
    public static final int DIAS_CITAS_RECIENTES = 30;

    private final TratamientoDAO tratamientoDAO;
    private final CitaDAO citaDAO;
    /** Fecha "actual"; las pruebas usan un reloj fijo. */
    private final Clock reloj;

    public TratamientoService(TratamientoDAO tratamientoDAO, CitaDAO citaDAO) {
        this(tratamientoDAO, citaDAO, Clock.systemDefaultZone());
    }

    public TratamientoService(TratamientoDAO tratamientoDAO, CitaDAO citaDAO, Clock reloj) {
        this.tratamientoDAO = tratamientoDAO;
        this.citaDAO = citaDAO;
        this.reloj = reloj;
    }

    /**
     * Registra el tratamiento en su cita, con la fecha de la cita. La cita no
     * puede estar cancelada ni ser futura, y si el odontólogo tiene un
     * profesional asociado, debe ser suya. Las piezas se guardan sin repetir y
     * en orden.
     */
    public Tratamiento registrar(Tratamiento tratamiento) {
        verificarPermiso();
        tratamiento.setDescripcion(limpiar(tratamiento.getDescripcion()));
        tratamiento.setPiezasDentales(normalizarPiezas(tratamiento.getPiezasDentales()));
        if (tratamiento.getEstado() == null) {
            tratamiento.setEstado(EstadoTratamiento.EN_CURSO);
        }
        Cita cita = verificarCita(tratamiento.getIdCita());
        tratamiento.setFecha(cita.getFecha());
        Validador.validar(tratamiento);
        return tratamientoDAO.crear(tratamiento);
    }

    /** CU-06, paso 4: el tratamiento en curso pasa a finalizado. */
    public void finalizar(int idTratamiento) {
        verificarPermiso();
        Tratamiento tratamiento = tratamientoDAO.buscarPorId(idTratamiento)
                .orElseThrow(() -> new ReglaNegocioException("El tratamiento no existe."));
        if (tratamiento.getEstado() == EstadoTratamiento.FINALIZADO) {
            throw new ReglaNegocioException("El tratamiento ya está finalizado.");
        }
        tratamientoDAO.actualizarEstado(idTratamiento, EstadoTratamiento.FINALIZADO);
    }

    // ===== Consultas =====

    /** Con paciente y lo pagado; texto vacío y estado null = todos. */
    public List<TratamientoResumen> buscar(String textoPaciente, EstadoTratamiento estado) {
        return tratamientoDAO.buscarResumenes(textoPaciente, estado);
    }

    public Optional<TratamientoResumen> resumen(int idTratamiento) {
        return tratamientoDAO.buscarResumen(idTratamiento);
    }

    public List<TratamientoResumen> dePaciente(int idPaciente) {
        return tratamientoDAO.resumenesDePaciente(idPaciente);
    }

    /**
     * Citas en las que se puede registrar un tratamiento: las de hoy que no
     * están canceladas y las atendidas en los últimos {@value #DIAS_CITAS_RECIENTES}
     * días, de la más reciente a la más antigua. El odontólogo solo ve las suyas.
     */
    public List<Cita> citasParaRegistrar() {
        LocalDate hoy = hoy();
        Integer idProfesional = idProfesionalDeLaSesion();
        return citaDAO.listarEntre(hoy.minusDays(DIAS_CITAS_RECIENTES), hoy).stream()
                .filter(c -> c.getEstado() == EstadoCita.ATENDIDA
                        || c.getFecha().equals(hoy) && c.getEstado() != EstadoCita.CANCELADA)
                .filter(c -> idProfesional == null || c.getProfesional().getIdProfesional() == idProfesional)
                .sorted(Comparator.comparing(Cita::getFecha).thenComparing(Cita::getHoraInicio).reversed())
                .toList();
    }

    public boolean puedeEditar() {
        return Sesion.get().tieneRol(Rol.ODONTOLOGO);
    }

    /**
     * Lee las piezas escritas como "36, 47" (también separadas por espacios).
     * Vacío = ninguna pieza. Lanza un error del campo "piezasDentales" si algo
     * no es un número o no existe en la notación FDI (RN-10).
     */
    public static List<Integer> leerPiezas(String texto) {
        List<Integer> piezas = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return piezas;
        }
        for (String parte : texto.strip().split("[,;\\s]+")) {
            try {
                piezas.add(Integer.parseInt(parte));
            } catch (NumberFormatException e) {
                throw new ReglaNegocioException("piezasDentales",
                        "Escribe los números de pieza separados por comas (ej. 36, 47).");
            }
        }
        return normalizarPiezas(piezas);
    }

    // ===== Privados =====

    /** Sin repetidas, en orden, y todas de la notación FDI (RN-10). */
    private static List<Integer> normalizarPiezas(List<Integer> piezas) {
        TreeSet<Integer> unicas = new TreeSet<>();
        if (piezas != null) {
            for (Integer pieza : piezas) {
                if (pieza == null || !OdontogramaService.esPiezaValida(pieza)) {
                    throw new ReglaNegocioException("piezasDentales", "La pieza " + pieza
                            + " no existe en la notación FDI (11–18, 21–28, 31–38, 41–48).");
                }
                unicas.add(pieza);
            }
        }
        return new ArrayList<>(unicas);
    }

    private Cita verificarCita(int idCita) {
        if (idCita <= 0) {
            throw new ReglaNegocioException("idCita", "Elige la cita en la que se realizó el tratamiento.");
        }
        Cita cita = citaDAO.buscarPorId(idCita)
                .orElseThrow(() -> new ReglaNegocioException("idCita", "La cita elegida no existe."));
        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new ReglaNegocioException("idCita", "No se puede registrar un tratamiento en una cita cancelada.");
        }
        if (cita.getFecha().isAfter(hoy())) {
            throw new ReglaNegocioException("idCita", "Esa cita todavía no ocurre: elige una de hoy o una ya atendida.");
        }
        Integer idProfesional = idProfesionalDeLaSesion();
        if (idProfesional != null && cita.getProfesional().getIdProfesional() != idProfesional) {
            throw new ReglaNegocioException("idCita", "Solo puedes registrar tratamientos en tus propias citas.");
        }
        return cita;
    }

    /** El profesional del odontólogo que inició sesión, o null si no tiene uno asociado. */
    private static Integer idProfesionalDeLaSesion() {
        return Sesion.get().getProfesional().map(Profesional::getIdProfesional).orElse(null);
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
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
