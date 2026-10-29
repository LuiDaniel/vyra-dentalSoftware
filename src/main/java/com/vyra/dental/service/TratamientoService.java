package com.vyra.dental.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import com.vyra.dental.dao.CitaDAO;
import com.vyra.dental.dao.PagoDAO;
import com.vyra.dental.dao.TratamientoDAO;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.EstadoTratamiento;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.util.Busqueda;
import com.vyra.dental.util.Validador;

/**
 * Tratamientos (RF-06, CU-06, RN-10, RN-11). El odontólogo registra y finaliza los de sus
 * propias citas; el administrador y recepción solo consultan (recepción para cobrar).
 */
public class TratamientoService {

    /** Cuántos días atrás se ofrecen citas para registrar un tratamiento. */
    public static final int DIAS_ATRAS = 30;

    private final TratamientoDAO tratamientoDAO;
    private final CitaDAO citaDAO;
    private final PagoDAO pagoDAO;
    /** Fecha "actual"; las pruebas usan un reloj fijo. */
    private final Clock reloj;

    public TratamientoService(TratamientoDAO tratamientoDAO, CitaDAO citaDAO, PagoDAO pagoDAO) {
        this(tratamientoDAO, citaDAO, pagoDAO, Clock.systemDefaultZone());
    }

    public TratamientoService(TratamientoDAO tratamientoDAO, CitaDAO citaDAO, PagoDAO pagoDAO, Clock reloj) {
        this.tratamientoDAO = tratamientoDAO;
        this.citaDAO = citaDAO;
        this.pagoDAO = pagoDAO;
        this.reloj = reloj;
    }

    /**
     * Registra un tratamiento "En curso" con la fecha de hoy, vinculado a una cita (que no esté
     * cancelada y sea del odontólogo que lo registra) y a sus piezas dentales.
     */
    public Tratamiento registrar(Tratamiento tratamiento) {
        verificarEscritura();
        tratamiento.setDescripcion(limpiar(tratamiento.getDescripcion()));
        tratamiento.setFecha(LocalDate.now(reloj));
        if (tratamiento.getEstado() == null) {
            tratamiento.setEstado(EstadoTratamiento.EN_CURSO);
        }
        tratamiento.setPiezasDentales(piezasSinRepetir(tratamiento.getPiezasDentales()));
        Validador.validar(tratamiento);
        for (int pieza : tratamiento.getPiezasDentales()) {
            if (pieza % 10 < 1 || pieza % 10 > 8) {
                throw new ReglaNegocioException("piezas", "La pieza " + pieza + " no existe.");
            }
        }
        Cita cita = citaDAO.buscarPorId(tratamiento.getIdCita())
                .orElseThrow(() -> new ReglaNegocioException("cita", "Elige la cita del tratamiento."));
        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new ReglaNegocioException("cita", "La cita elegida está cancelada.");
        }
        if (!esDelProfesionalActual(cita)) {
            throw new ReglaNegocioException("cita", "Solo puedes registrar tratamientos en tus propias citas.");
        }
        return tratamientoDAO.crear(tratamiento);
    }

    /** Pasa un tratamiento de "En curso" a "Finalizado". */
    public void finalizar(int idTratamiento) {
        verificarEscritura();
        Tratamiento tratamiento = tratamientoDAO.buscarPorId(idTratamiento)
                .orElseThrow(() -> new ReglaNegocioException("El tratamiento ya no existe."));
        if (tratamiento.getEstado() == EstadoTratamiento.FINALIZADO) {
            throw new ReglaNegocioException("El tratamiento ya está finalizado.");
        }
        Cita cita = citaDAO.buscarPorId(tratamiento.getIdCita()).orElse(null);
        if (cita != null && !esDelProfesionalActual(cita)) {
            throw new ReglaNegocioException("Solo puedes finalizar tratamientos de tus propias citas.");
        }
        tratamiento.setEstado(EstadoTratamiento.FINALIZADO);
        tratamientoDAO.actualizar(tratamiento);
    }

    /**
     * Los tratamientos más recientes primero. {@code estado} null = todos; {@code texto} filtra por
     * nombre o DNI del paciente (sin importar mayúsculas ni tildes).
     */
    public List<TratamientoResumen> listar(EstadoTratamiento estado, String texto) {
        verificarLectura();
        List<Tratamiento> base = estado == null ? tratamientoDAO.listarTodos() : tratamientoDAO.listarPorEstado(estado);
        List<TratamientoResumen> resultado = new ArrayList<>();
        for (Tratamiento t : base) {
            resumir(t).filter(r -> coincide(r, texto)).ifPresent(resultado::add);
        }
        return resultado;
    }

    public List<TratamientoResumen> listarPorPaciente(int idPaciente) {
        verificarLectura();
        List<TratamientoResumen> resultado = new ArrayList<>();
        for (Cita cita : citaDAO.listarPorPaciente(idPaciente)) {
            for (Tratamiento t : tratamientoDAO.listarPorCita(cita.getIdCita())) {
                resultado.add(new TratamientoResumen(t, cita, pagoDAO.totalPagado(t.getIdTratamiento())));
            }
        }
        resultado.sort((a, b) -> b.tratamiento().getFecha().compareTo(a.tratamiento().getFecha()) != 0
                ? b.tratamiento().getFecha().compareTo(a.tratamiento().getFecha())
                : Integer.compare(b.tratamiento().getIdTratamiento(), a.tratamiento().getIdTratamiento()));
        return resultado;
    }

    public Optional<TratamientoResumen> buscarPorId(int idTratamiento) {
        verificarLectura();
        return tratamientoDAO.buscarPorId(idTratamiento).flatMap(this::resumir);
    }

    /**
     * Citas donde el odontólogo puede registrar un tratamiento: las de hoy y las de los últimos
     * {@value #DIAS_ATRAS} días que no estén canceladas, de la más reciente a la más antigua.
     */
    public List<Cita> citasParaRegistrar() {
        verificarEscritura();
        LocalDate hoy = LocalDate.now(reloj);
        return citaDAO.listarEntre(hoy.minusDays(DIAS_ATRAS), hoy).stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA)
                .filter(this::esDelProfesionalActual)
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()) != 0
                        ? b.getFecha().compareTo(a.getFecha()) : b.getHoraInicio().compareTo(a.getHoraInicio()))
                .toList();
    }

    public boolean puedeVer() {
        return Sesion.get().tieneRol(Rol.ADMIN, Rol.RECEPCION, Rol.ODONTOLOGO);
    }

    public boolean puedeEscribir() {
        return Sesion.get().tieneRol(Rol.ODONTOLOGO);
    }

    private Optional<TratamientoResumen> resumir(Tratamiento t) {
        return citaDAO.buscarPorId(t.getIdCita())
                .map(cita -> new TratamientoResumen(t, cita, pagoDAO.totalPagado(t.getIdTratamiento())));
    }

    private static boolean coincide(TratamientoResumen resumen, String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        String buscado = Busqueda.normalizar(texto);
        return Busqueda.normalizar(resumen.paciente().getNombreCompleto()).contains(buscado)
                || (resumen.paciente().getDni() != null && resumen.paciente().getDni().contains(texto.strip()));
    }

    /** Si el usuario es odontólogo con profesional asociado, la cita debe ser suya. */
    private boolean esDelProfesionalActual(Cita cita) {
        return Sesion.get().getProfesional()
                .map(p -> p.getIdProfesional() == cita.getProfesional().getIdProfesional())
                .orElse(true);
    }

    private static List<Integer> piezasSinRepetir(List<Integer> piezas) {
        return piezas == null ? new ArrayList<>() : new ArrayList<>(new TreeSet<>(piezas));
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

    private static String limpiar(String texto) {
        return texto == null ? null : texto.strip();
    }
}
