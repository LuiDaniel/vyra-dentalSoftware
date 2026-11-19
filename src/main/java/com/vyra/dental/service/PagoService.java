package com.vyra.dental.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vyra.dental.dao.CitaDAO;
import com.vyra.dental.dao.PagoDAO;
import com.vyra.dental.dao.TratamientoDAO;
import com.vyra.dental.model.Cita;
import com.vyra.dental.model.Cobro;
import com.vyra.dental.model.EstadoCita;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.PagoResumen;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.model.TratamientoResumen;
import com.vyra.dental.util.Busqueda;
import com.vyra.dental.util.Formatos;
import com.vyra.dental.util.Validador;

/**
 * Pagos de los tratamientos y de las consultas (RF-10, CU-10, RN-12, RN-16).
 * El saldo no se guarda: es el costo (del tratamiento o de la consulta) menos
 * la suma de sus pagos. Administrador y Recepción registran pagos; el
 * odontólogo no ve esta parte.
 */
public class PagoService {

    /** Periodos del selector de la pantalla Pagos. */
    public enum Periodo {
        HOY, SEMANA, MES
    }

    /** Fechas de un periodo, ambas incluidas. */
    public record Rango(LocalDate desde, LocalDate hasta) {
    }

    /** Lo que falta cobrar y en cuántos tratamientos. */
    public record SaldoPendiente(double total, int tratamientos) {
    }

    /** Además de las de hoy, se ofrecen para cobrar las consultas de estos días antes y después. */
    public static final int DIAS_CONSULTAS = 30;

    private final PagoDAO pagoDAO;
    private final TratamientoDAO tratamientoDAO;
    /** Para cobrar consultas (RN-16); null = solo tratamientos (pruebas antiguas). */
    private final CitaDAO citaDAO;
    /** Fecha "actual"; las pruebas usan un reloj fijo. */
    private final Clock reloj;

    public PagoService(PagoDAO pagoDAO, TratamientoDAO tratamientoDAO) {
        this(pagoDAO, tratamientoDAO, null, Clock.systemDefaultZone());
    }

    public PagoService(PagoDAO pagoDAO, TratamientoDAO tratamientoDAO, Clock reloj) {
        this(pagoDAO, tratamientoDAO, null, reloj);
    }

    public PagoService(PagoDAO pagoDAO, TratamientoDAO tratamientoDAO, CitaDAO citaDAO) {
        this(pagoDAO, tratamientoDAO, citaDAO, Clock.systemDefaultZone());
    }

    public PagoService(PagoDAO pagoDAO, TratamientoDAO tratamientoDAO, CitaDAO citaDAO, Clock reloj) {
        this.pagoDAO = pagoDAO;
        this.tratamientoDAO = tratamientoDAO;
        this.citaDAO = citaDAO;
        this.reloj = reloj;
    }

    /**
     * RN-12: monto mayor a 0 y con hasta 2 decimales, fecha no futura (hoy si
     * no se indica) y sin superar el saldo pendiente del tratamiento o de la
     * consulta (RN-16). Un pago por Mercado Pago necesita su número de operación
     * y no se registra dos veces (RF-13).
     */
    public Pago registrar(Pago pago) {
        verificarPermiso();
        if ((pago.getIdTratamiento() == null) == (pago.getIdCita() == null)) {
            throw new ReglaNegocioException("idTratamiento", "Elige la consulta o el tratamiento que se paga.");
        }
        pago.setNumeroOperacion(limpiar(pago.getNumeroOperacion()));
        if (pago.getFechaPago() == null) {
            pago.setFechaPago(hoy());
        }
        if (pago.getFechaPago().isAfter(hoy())) {
            throw new ReglaNegocioException("fechaPago", "La fecha de pago no puede ser futura.");
        }
        if (pago.getMonto() <= 0) {
            throw new ReglaNegocioException("monto", "El monto debe ser mayor a 0.");
        }
        if (BigDecimal.valueOf(pago.getMonto()).stripTrailingZeros().scale() > 2) {
            throw new ReglaNegocioException("monto", "El monto puede tener hasta 2 decimales.");
        }
        if (pago.getMetodoPago() == null) {
            throw new ReglaNegocioException("metodoPago", "Elige el método de pago.");
        }
        if (pago.getMetodoPago() == MetodoPago.MERCADO_PAGO) {
            if (pago.getNumeroOperacion() == null) {
                throw new ReglaNegocioException("metodoPago",
                        "Los pagos con Mercado Pago se registran solos cuando el cobro se aprueba.");
            }
            if (pagoDAO.existeOperacion(MetodoPago.MERCADO_PAGO, pago.getNumeroOperacion())) {
                throw new ReglaNegocioException("Ese pago de Mercado Pago ya está registrado.");
            }
        }
        double saldo;
        if (pago.esDeConsulta()) {
            saldo = saldoConsulta(pago.getIdCita());
            if (centimos(saldo) <= 0) {
                throw new ReglaNegocioException("idCita", "Esta consulta ya está pagada.");
            }
        } else {
            saldo = saldo(pago.getIdTratamiento());
            if (centimos(saldo) <= 0) {
                throw new ReglaNegocioException("idTratamiento", "Este tratamiento ya está pagado por completo.");
            }
        }
        if (centimos(pago.getMonto()) > centimos(saldo)) {
            throw new ReglaNegocioException("monto",
                    "El monto supera el saldo pendiente (" + Formatos.dinero(saldo) + ").");
        }
        Validador.validar(pago);
        return pagoDAO.crear(pago);
    }

    /** Costo − suma de pagos, redondeado a céntimos (docs/03-modelo-base-datos.md §5.3). */
    public double saldo(int idTratamiento) {
        Tratamiento tratamiento = tratamientoDAO.buscarPorId(idTratamiento)
                .orElseThrow(() -> new ReglaNegocioException("idTratamiento", "Elige el tratamiento que se paga."));
        return (centimos(tratamiento.getCosto()) - centimos(pagoDAO.totalPagado(idTratamiento))) / 100.0;
    }

    /** Costo de la consulta − lo pagado (RN-16). La cita no puede estar cancelada. */
    public double saldoConsulta(int idCita) {
        Cita cita = buscarCita(idCita);
        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new ReglaNegocioException("idCita", "La cita está cancelada: su consulta ya no se cobra.");
        }
        return (centimos(cita.getCostoConsulta()) - centimos(pagoDAO.totalPagadoConsulta(idCita))) / 100.0;
    }

    /** Pagos del tratamiento, del más reciente al más antiguo. */
    public List<Pago> pagosDe(int idTratamiento) {
        return pagoDAO.listarPorTratamiento(idTratamiento);
    }

    /** Pagos de la consulta de la cita, del más reciente al más antiguo. */
    public List<Pago> pagosDeConsulta(int idCita) {
        return pagoDAO.listarPorCita(idCita);
    }

    /**
     * Lo que se puede cobrar a los pacientes que coinciden con el texto
     * (nombre o DNI; vacío = todos): primero las consultas sin pagar de las
     * citas no canceladas de {@value #DIAS_CONSULTAS} días atrás a
     * {@value #DIAS_CONSULTAS} días adelante (las más cercanas a hoy primero),
     * después los tratamientos con saldo.
     */
    public List<Cobro> porCobrar(String textoPaciente) {
        List<Cobro> cobros = new ArrayList<>(consultasPorCobrar(textoPaciente));
        tratamientosConSaldo(textoPaciente).stream().map(Cobro::deTratamiento).forEach(cobros::add);
        return cobros;
    }

    /** Consultas pendientes de pago (RN-16) de los pacientes que coinciden con el texto. */
    public List<Cobro> consultasPorCobrar(String textoPaciente) {
        if (citaDAO == null) {
            return List.of();
        }
        LocalDate hoy = hoy();
        String texto = Busqueda.normalizar(textoPaciente);
        return citaDAO.listarEntre(hoy.minusDays(DIAS_CONSULTAS), hoy.plusDays(DIAS_CONSULTAS)).stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA && c.getCostoConsulta() > 0)
                .filter(c -> texto.isEmpty() || c.getPaciente().getDni().contains(texto)
                        || Busqueda.normalizar(c.getPaciente().getNombreCompleto()).contains(texto))
                .sorted(Comparator.comparingLong((Cita c) -> Math.abs(c.getFecha().toEpochDay() - hoy.toEpochDay()))
                        .thenComparing(Cita::getHoraInicio))
                .map(c -> Cobro.deConsulta(c, pagoDAO.totalPagadoConsulta(c.getIdCita())))
                .filter(cobro -> centimos(cobro.getSaldo()) > 0)
                .toList();
    }

    /** La consulta de la cita como algo por cobrar (aunque ya esté pagada). */
    public Optional<Cobro> cobroDeConsulta(int idCita) {
        if (citaDAO == null) {
            return Optional.empty();
        }
        return citaDAO.buscarPorId(idCita).map(c -> Cobro.deConsulta(c, pagoDAO.totalPagadoConsulta(idCita)));
    }

    /** El tratamiento como algo por cobrar (aunque ya esté pagado). */
    public Optional<Cobro> cobroDeTratamiento(int idTratamiento) {
        return tratamientoDAO.buscarResumen(idTratamiento).map(Cobro::deTratamiento);
    }

    /** El mismo cobro, releído (después de un pago). */
    public Optional<Cobro> releer(Cobro cobro) {
        return cobro.esConsulta() ? cobroDeConsulta(cobro.getId()) : cobroDeTratamiento(cobro.getId());
    }

    /** RN-18: lo que el paciente debe de sus tratamientos (aviso al agendar). */
    public SaldoPendiente deudaDePaciente(int idPaciente) {
        List<TratamientoResumen> conSaldo = tratamientoDAO.resumenesDePaciente(idPaciente).stream()
                .filter(r -> centimos(r.getSaldo()) > 0)
                .toList();
        long centimos = conSaldo.stream().mapToLong(r -> centimos(r.getSaldo())).sum();
        return new SaldoPendiente(centimos / 100.0, conSaldo.size());
    }

    /** Pagos del rango con concepto y paciente, del más reciente al más antiguo. */
    public List<PagoResumen> pagosEntre(Rango rango) {
        return pagoDAO.resumenesEntre(rango.desde(), rango.hasta());
    }

    public Map<MetodoPago, Double> totalesPorMetodo(Rango rango) {
        return pagoDAO.totalesPorMetodo(rango.desde(), rango.hasta());
    }

    public double totalCobrado(Rango rango) {
        long centimos = totalesPorMetodo(rango).values().stream().mapToLong(PagoService::centimos).sum();
        return centimos / 100.0;
    }

    /** Suma de los saldos de todos los tratamientos que aún deben algo (en curso o finalizados). */
    public SaldoPendiente saldoPendiente() {
        List<TratamientoResumen> conSaldo = tratamientosConSaldo("");
        long centimos = conSaldo.stream().mapToLong(r -> centimos(r.getSaldo())).sum();
        return new SaldoPendiente(centimos / 100.0, conSaldo.size());
    }

    /** Tratamientos que aún deben algo, para elegir a cuál se le registra un pago. */
    public List<TratamientoResumen> tratamientosConSaldo(String textoPaciente) {
        return tratamientoDAO.buscarResumenes(textoPaciente, null).stream()
                .filter(r -> centimos(r.getSaldo()) > 0)
                .toList();
    }

    /** Hoy; la semana de lunes a domingo; el mes calendario. */
    public Rango rango(Periodo periodo) {
        return rango(periodo, hoy());
    }

    /** El periodo anterior, para comparar lo cobrado: ayer, la semana pasada o el mes pasado. */
    public Rango rangoAnterior(Periodo periodo) {
        LocalDate hoy = hoy();
        return rango(periodo, switch (periodo) {
            case HOY -> hoy.minusDays(1);
            case SEMANA -> hoy.minusWeeks(1);
            case MES -> hoy.minusMonths(1);
        });
    }

    public boolean puedeRegistrar() {
        return Sesion.get().tieneRol(Rol.ADMIN, Rol.RECEPCION);
    }

    private static Rango rango(Periodo periodo, LocalDate dia) {
        return switch (periodo) {
            case HOY -> new Rango(dia, dia);
            case SEMANA -> new Rango(dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    dia.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            case MES -> new Rango(dia.withDayOfMonth(1), dia.with(TemporalAdjusters.lastDayOfMonth()));
        };
    }

    /** Los montos se comparan en céntimos para que 0.1 + 0.2 no dé 0.30000000000000004. */
    private static long centimos(double monto) {
        return Math.round(monto * 100);
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private Cita buscarCita(int idCita) {
        if (citaDAO == null) {
            throw new IllegalStateException("PagoService se creó sin CitaDAO: no puede cobrar consultas");
        }
        return citaDAO.buscarPorId(idCita)
                .orElseThrow(() -> new ReglaNegocioException("idCita", "La cita elegida no existe."));
    }

    private void verificarPermiso() {
        if (!puedeRegistrar()) {
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
