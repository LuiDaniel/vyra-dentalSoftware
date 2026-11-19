package com.vyra.dental.pasarela;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pasarela de mentira: no se conecta a nada ni mueve dinero. El enlace del QR
 * es solo un texto; el pago se "aprueba" cuando alguien pulsa "Simular pago
 * aprobado" ({@link #simularPago}). Sirve para probar el flujo y para la
 * exposición sin internet. Vyra la usa si no hay credenciales de Mercado Pago.
 */
public class PasarelaDemo implements PasarelaPago {

    private final Map<String, Double> ordenes = new ConcurrentHashMap<>();
    private final Map<String, ResultadoCobro> pagados = new ConcurrentHashMap<>();
    private final AtomicLong operaciones = new AtomicLong(System.currentTimeMillis() % 1_000_000);

    @Override
    public String nombre() {
        return "Mercado Pago (demo)";
    }

    @Override
    public boolean esDemo() {
        return true;
    }

    @Override
    public OrdenCobro crearOrden(String referencia, String concepto, double monto) {
        ordenes.put(referencia, monto);
        return new OrdenCobro(referencia, "VYRA-DEMO|" + referencia + "|S/ " + monto + "|" + concepto, monto);
    }

    @Override
    public ResultadoCobro consultar(String referencia) {
        return pagados.getOrDefault(referencia, ResultadoCobro.pendiente());
    }

    /** Como si el paciente hubiera pagado el monto completo del cobro. */
    public void simularPago(String referencia) {
        Double monto = ordenes.get(referencia);
        if (monto == null) {
            throw new IllegalArgumentException("No hay un cobro con la referencia " + referencia);
        }
        pagados.putIfAbsent(referencia, new ResultadoCobro(Estado.APROBADO,
                "DEMO-" + operaciones.incrementAndGet(), monto, "Pago simulado"));
    }
}
