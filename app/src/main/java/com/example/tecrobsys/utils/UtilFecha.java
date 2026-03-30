package com.example.tecrobsys.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * UtilFecha — Utilidades para formatear fechas en la UI.
 *
 * Supabase devuelve las fechas en formato ISO 8601:
 *   "2026-03-23T08:15:00.000Z"
 *
 * Esta clase las convierte a formatos legibles para el usuario.
 */
public class UtilFecha {

    // Locale en español de Perú
    private static final Locale LOCALE_ES = new Locale("es", "PE");

    /**
     * Retorna la fecha de hoy en formato largo.
     * Ejemplo: "Lunes 23 de marzo, 2026"
     */
    public static String obtenerFechaHoy() {
        SimpleDateFormat formato = new SimpleDateFormat(
                "EEEE d 'de' MMMM, yyyy", LOCALE_ES);
        String fecha = formato.format(new Date());
        // Capitalizar primera letra
        if (fecha.isEmpty()) return "";
        return fecha.substring(0, 1).toUpperCase() + fecha.substring(1);
    }

    /**
     * Formatea una fecha ISO 8601 de Supabase a formato corto.
     * Entrada:  "2026-03-23T08:15:00.000Z"
     * Salida:   "23 Mar 2026, 08:15"
     *
     * @param fechaIso Fecha en formato ISO 8601
     * @return Fecha formateada o "—" si es null/inválida
     */
    public static String formatearFechaCorta(String fechaIso) {
        if (fechaIso == null || fechaIso.isEmpty()) return "—";
        try {
            SimpleDateFormat entrada = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss", LOCALE_ES);
            SimpleDateFormat salida = new SimpleDateFormat(
                    "dd MMM yyyy, HH:mm", LOCALE_ES);
            Date fecha = entrada.parse(fechaIso);
            return fecha != null ? salida.format(fecha) : "—";
        } catch (Exception e) {
            // Si falla el parseo retornar solo la parte de la fecha
            if (fechaIso.length() >= 10) return fechaIso.substring(0, 10);
            return "—";
        }
    }

    /**
     * Retorna el tiempo transcurrido desde una fecha ISO 8601.
     * Ejemplos: "hace 5 min", "hace 2h", "hace 3 días", "ayer"
     *
     * @param fechaIso Fecha en formato ISO 8601
     * @return Texto relativo del tiempo transcurrido
     */
    public static String tiempoTranscurrido(String fechaIso) {
        if (fechaIso == null || fechaIso.isEmpty()) return "";
        try {
            SimpleDateFormat formato = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss", LOCALE_ES);
            Date fecha = formato.parse(fechaIso);
            if (fecha == null) return "";

            long diferenciaMs = System.currentTimeMillis() - fecha.getTime();
            long minutos = diferenciaMs / 60_000;
            long horas   = minutos / 60;
            long dias    = horas / 24;

            if (minutos < 1)    return "ahora";
            if (minutos < 60)   return "hace " + minutos + " min";
            if (horas < 24)     return "hace " + horas + "h";
            if (dias == 1)      return "ayer";
            if (dias < 7)       return "hace " + dias + " días";
            return formatearFechaCorta(fechaIso);

        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Formatea solo la hora de una fecha ISO 8601.
     * Entrada:  "2026-03-23T08:15:00.000Z"
     * Salida:   "08:15 AM"
     */
    public static String formatearHora(String fechaIso) {
        if (fechaIso == null || fechaIso.isEmpty()) return "—";
        try {
            SimpleDateFormat entrada = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss", LOCALE_ES);
            SimpleDateFormat salida = new SimpleDateFormat(
                    "hh:mm a", LOCALE_ES);
            Date fecha = entrada.parse(fechaIso);
            return fecha != null ? salida.format(fecha) : "—";
        } catch (Exception e) {
            return "—";
        }
    }
}