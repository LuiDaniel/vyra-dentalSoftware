package com.vyra.dental.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.vyra.dental.dao.CitaDAO;
import com.vyra.dental.dao.PagoDAO;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.Rol;

/**
 * Reportes (RF-09, CU-09): citas, historial de tratamientos de un paciente e ingresos por
 * periodo. Solo el administrador (RN-15). Los montos se suman en céntimos enteros.
 */
public class ReporteService {

    /** Totales de un reporte de citas. {@code asistencia} = atendidas / (total − canceladas), en 0..100. */
    public record ResumenCitas(int total, int atendidas, int canceladas, int pendientes, int asistencia) {
    }

    /** Ingresos de un periodo: total, por método de pago y por día (solo los días con pagos). */
    public record Ingresos(double total, Map<MetodoPago, Double> porMetodo, Map<LocalDate, Double> porDia,
                           List<PagoDetalle> pagos) {
    }

    private final CitaDAO citaDAO;
    private final PagoDAO pagoDAO;
    private final TratamientoService tratamientoService;
    private final PagoService pagoService;

    public ReporteService(CitaDAO citaDAO, PagoDAO pagoDAO, TratamientoService tratamientoService,
                          PagoService pagoService) {
        this.citaDAO = citaDAO;
        this.pagoDAO = pagoDAO;
        this.tratamientoService = tratamientoService;
        this.pagoService = pagoService;
    }

    /**
     * Citas entre dos fechas (incluidas), por fecha y hora. {@code idProfesional} null = todos
     * los profesionales.
     */
    public List<Cita> citas(LocalDate desde, LocalDate hasta, Integer idProfesional) {
        verificarAcceso();
        validarRango(desde, hasta);
        return citaDAO.listarEntre(desde, hasta).stream()
                .filter(c -> idProfesional == null || c.getProfesional().getIdProfesional() == idProfesional)
                .sorted(Comparator.comparing(Cita::getFecha).thenComparing(Cita::getHoraInicio))
                .toList();
    }

    public ResumenCitas resumir(List<Cita> citas) {
        int atendidas = contar(citas, EstadoCita.ATENDIDA);
        int canceladas = contar(citas, EstadoCita.CANCELADA);
        int pendientes = contar(citas, EstadoCita.PENDIENTE) + contar(citas, EstadoCita.CONFIRMADA);
        int validas = citas.size() - canceladas;
        int asistencia = validas == 0 ? 0 : (int) Math.round(atendidas * 100.0 / validas);
        return new ResumenCitas(citas.size(), atendidas, canceladas, pendientes, asistencia);
    }

    /** Tratamientos del paciente con su costo, lo pagado y el saldo. */
    public List<TratamientoResumen> historialPaciente(int idPaciente) {
        verificarAcceso();
        return tratamientoService.listarPorPaciente(idPaciente);
    }

    public Ingresos ingresos(LocalDate desde, LocalDate hasta) {
        verificarAcceso();
        validarRango(desde, hasta);
        Map<MetodoPago, Long> metodos = new EnumMap<>(MetodoPago.class);
        for (MetodoPago metodo : MetodoPago.values()) {
            metodos.put(metodo, 0L);
        }
        Map<LocalDate, Long> dias = new TreeMap<>();
        long total = 0;
        for (Pago pago : pagoDAO.listarPorPeriodo(desde, hasta)) {
            long centimos = PagoService.aCentimos(pago.getMonto());
            total += centimos;
            metodos.merge(pago.getMetodoPago(), centimos, Long::sum);
            dias.merge(pago.getFechaPago(), centimos, Long::sum);
        }
        Map<MetodoPago, Double> porMetodo = new EnumMap<>(MetodoPago.class);
        metodos.forEach((metodo, valor) -> porMetodo.put(metodo, valor / 100.0));
        Map<LocalDate, Double> porDia = new TreeMap<>();
        dias.forEach((dia, valor) -> porDia.put(dia, valor / 100.0));
        return new Ingresos(total / 100.0, porMetodo, porDia, pagoService.pagosEntre(desde, hasta));
    }

    public boolean puedeAcceder() {
        return Sesion.get().tieneRol(Rol.ADMIN);
    }

    private static int contar(List<Cita> citas, EstadoCita estado) {
        return (int) citas.stream().filter(c -> c.getEstado() == estado).count();
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ReglaNegocioException("Elige las fechas del reporte.");
        }
        if (hasta.isBefore(desde)) {
            throw new ReglaNegocioException("La fecha final no puede ser anterior a la inicial.");
        }
    }

    private void verificarAcceso() {
        if (!puedeAcceder()) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
    }
}
