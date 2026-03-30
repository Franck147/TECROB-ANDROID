package com.example.tecrobsys.modelos;

import com.google.gson.annotations.SerializedName;

/**
 * ServicioCatalogo — Mapea la tabla "servicio_catalogo" de PostgreSQL.
 * Representa los servicios y repuestos disponibles del taller.
 */
public class ServicioCatalogo {

    @SerializedName("id")
    private int id;

    @SerializedName("empresa_id")
    private int empresaId;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("descripcion")
    private String descripcion;

    @SerializedName("precio_base")
    private double precioBase;

    // Valores posibles: mantenimiento, reparacion,
    // software, repuesto, diagnostico, otro
    @SerializedName("categoria")
    private String categoria;

    @SerializedName("activo")
    private boolean activo;

    // Campo local (no viene de la BD)
    // Indica si está seleccionado en la UI al agregar a una orden
    private boolean seleccionado = false;

    // Constructor vacío requerido por Gson
    public ServicioCatalogo() {}

    // ── Getters ───────────────────────────────────────────────────

    public int getId() { return id; }
    public int getEmpresaId() { return empresaId; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public double getPrecioBase() { return precioBase; }
    public String getCategoria() { return categoria; }
    public boolean isActivo() { return activo; }
    public boolean isSeleccionado() { return seleccionado; }

    // ── Setters ───────────────────────────────────────────────────

    public void setId(int id) { this.id = id; }
    public void setEmpresaId(int id) { this.empresaId = id; }
    public void setNombre(String n) { this.nombre = n; }
    public void setDescripcion(String d) { this.descripcion = d; }
    public void setPrecioBase(double p) { this.precioBase = p; }
    public void setCategoria(String c) { this.categoria = c; }
    public void setActivo(boolean a) { this.activo = a; }
    public void setSeleccionado(boolean s) { this.seleccionado = s; }

    // ── Métodos de utilidad ───────────────────────────────────────

    /** Retorna "S/ 35.00" */
    public String getPrecioFormateado() {
        return String.format("S/ %.2f", precioBase);
    }

    /** Capitaliza la categoría: "mantenimiento" → "Mantenimiento" */
    public String getCategoriaFormateada() {
        if (categoria == null || categoria.isEmpty()) return "";
        return categoria.substring(0, 1).toUpperCase() + categoria.substring(1);
    }
}