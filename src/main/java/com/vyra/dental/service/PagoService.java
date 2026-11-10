package com.vyra.dental.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

import com.vyra.dental.dao.PagoDAO;
import com.vyra.dental.dao.TratamientoDAO;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.PagoResumen;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.model.TratamientoResumen;
import com.vyra.dental.util.Formatos;
import com.vyra.dental.util.Validador;

/**
 * Pagos de los tratamientos (RF-10, CU-10, RN-12). El saldo no se guarda: es
 * el costo del tratamiento menos la suma de sus pagos. Administrador y
 * Recepción registran pagos; el odontólogo no ve esta parte.
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

    private final PagoDAO pagoDAO;
    private final TratamientoDAO tratamientoDAO;
    /** Fecha "actual"; las pruebas usan un reloj fijo. */
    private final Clock reloj;

    public PagoService(PagoDAO pagoDAO, TratamientoDAO tratamientoDAO) {
        this(pagoDAO, tratamientoDAO, Clock.systemDefaultZone());
    }

    public PagoService(PagoDAO pagoDAO, TratamientoDAO tratamientoDAO, Clock reloj) {
        this.pagoDAO = pagoDAO;
        this.tratamientoDAO = tratamientoDAO;
        this.reloj = reloj;
    }

    /**
     * RN-12: monto mayor a 0 y con hasta 2 decimales, fecha no futura (hoy si
     * no se indica) y sin superar el saldo pendiente del tratamiento.
     */
    public Pago registrar(Pago pago) {
        verificarPermiso();
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
        double saldo = saldo(pago.getIdTratamiento());
        if (centimos(saldo) <= 0) {
            throw new ReglaNegocioException("idTratamiento", "Este tratamiento ya está pagado por completo.");
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

    /** Pagos del tratamiento, del más reciente al más antiguo. */
    public List<Pago> pagosDe(int idTratamiento) {
        return pagoDAO.listarPorTratamiento(idTratamiento);
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
