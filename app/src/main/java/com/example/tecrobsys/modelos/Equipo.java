package com.example.tecrobsys.modelos;

import com.google.gson.annotations.SerializedName;

/**
 * Equipo — Mapea la tabla "equipo" de PostgreSQL/Supabase.
 * Relación 1 a 1 con Orden: cada orden tiene exactamente un equipo.
 */
public class Equipo {

    @SerializedName("id")
    private int id;

    @SerializedName("orden_id")
    private int ordenId;

    // Valores posibles: laptop, computadora, impresora,
    // fotocopiadora, tablet, celular, parlante, otro
    @SerializedName("tipo")
    private String tipo;

    @SerializedName("marca")
    private String marca;

    @SerializedName("modelo")
    private String modelo;

    @SerializedName("numero_serie")
    private String numeroSerie;

    @SerializedName("desperfecto")
    private String desperfecto;

    @SerializedName("descripcion_general")
    private String descripcionGeneral;

    @SerializedName("created_at")
    private String creadoEn;

    // Constructor vacío requerido por Gson
    public Equipo() {}

    // ── Getters ───────────────────────────────────────────────────

    public int getId() { return id; }
    public int getOrdenId() { return ordenId; }
    public String getTipo() { return tipo; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getNumeroSerie() { return numeroSerie; }
    public String getDesperfecto() { return desperfecto; }
    public String getDescripcionGeneral() { return descripcionGeneral; }
    public String getCreadoEn() { return creadoEn; }

    // ── Setters ───────────────────────────────────────────────────

    public void setId(int id) { this.id = id; }
    public void setOrdenId(int id) { this.ordenId = id; }
    public void setTipo(String t) { this.tipo = t; }
    public void setMarca(String m) { this.marca = m; }
    public void setModelo(String m) { this.modelo = m; }
    public void setNumeroSerie(String n) { this.numeroSerie = n; }
    public void setDesperfecto(String d) { this.desperfecto = d; }
    public void setDescripcionGeneral(String d) { this.descripcionGeneral = d; }

    // ── Métodos de utilidad ───────────────────────────────────────

    /** Retorna "HP Pavilion 15" */
    public String getNombreCompleto() {
        String m = marca != null ? marca : "";
        String mo = modelo != null ? " " + modelo : "";
        return (m + mo).trim();
    }

    /** Capitaliza el tipo: "laptop" → "Laptop" */
    public String getTipoFormateado() {
        if (tipo == null || tipo.isEmpty()) return "";
        return tipo.substring(0, 1).toUpperCase() + tipo.substring(1);
    }
}