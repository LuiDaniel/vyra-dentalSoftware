package com.vyra.dental.pasarela;

/**
 * Una pasarela de pagos en línea (RF-13): crea un enlace de pago (que la
 * pantalla muestra como código QR) y después pregunta si el paciente ya pagó.
 * Hay dos: {@link MercadoPagoPasarela} (de verdad, con las credenciales del
 * consultorio) y {@link PasarelaDemo} (sin internet ni cuenta, para probar y
 * exponer). {@link ConfigPasarela#crear()} elige cuál usar.
 */
public interface PasarelaPago {

    /** "Mercado Pago" o "Mercado Pago (demo)". */
    String nombre();

    /** true si no cobra de verdad. */
    boolean esDemo();

    /**
     * Crea el cobro. {@code referencia} es única y es la que después se usa
     * para preguntar por el pago; {@code concepto} es lo que ve el paciente.
     */
    OrdenCobro crearOrden(String referencia, String concepto, double monto);

    /** Estado del cobro con esa referencia. */
    ResultadoCobro consultar(String referencia);

    /** El cobro creado: su referencia y el enlace donde el paciente paga. */
    record OrdenCobro(String referencia, String enlace, double monto) {
    }

    /** Lo que respondió la pasarela al preguntar por un cobro. */
    record ResultadoCobro(Estado estado, String idOperacion, double monto, String detalle) {

        public static ResultadoCobro pendiente() {
            return new ResultadoCobro(Estado.PENDIENTE, null, 0, null);
        }
    }

    enum Estado {
        PENDIENTE, APROBADO, RECHAZADO
    }
}
