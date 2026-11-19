package com.vyra.dental.pasarela;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.vyra.dental.util.ConexionBD;

/**
 * Decide qué pasarela usar. El Access Token de Mercado Pago se busca, en orden:
 * <ol>
 *   <li>la variable de entorno {@code VYRA_MP_ACCESS_TOKEN};</li>
 *   <li>la clave {@code mercadopago.access_token} del archivo
 *       {@code vyra.properties}, en la carpeta de los datos (junto a vyra.db;
 *       en la versión instalada, %LOCALAPPDATA%\Vyra).</li>
 * </ol>
 * Sin token, Vyra usa {@link PasarelaDemo}. El token es secreto: no se sube a
 * Git (vyra.properties está en .gitignore); ver vyra.properties.ejemplo.
 */
public final class ConfigPasarela {

    public static final String ARCHIVO = "vyra.properties";
    public static final String VARIABLE_TOKEN = "VYRA_MP_ACCESS_TOKEN";
    public static final String CLAVE_TOKEN = "mercadopago.access_token";
    public static final String CLAVE_MONEDA = "mercadopago.moneda";

    private ConfigPasarela() {
    }

    public static PasarelaPago crear() {
        Properties propiedades = leer(ConexionBD.carpetaDatos().resolve(ARCHIVO));
        String token = System.getenv(VARIABLE_TOKEN);
        if (token == null || token.isBlank()) {
            token = propiedades.getProperty(CLAVE_TOKEN, "");
        }
        if (token.isBlank()) {
            return new PasarelaDemo();
        }
        return new MercadoPagoPasarela(token, propiedades.getProperty(CLAVE_MONEDA, "PEN"));
    }

    private static Properties leer(Path archivo) {
        Properties propiedades = new Properties();
        if (Files.isRegularFile(archivo)) {
            try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                propiedades.load(lector);
            } catch (IOException e) {
                // Un archivo ilegible se trata como si no existiera: se usa la demo
            }
        }
        return propiedades;
    }
}
