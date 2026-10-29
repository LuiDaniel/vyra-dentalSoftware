package com.vyra.dental.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de contraseñas con PBKDF2WithHmacSHA256 y sal aleatoria (RNF-05, RN-13).
 * El resultado se guarda tal cual en usuario.contrasena_hash con el formato
 * {@code pbkdf2$iteraciones$salBase64$hashBase64}, para poder verificarlo
 * aunque en el futuro cambie el número de iteraciones.
 */
public final class HashUtil {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIJO = "pbkdf2";
    private static final int ITERACIONES = 120_000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private HashUtil() {
    }

    public static String hashear(String contrasena) {
        if (contrasena == null) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        byte[] sal = new byte[BYTES_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = calcular(contrasena.toCharArray(), sal, ITERACIONES);
        Base64.Encoder base64 = Base64.getEncoder();
        return PREFIJO + "$" + ITERACIONES + "$" + base64.encodeToString(sal) + "$" + base64.encodeToString(hash);
    }

    /**
     * Devuelve true si la contraseña corresponde al hash guardado.
     * Un hash vacío o con otro formato nunca coincide.
     */
    public static boolean verificar(String contrasena, String hashGuardado) {
        if (contrasena == null || hashGuardado == null) {
            return false;
        }
        String[] partes = hashGuardado.split("\\$");
        if (partes.length != 4 || !PREFIJO.equals(partes[0])) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = calcular(contrasena.toCharArray(), sal, iteraciones);
            // Comparación en tiempo constante, para no revelar cuántos bytes coinciden
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] calcular(char[] contrasena, byte[] sal, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(contrasena, sal, iteraciones, BITS_HASH);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        } finally {
            spec.clearPassword();
        }
    }
}
