package com.vyra.dental.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Map;

/**
 * Formatos de texto que ve el usuario (docs/08-guia-frontend.md §9).
 * Los valores null se muestran como texto vacío.
 */
public final class Formatos {

    public static final Locale ES_PE = Locale.of("es", "PE");

    private static final DateTimeFormatter FECHA_LARGA =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", ES_PE);

    // Abreviaturas propias: el JDK agrega un punto ("oct.") y la guía pide "29 oct 2026"
    private static final DateTimeFormatter FECHA_CORTA = new DateTimeFormatterBuilder()
            .appendPattern("dd ")
            .appendText(ChronoField.MONTH_OF_YEAR, Map.ofEntries(
                    Map.entry(1L, "ene"), Map.entry(2L, "feb"), Map.entry(3L, "mar"),
                    Map.entry(4L, "abr"), Map.entry(5L, "may"), Map.entry(6L, "jun"),
                    Map.entry(7L, "jul"), Map.entry(8L, "ago"), Map.entry(9L, "set"),
                    Map.entry(10L, "oct"), Map.entry(11L, "nov"), Map.entry(12L, "dic")))
            .appendPattern(" yyyy")
            .toFormatter(ES_PE);

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MES_ANIO = DateTimeFormatter.ofPattern("MMMM yyyy", ES_PE);
    private static final DateTimeFormatter FECHA_CAMPO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formatos() {
    }

    /** "Jueves, 29 de octubre de 2026". */
    public static String fechaLarga(LocalDate fecha) {
        if (fecha == null) {
            return "";
        }
        String texto = FECHA_LARGA.format(fecha);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    /** "29 oct 2026". */
    public static String fechaCorta(LocalDate fecha) {
        return fecha == null ? "" : FECHA_CORTA.format(fecha);
    }

    /** "marzo 2024" (para "Paciente desde marzo 2024"). */
    public static String mesAnio(LocalDate fecha) {
        return fecha == null ? "" : MES_ANIO.format(fecha);
    }

    /** Formato de los campos de fecha (DatePicker): "14/03/1992". */
    public static DateTimeFormatter formatoFechaCampo() {
        return FECHA_CAMPO;
    }

    /** "09:30". */
    public static String hora(LocalTime hora) {
        return hora == null ? "" : HORA.format(hora);
    }

    /** "S/ 1,250.00", con coma de miles y punto decimal en cualquier PC. */
    public static String dinero(double monto) {
        // DecimalFormat no es seguro entre hilos, por eso se crea en cada llamada
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return "S/ " + formato.format(monto);
    }

    /** "987 654 321". Si no tiene 9 dígitos se devuelve sin cambios. */
    public static String telefono(String telefono) {
        if (telefono == null) {
            return "";
        }
        if (!telefono.matches("\\d{9}")) {
            return telefono;
        }
        return telefono.substring(0, 3) + " " + telefono.substring(3, 6) + " " + telefono.substring(6);
    }

    /** "34 años" (años cumplidos a hoy). */
    public static String edad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            return "";
        }
        int anios = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        return anios + (anios == 1 ? " año" : " años");
    }

    /** "MQ": primera letra del primer nombre y del primer apellido. */
    public static String iniciales(String nombres, String apellidos) {
        return primeraLetra(nombres) + primeraLetra(apellidos);
    }

    /** "MQ" a partir de un nombre completo ("María Quispe"); "D" si es una sola palabra. */
    public static String iniciales(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            return "";
        }
        String[] palabras = nombreCompleto.strip().split("\\s+");
        return primeraLetra(palabras[0]) + (palabras.length > 1 ? primeraLetra(palabras[1]) : "");
    }

    private static String primeraLetra(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        return texto.strip().substring(0, 1).toUpperCase(ES_PE);
    }
}
