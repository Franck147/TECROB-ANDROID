package com.example.tecrobsys.modelos;

import com.google.gson.annotations.SerializedName;

/**
 * Cliente — Mapea la tabla "cliente" de PostgreSQL/Supabase.
 */
public class Cliente {

    @SerializedName("id")
    private int id;

    @SerializedName("empresa_id")
    private int empresaId;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("apellido")
    private String apellido;

    @SerializedName("telefono")
    private String telefono;

    @SerializedName("email")
    private String email;

    @SerializedName("dni")
    private String dni;

    @SerializedName("direccion")
    private String direccion;

    @SerializedName("created_at")
    private String creadoEn;

    // Constructor vacío requerido por Gson
    public Cliente() {}

    // ── Getters ───────────────────────────────────────────────────

    public int getId() { return id; }
    public int getEmpresaId() { return empresaId; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public String getDni() { return dni; }
    public String getDireccion() { return direccion; }
    public String getCreadoEn() { return creadoEn; }

    // ── Setters ───────────────────────────────────────────────────

    public void setId(int id) { this.id = id; }
    public void setEmpresaId(int id) { this.empresaId = id; }
    public void setNombre(String n) { this.nombre = n; }
    public void setApellido(String a) { this.apellido = a; }
    public void setTelefono(String t) { this.telefono = t; }
    public void setEmail(String e) { this.email = e; }
    public void setDni(String d) { this.dni = d; }
    public void setDireccion(String d) { this.direccion = d; }

    // ── Métodos de utilidad ───────────────────────────────────────

    /** Retorna "Carlos Mendoza" */
    public String getNombreCompleto() {
        String n = nombre != null ? nombre : "";
        String a = apellido != null ? apellido : "";
        return (n + " " + a).trim();
    }

    /** Retorna "CM" para el avatar circular */
    public String getIniciales() {
        String ini = "";
        if (nombre != null && !nombre.isEmpty())
            ini += nombre.charAt(0);
        if (apellido != null && !apellido.isEmpty())
            ini += apellido.charAt(0);
        return ini.toUpperCase();
    }

    /** Retorna el texto a mostrar en el autocompletado de búsqueda */
    public String getTextoAutocompletado() {
        return getNombreCompleto() + " — " + (telefono != null ? telefono : "");
    }
}