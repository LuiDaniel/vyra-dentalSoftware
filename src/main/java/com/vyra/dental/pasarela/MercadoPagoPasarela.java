package com.vyra.dental.pasarela;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Mercado Pago con Checkout Pro (RF-13). Con las credenciales de prueba del
 * consultorio no se mueve dinero real y no cuesta nada; con las de producción,
 * Mercado Pago cobra su comisión por cada venta.
 *
 * <ol>
 *   <li>{@link #crearOrden}: POST /checkout/preferences con el concepto, el
 *       monto en soles y la referencia ("external_reference"). Devuelve el
 *       enlace de pago (init_point), que Vyra muestra como QR.</li>
 *   <li>{@link #consultar}: GET /v1/payments/search?external_reference=... y
 *       mira si algún pago de esa referencia quedó "approved".</li>
 * </ol>
 * Documentación: https://www.mercadopago.com.pe/developers/es/docs/checkout-pro
 */
public class MercadoPagoPasarela implements PasarelaPago {

    private static final String API = "https://api.mercadopago.com";
    private static final Duration ESPERA = Duration.ofSeconds(15);

    private final String token;
    private final String moneda;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    public MercadoPagoPasarela(String token, String moneda) {
        this(token, moneda, HttpClient.newBuilder().connectTimeout(ESPERA).build());
    }

    MercadoPagoPasarela(String token, String moneda, HttpClient http) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Falta el Access Token de Mercado Pago");
        }
        this.token = token.strip();
        this.moneda = moneda == null || moneda.isBlank() ? "PEN" : moneda.strip();
        this.http = http;
    }

    @Override
    public String nombre() {
        return esDePrueba() ? "Mercado Pago (prueba)" : "Mercado Pago";
    }

    @Override
    public boolean esDemo() {
        return false;
    }

    /** Las credenciales de prueba antiguas empiezan con TEST-. */
    private boolean esDePrueba() {
        return token.startsWith("TEST-");
    }

    @Override
    public OrdenCobro crearOrden(String referencia, String concepto, double monto) {
        ObjectNode cuerpo = json.createObjectNode();
        ObjectNode item = cuerpo.putArray("items").addObject();
        item.put("title", concepto.length() > 250 ? concepto.substring(0, 250) : concepto);
        item.put("quantity", 1);
        item.put("currency_id", moneda);
        item.put("unit_price", BigDecimal.valueOf(monto).setScale(2, RoundingMode.HALF_UP));
        cuerpo.put("external_reference", referencia);
        cuerpo.put("statement_descriptor", "VYRA DENTAL");

        JsonNode respuesta = enviar(HttpRequest.newBuilder(URI.create(API + "/checkout/preferences"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString(), StandardCharsets.UTF_8)));
        String enlace = esDePrueba() && respuesta.hasNonNull("sandbox_init_point")
                ? respuesta.get("sandbox_init_point").asText()
                : respuesta.path("init_point").asText(null);
        if (enlace == null || enlace.isBlank()) {
            throw new PasarelaException("Mercado Pago no devolvió el enlace de pago.");
        }
        return new OrdenCobro(referencia, enlace, monto);
    }

    @Override
    public ResultadoCobro consultar(String referencia) {
        String url = API + "/v1/payments/search?sort=date_created&criteria=desc&external_reference="
                + URLEncoder.encode(referencia, StandardCharsets.UTF_8);
        JsonNode respuesta = enviar(HttpRequest.newBuilder(URI.create(url)).GET());
        ResultadoCobro rechazado = null;
        boolean hayPendiente = false;
        for (JsonNode pago : respuesta.path("results")) {
            String estado = pago.path("status").asText("");
            switch (estado) {
                case "approved" -> {
                    return new ResultadoCobro(Estado.APROBADO, pago.path("id").asText(),
                            pago.path("transaction_amount").asDouble(), pago.path("status_detail").asText(null));
                }
                case "rejected", "cancelled" -> {
                    if (rechazado == null) {
                        rechazado = new ResultadoCobro(Estado.RECHAZADO, pago.path("id").asText(), 0,
                                pago.path("status_detail").asText(estado));
                    }
                }
                default -> hayPendiente = true;
            }
        }
        // Si el paciente intentó y falló, pero sigue un intento en curso, se sigue esperando
        return rechazado != null && !hayPendiente ? rechazado : ResultadoCobro.pendiente();
    }

    private JsonNode enviar(HttpRequest.Builder solicitud) {
        HttpResponse<String> respuesta;
        try {
            respuesta = http.send(solicitud.timeout(ESPERA).header("Authorization", "Bearer " + token).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new PasarelaException("No hay conexión con Mercado Pago. Revisa el internet e inténtalo de nuevo.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PasarelaException("Se interrumpió la conexión con Mercado Pago.", e);
        }
        int codigo = respuesta.statusCode();
        if (codigo == 401 || codigo == 403) {
            throw new PasarelaException("Mercado Pago rechazó las credenciales. Revisa el Access Token en vyra.properties.");
        }
        if (codigo >= 400) {
            throw new PasarelaException("Mercado Pago respondió con un error (" + codigo + ").");
        }
        try {
            return json.readTree(respuesta.body());
        } catch (IOException e) {
            throw new PasarelaException("No se entendió la respuesta de Mercado Pago.", e);
        }
    }
}
