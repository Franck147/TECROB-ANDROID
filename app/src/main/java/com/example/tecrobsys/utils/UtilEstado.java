package com.example.tecrobsys.utils;

import com.example.tecrobsys.R;

/**
 * UtilEstado — Convierte estados de la BD en textos y colores para la UI.
 *
 * Los estados en PostgreSQL son en minúsculas con guión bajo: "en_progreso"
 * En la UI se muestran como texto legible con color correspondiente.
 *
 * Uso:
 *   chip.setText(UtilEstado.obtenerTexto("en_progreso"));
 *   chip.setChipBackgroundColorResource(UtilEstado.obtenerColorFondo("en_progreso"));
 */
public class UtilEstado {

    /**
     * Convierte el estado de la BD a texto legible para el usuario.
     *
     * @param estado Estado en formato BD: "en_progreso", "pendiente", etc.
     * @return Texto legible: "En progreso", "Pendiente", etc.
     */
    public static String obtenerTexto(String estado) {
        if (estado == null) return "Desconocido";
        switch (estado) {
            case "pendiente":      return "Pendiente";
            case "diagnostico":    return "Diagnóstico";
            case "en_progreso":    return "En progreso";
            case "listo":          return "Listo";
            case "entregado":      return "Entregado";
            case "cancelado":      return "Cancelado";
            case "sin_reparacion": return "Sin reparación";
            default:               return estado;
        }
    }

    /**
     * Retorna el color de fondo del chip según el estado.
     * Referencia colores definidos en colors.xml
     *
     * @param estado Estado en formato BD
     * @return Resource ID del color de fondo
     */
    public static int obtenerColorFondo(String estado) {
        if (estado == null) return R.color.estado_desconocido_fondo;
        switch (estado) {
            case "pendiente":      return R.color.estado_pendiente_fondo;
            case "diagnostico":    return R.color.estado_diagnostico_fondo;
            case "en_progreso":    return R.color.estado_progreso_fondo;
            case "listo":          return R.color.estado_listo_fondo;
            case "entregado":      return R.color.estado_entregado_fondo;
            case "cancelado":      return R.color.estado_cancelado_fondo;
            case "sin_reparacion": return R.color.estado_cancelado_fondo;
            default:               return R.color.estado_desconocido_fondo;
        }
    }

    /**
     * Retorna el color del texto del chip según el estado.
     *
     * @param estado Estado en formato BD
     * @return Resource ID del color de texto
     */
    public static int obtenerColorTexto(String estado) {
        if (estado == null) return R.color.estado_desconocido_texto;
        switch (estado) {
            case "pendiente":      return R.color.estado_pendiente_texto;
            case "diagnostico":    return R.color.estado_diagnostico_texto;
            case "en_progreso":    return R.color.estado_progreso_texto;
            case "listo":          return R.color.estado_listo_texto;
            case "entregado":      return R.color.estado_entregado_texto;
            case "cancelado":      return R.color.estado_cancelado_texto;
            case "sin_reparacion": return R.color.estado_cancelado_texto;
            default:               return R.color.estado_desconocido_texto;
        }
    }

    /**
     * Retorna todos los estados disponibles para el selector de cambio.
     * Se usa en el diálogo de cambio de estado del detalle de orden.
     */
    public static String[] obtenerTodosLosEstados() {
        return new String[]{
                "pendiente",
                "diagnostico",
                "en_progreso",
                "listo",
                "entregado",
                "cancelado"
        };
    }

    /**
     * Retorna las etiquetas legibles de todos los estados.
     * Mismo orden que obtenerTodosLosEstados().
     */
    public static String[] obtenerEtiquetasEstados() {
        return new String[]{
                "Pendiente",
                "Diagnóstico",
                "En progreso",
                "Listo",
                "Entregado",
                "Cancelado"
        };
    }
}