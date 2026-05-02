package com.example.tecrobsys.modelos;

import com.google.gson.annotations.SerializedName;

/**
 * Tecnico — Mapea la tabla "tecnico" de PostgreSQL/Supabase.
 * Contiene el perfil del usuario autenticado: nombre real, empresa y rol.
 *
 * El campo rol puede ser:
 *   "administrador" → acceso completo (configuración, reportes globales)
 *   "tecnico"       → acceso estándar (solo gestión de órdenes)
 */
public class Tecnico {

    @SerializedName("id")
    private int id;

    @SerializedName("empresa_id")
    private int empresaId;

    @SerializedName("auth_user_id")
    private String authUserId;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("apellido")
    private String apellido;

    @SerializedName("email")
    private String email;

    @SerializedName("rol")
    private String rol;

    @SerializedName("activo")
    private boolean activo;

    @SerializedName("created_at")
    private String creadoEn;

    // Constructor vacío requerido por Gson
    public Tecnico() {}

    // ── Getters ───────────────────────────────────────────────────

    public int getId()           { return id; }
    public int getEmpresaId()    { return empresaId; }
    public String getAuthUserId(){ return authUserId; }
    public String getNombre()    { return nombre; }
    public String getApellido()  { return apellido; }
    public String getEmail()     { return email; }
    public String getRol()       { return rol; }
    public boolean isActivo()    { return activo; }
    public String getCreadoEn()  { return creadoEn; }

    // ── Setters ───────────────────────────────────────────────────

    public void setId(int id)                  { this.id = id; }
    public void setEmpresaId(int id)           { this.empresaId = id; }
    public void setAuthUserId(String uid)      { this.authUserId = uid; }
    public void setNombre(String n)            { this.nombre = n; }
    public void setApellido(String a)          { this.apellido = a; }
    public void setEmail(String e)             { this.email = e; }
    public void setRol(String r)               { this.rol = r; }
    public void setActivo(boolean a)           { this.activo = a; }

    // ── Métodos de utilidad ───────────────────────────────────────

    /** Retorna "Nelson Quispe" */
    public String getNombreCompleto() {
        String n = nombre   != null ? nombre   : "";
        String a = apellido != null ? apellido : "";
        return (n + " " + a).trim();
    }

    /** true si el técnico tiene rol de administrador */
    public boolean esAdministrador() {
        return "administrador".equals(rol);
    }
}
