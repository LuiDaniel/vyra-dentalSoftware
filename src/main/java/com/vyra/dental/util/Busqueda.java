package com.vyra.dental.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Búsquedas de texto que ignoran mayúsculas y tildes: "lucia" encuentra a
 * "Lucía" y "pena" a "Peña" (en Perú se escribe mucho sin tildes).
 */
public final class Busqueda {

    // SQLite: lower() solo baja letras ASCII, por eso las mayúsculas con tilde van aparte
    private static final String[][] TILDES = {
        { "á", "a" }, { "é", "e" }, { "í", "i" }, { "ó", "o" }, { "ú", "u" }, { "ü", "u" }, { "ñ", "n" },
        { "Á", "a" }, { "É", "e" }, { "Í", "i" }, { "Ó", "o" }, { "Ú", "u" }, { "Ü", "u" }, { "Ñ", "n" },
    };

    private Busqueda() {
    }

    /** "María Peña" → "maria pena". */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinMarcas = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinMarcas.toLowerCase(Locale.ROOT).strip();
    }

    /**
     * Expresión SQL que hace lo mismo que {@link #normalizar} sobre una
     * columna, para compararla con LIKE: replace(replace(lower(col), 'á', 'a'), ...).
     */
    public static String sinTildesSql(String expresion) {
        String resultado = "lower(" + expresion + ")";
        for (String[] par : TILDES) {
            resultado = "replace(" + resultado + ", '" + par[0] + "', '" + par[1] + "')";
        }
        return resultado;
    }
}
