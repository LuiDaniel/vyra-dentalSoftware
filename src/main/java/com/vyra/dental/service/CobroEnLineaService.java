package com.vyra.dental.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import com.vyra.dental.model.Cobro;
import com.vyra.dental.model.MetodoPago;
import com.vyra.dental.model.Pago;
import com.vyra.dental.pasarela.PasarelaDemo;
import com.vyra.dental.pasarela.PasarelaPago;
import com.vyra.dental.pasarela.PasarelaPago.OrdenCobro;
import com.vyra.dental.pasarela.PasarelaPago.ResultadoCobro;
import com.vyra.dental.util.Formatos;

/**
 * Cobro en línea con Mercado Pago (RF-13, CU-13). Recepción elige la consulta
 * o el tratamiento y el monto; Vyra crea el cobro en la pasarela y muestra un
 * QR; cuando la pasarela dice que el pago se aprobó, se registra solo como un
 * pago "Mercado Pago" con su número de operación (con las mismas reglas que un
 * pago normal: RN-12 y RN-16).
 */
public class CobroEnLineaService {

    private final PagoService pagos;
    private final PasarelaPago pasarela;
    private final Clock reloj;

    public CobroEnLineaService(PagoService pagos, PasarelaPago pasarela) {
        this(pagos, pasarela, Clock.systemDefaultZone());
    }

    public CobroEnLineaService(PagoService pagos, PasarelaPago pasarela, Clock reloj) {
        this.pagos = pagos;
        this.pasarela = pasarela;
        this.reloj = reloj;
    }

    public String nombrePasarela() {
        return pasarela.nombre();
    }

    public boolean esDemo() {
        return pasarela.esDemo();
    }

    /** Crea el cobro por el monto (sin pasar el saldo) y devuelve el enlace para el QR. */
    public OrdenCobro iniciar(Cobro cobro, double monto) {
        if (!pagos.puedeRegistrar()) {
            throw new ReglaNegocioException(PacienteService.SIN_PERMISO);
        }
        Cobro actual = pagos.releer(cobro)
                .orElseThrow(() -> new ReglaNegocioException("idTratamiento", "Ya no existe lo que se iba a cobrar."));
        if (monto <= 0) {
            throw new ReglaNegocioException("monto", "El monto debe ser mayor a 0.");
        }
        if (Math.round(actual.getSaldo() * 100) <= 0) {
            throw new ReglaNegocioException("idTratamiento", "No hay nada pendiente de cobro.");
        }
        if (Math.round(monto * 100) > Math.round(actual.getSaldo() * 100)) {
            throw new ReglaNegocioException("monto",
                    "El monto supera el saldo pendiente (" + Formatos.dinero(actual.getSaldo()) + ").");
        }
        String referencia = "VYRA-" + (cobro.esConsulta() ? "C" : "T") + cobro.getId() + "-" + reloj.millis();
        String concepto = "Vyra · " + actual.getConcepto() + " · " + actual.getPaciente().getNombreCompleto();
        return pasarela.crearOrden(referencia, concepto, monto);
    }

    /**
     * Pregunta a la pasarela. Si el pago se aprobó, lo registra y lo devuelve;
     * si todavía no, devuelve vacío; si se rechazó, lanza el motivo.
     */
    public Optional<Pago> verificar(Cobro cobro, OrdenCobro orden) {
        ResultadoCobro resultado = pasarela.consultar(orden.referencia());
        switch (resultado.estado()) {
            case PENDIENTE -> {
                return Optional.empty();
            }
            case RECHAZADO -> throw new ReglaNegocioException("Mercado Pago rechazó el pago ("
                    + resultado.detalle() + "). El paciente puede intentarlo de nuevo con el mismo QR.");
            default -> {
                // APROBADO: sigue abajo
            }
        }
        double saldo = pagos.releer(cobro).map(Cobro::getSaldo).orElse(0.0);
        // Nunca más que el saldo (RN-12), aunque la pasarela reporte más
        double monto = Math.min(resultado.monto() > 0 ? resultado.monto() : orden.monto(), saldo);
        LocalDate hoy = LocalDate.now(reloj);
        Pago pago = cobro.esConsulta()
                ? Pago.deConsulta(cobro.getId(), monto, hoy, MetodoPago.MERCADO_PAGO, resultado.idOperacion())
                : new Pago(0, cobro.getId(), monto, hoy, MetodoPago.MERCADO_PAGO, resultado.idOperacion());
        return Optional.of(pagos.registrar(pago));
    }

    /** Solo en modo demo: como si el paciente hubiera pagado. */
    public void simularPago(OrdenCobro orden) {
        if (!(pasarela instanceof PasarelaDemo demo)) {
            throw new IllegalStateException("Solo la pasarela demo simula pagos");
        }
        demo.simularPago(orden.referencia());
    }
}
