package com.vyra.dental.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.vyra.dental.dao.CitaDAO;
import com.vyra.dental.dao.PagoDAO;
import com.vyra.dental.dao.TratamientoDAO;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Pago;
import com.vyra.dental.model.Rol;
import com.vyra.dental.model.Tratamiento;
import com.vyra.dental.util.Formatos;
import com.vyra.dental.util.Validador;

/**
 * Pagos (RF-10, CU-10, RN-12). Administrador y recepción cobran; el odontólogo no tiene acceso.
 * El saldo no se guarda: es costo − suma de pagos. Las comparaciones de dinero se hacen en
 * céntimos enteros para evitar errores de redondeo de {@code double}.
 */
public class PagoService {

    /** Saldo pendiente de todos los tratamientos y cuántos tratamientos lo tienen. */
    public record SaldoPendiente(double total, int tratamientos) {
    }

    private final PagoDAO pagoDAO;
    private final TratamientoDAO tratamientoDAO;
    private final CitaDAO citaDAO;
    /** Fecha "actual"; las pruebas usan un reloj fijo. */
    private final Clock reloj;

    public PagoService(PagoDAO pagoDAO, TratamientoDAO tratamientoDAO, CitaDAO citaDAO) {
        this(pagoDAO, tratamientoDAO, citaDAO, Clock.systemDefaultZone());
    }

    public PagoService(PagoDAO pagoDAO, TratamientoDAO tratamientoDAO, CitaDAO citaDAO, Clock reloj) {
        this.pagoDAO = pagoDAO;
        this.tratamientoDAO = tratamientoDAO;
        this.citaDAO = citaDAO;
        this.reloj = reloj;
    }

    /** Soles → céntimos enteros. */
    static long aCentimos(double soles) {
        return Math.max(0, Math.round(soles * 100));
    }

    /**
     * Registra un pago. El monto debe ser mayor a 0 y no superar el saldo pendiente del
     * tratamiento; la fecha no puede ser futura (RN-12).
     */
    public Pago registrar(Pago pago) {
        verificarAcceso();
        pago.setNumeroOperacion(limpiar(pago.getNumeroOperacion()));
        if (pago.getFechaPago() != null && pago.getFechaPago().isAfter(LocalDate.now(reloj))) {
            throw new ReglaNegocioException("fechaPago", "La fecha de pago no puede ser futura.");
        }
        Validador.validar(pago);
        Tratamiento tratamiento = tratamientoDAO.buscarPorId(pago.getIdTratamiento())
                .orElseThrow(() -> new ReglaNegocioException("tratamiento", "Elige el tratamiento que se paga."));
        double saldo = saldo(tratamiento);
        if (aCentimos(saldo) == 0) {
            throw new ReglaNegocioException("monto", "Este tratamiento ya está pagado por completo.");
        }
        if (aCentimos(pago.getMonto()) > aCentimos(saldo)) {
            throw new ReglaNegocioException("monto", "El monto supera el saldo pendiente (" + Formatos.dinero(saldo) + ").");
        }
        return pagoDAO.crear(pago);
    }

    /** Costo − suma de pagos del tratamiento (nunca negativo). */
    public double saldo(int idTratamiento) {
        verificarAcceso();
        Tratamiento tratamiento = tratamientoDAO.buscarPorId(idTratamiento)
                .orElseThrow(() -> new ReglaNegocioException("El tratamiento ya no existe."));
        return saldo(tratamiento);
    }

    public double pagado(int idTratamiento) {
        verificarAcceso();
        return pagoDAO.totalPagado(idTratamiento);
    }

    /** Pagos de un tratamiento, del más antiguo al más reciente. */
    public List<Pago> pagosDe(int idTratamiento) {
        verificarAcceso();
        return pagoDAO.listarPorTratamiento(idTratamiento);
    }

    /** Pagos con fecha entre {@code desde} y {@code hasta} (incluidas), del más reciente al más antiguo. */
    public List<PagoDetalle> pagosEntre(LocalDate desde, LocalDate hasta) {
        verificarAcceso();
        List<PagoDetalle> detalles = new ArrayList<>();
        for (Pago pago : pagoDAO.listarPorPeriodo(desde, hasta)) {
            detalles.add(detallar(pago));
        }
        return detalles;
    }

    /** Pagos de todos los tratamientos del paciente, del más reciente al más antiguo. */
    public List<PagoDetalle> pagosDelPaciente(int idPaciente) {
        verificarAcceso();
        List<PagoDetalle> detalles = new ArrayList<>();
        for (Pago pago : pagoDAO.listarTodos()) {
            PagoDetalle detalle = detallar(pago);
            if (detalle.paciente() != null && detalle.paciente().getIdPaciente() == idPaciente) {
                detalles.add(detalle);
            }
        }
        return detalles;
    }

    public double cobradoEntre(LocalDate desde, LocalDate hasta) {
        verificarAcceso();
        long centimos = 0;
        for (Pago pago : pagoDAO.listarPorPeriodo(desde, hasta)) {
            centimos += aCentimos(pago.getMonto());
        }
        return centimos / 100.0;
    }

    /** Lo cobrado en el periodo según el método de pago; todos los métodos aparecen, aunque sea con 0. */
    public Map<MetodoPago, Double> cobradoPorMetodo(LocalDate desde, LocalDate hasta) {
        verificarAcceso();
        Map<MetodoPago, Long> centimos = new EnumMap<>(MetodoPago.class);
        for (MetodoPago metodo : MetodoPago.values()) {
            centimos.put(metodo, 0L);
        }
        for (Pago pago : pagoDAO.listarPorPeriodo(desde, hasta)) {
            centimos.merge(pago.getMetodoPago(), aCentimos(pago.getMonto()), Long::sum);
        }
        Map<MetodoPago, Double> soles = new EnumMap<>(MetodoPago.class);
        centimos.forEach((metodo, valor) -> soles.put(metodo, valor / 100.0));
        return soles;
    }

    public SaldoPendiente saldoPendiente() {
        verificarAcceso();
        long centimos = 0;
        int cantidad = 0;
        for (Tratamiento tratamiento : tratamientoDAO.listarTodos()) {
            long saldo = aCentimos(saldo(tratamiento));
            if (saldo > 0) {
                centimos += saldo;
                cantidad++;
            }
        }
        return new SaldoPendiente(centimos / 100.0, cantidad);
    }

    public boolean puedeAcceder() {
        return Sesion.get().tieneRol(Rol.ADMIN, Rol.RECEPCION);
    }

    private double saldo(Tratamiento tratamiento) {
        return aCentimos(tratamiento.getCosto() - pagoDAO.totalPagado(tratamiento.getIdTratamiento())) / 100.0;
    }

    private PagoDetalle detallar(Pago pago) {
        Tratamiento tratamiento = tratamientoDAO.buscarPorId(pago.getIdTratamiento()).orElse(null);
        var paciente = tratamiento == null ? null
                : citaDAO.buscarPorId(tratamiento.getIdCita()).map(c -> c.getPaciente()).orElse(null);
        return new PagoDetalle(pago, tratamiento, paciente);
    }

    private void verificarAcceso() {
        if (!puedeAcceder()) {
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
