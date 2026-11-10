package com.vyra.dental.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.vyra.dental.dao.CitaDAO;
import com.vyra.dental.dao.PagoDAO;
import com.vyra.dental.dao.TratamientoDAO;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.TratamientoResumen;

/**
 * Reportes (RF-09, CU-09): citas de un periodo, historial de tratamientos de
 * un paciente e ingresos por periodo. Solo el Administrador. Los montos se
 * suman en céntimos para no arrastrar errores de redondeo de double.
 */
public class ReporteService {

    /** Rango máximo de un reporte, para que las barras por día sigan siendo legibles y la consulta rápida. */
    public static final int DIAS_MAXIMOS = 366;

    /**
     * Citas del periodo con sus totales. {@code asistencia} es el % de citas
     * atendidas entre las que ya debieron ocurrir (atendidas + pendientes o
     * confirmadas de días pasados); las canceladas y las futuras no cuentan.
     * Es null si todavía no hay citas que medir.
     */
    public record ReporteCitas(List<Cita> citas, int atendidas, int canceladas, int porAtender, Integer asistencia) {
        public int total() {
            return citas.size();
        }
    }

    /** Tratamientos del paciente con sus totales. */
    public record ReporteHistorial(List<TratamientoResumen> tratamientos, double costo, double pagado, double saldo) {
    }

    /** Ingresos del periodo: total, por método (monto y cantidad de pagos) y por día (todos los días, 0 incluido). */
    public record ReporteIngresos(double total, int pagos, Map<MetodoPago, Double> porMetodo,
                                  Map<MetodoPago, Integer> pagosPorMetodo, SortedMap<LocalDate, Double> porDia) {
    }

    private final CitaDAO citaDAO;
    private final PagoDAO pagoDAO;
    private final TratamientoDAO tratamientoDAO;
    /** Fecha "actual"; las pruebas usan un reloj fijo. */
    private final Clock reloj;

    public ReporteService(CitaDAO citaDAO, PagoDAO pagoDAO, TratamientoDAO tratamientoDAO) {
        this(citaDAO, pagoDAO, tratamientoDAO, Clock.systemDefaultZone());
    }

    public ReporteService(CitaDAO citaDAO, PagoDAO pagoDAO, TratamientoDAO tratamientoDAO, Clock reloj) {
        this.citaDAO = citaDAO;
        this.pagoDAO = pagoDAO;
        this.tratamientoDAO = tratamientoDAO;
        this.reloj = reloj;
    }

    /** Citas entre las fechas, de un profesional o de todos ({@code idProfesional} null). */
    public ReporteCitas citas(LocalDate desde, LocalDate hasta, Integer idProfesional) {
        verificarPermiso();
        verificarRango(desde, hasta);
        LocalDate hoy = LocalDate.now(reloj);
        List<Cita> citas = citaDAO.listarEntre(desde, hasta).stream()
                .filter(c -> idProfesional == null || c.getProfesional().getIdProfesional() == idProfesional)
                .toList();
        int atendidas = contar(citas, EstadoCita.ATENDIDA);
        int canceladas = contar(citas, EstadoCita.CANCELADA);
        int porAtender = citas.size() - atendidas - canceladas;
        long noAsistidas = citas.stream()
                .filter(c -> c.getFecha().isBefore(hoy)
                        && (c.getEstado() == EstadoCita.PENDIENTE || c.getEstado() == EstadoCita.CONFIRMADA))
                .count();
        long medibles = atendidas + noAsistidas;
        Integer asistencia = medibles == 0 ? null : (int) Math.round(atendidas * 100.0 / medibles);
        return new ReporteCitas(citas, atendidas, canceladas, porAtender, asistencia);
    }

    /** Tratamientos del paciente (del más reciente al más antiguo) con costo, pagado y saldo totales. */
    public ReporteHistorial historialPaciente(int idPaciente) {
        verificarPermiso();
        List<TratamientoResumen> tratamientos = tratamientoDAO.resumenesDePaciente(idPaciente);
        long costo = tratamientos.stream().mapToLong(t -> centimos(t.getTratamiento().getCosto())).sum();
        long pagado = tratamientos.stream().mapToLong(t -> centimos(t.getPagado())).sum();
        return new ReporteHistorial(tratamientos, costo / 100.0, pagado / 100.0, (costo - pagado) / 100.0);
    }

    public ReporteIngresos ingresos(LocalDate desde, LocalDate hasta) {
        verificarPermiso();
        verificarRango(desde, hasta);
        List<Pago> pagos = pagoDAO.listarPorPeriodo(desde, hasta);
        Map<MetodoPago, Long> centimosPorMetodo = new EnumMap<>(MetodoPago.class);
        Map<MetodoPago, Integer> cantidad = new EnumMap<>(MetodoPago.class);
        for (Pago pago : pagos) {
            centimosPorMetodo.merge(pago.getMetodoPago(), centimos(pago.getMonto()), Long::sum);
            cantidad.merge(pago.getMetodoPago(), 1, Integer::sum);
        }
        Map<MetodoPago, Double> porMetodo = new EnumMap<>(MetodoPago.class);
        centimosPorMetodo.forEach((metodo, total) -> porMetodo.put(metodo, total / 100.0));
        long total = centimosPorMetodo.values().stream().mapToLong(Long::longValue).sum();
        return new ReporteIngresos(total / 100.0, pagos.size(), porMetodo, cantidad, porDia(pagos, desde, hasta));
    }

    /** Ingresos de cada día del rango, 0 incluido (barras "Ingresos de la semana" de Inicio). */
    public SortedMap<LocalDate, Double> ingresosPorDia(LocalDate desde, LocalDate hasta) {
        verificarPermiso();
        verificarRango(desde, hasta);
        return porDia(pagoDAO.listarPorPeriodo(desde, hasta), desde, hasta);
    }

    public boolean puedeVer() {
        return Sesion.get().tieneRol(Rol.ADMIN);
    }

    private static SortedMap<LocalDate, Double> porDia(List<Pago> pagos, LocalDate desde, LocalDate hasta) {
        SortedMap<LocalDate, Long> centimos = new TreeMap<>();
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            centimos.put(dia, 0L);
        }
        for (Pago pago : pagos) {
            centimos.merge(pago.getFechaPago(), centimos(pago.getMonto()), Long::sum);
        }
        SortedMap<LocalDate, Double> porDia = new TreeMap<>();
        centimos.forEach((dia, total) -> porDia.put(dia, total / 100.0));
        return porDia;
    }

    private static void verificarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ReglaNegocioException("desde", "Elige las dos fechas del periodo.");
        }
        if (hasta.isBefore(desde)) {
            throw new ReglaNegocioException("hasta", "La fecha final debe ser igual o posterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) >= DIAS_MAXIMOS) {
            throw new ReglaNegocioException("hasta", "Elige un periodo de hasta un año.");
        }
    }

    private static int contar(List<Cita> citas, EstadoCita estado) {
        return (int) citas.stream().filter(c -> c.getEstado() == estado).count();
    }

    private static long centimos(double monto) {
        return Math.round(monto * 100);
    }

    private void verificarPermiso() {
        if (!puedeVer()) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
    }
}
