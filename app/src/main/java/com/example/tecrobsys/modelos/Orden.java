package com.example.tecrobsys.modelos;

import com.google.gson.annotations.SerializedName;

/**
 * Orden — Modelo principal del sistema.
 * Mapea exactamente la tabla "orden" de PostgreSQL/Supabase.
 *
 * @SerializedName: indica el nombre del campo en el JSON de Supabase.
 * Si el nombre en Java coincide con el JSON, no hace falta la anotación,
 * pero la ponemos siempre para mayor claridad.
 */
public class Orden {

    @SerializedName("id")
    private int id;

    // Número legible de la orden: ORD-0041
    // Lo genera automáticamente el trigger en PostgreSQL
    @SerializedName("numero_orden")
    private String numeroOrden;

    @SerializedName("empresa_id")
    private int empresaId;

    @SerializedName("cliente_id")
    private int clienteId;

    @SerializedName("tecnico_id")
    private int tecnicoId;

    // Valores posibles: pendiente, diagnostico, en_progreso,
    // listo, entregado, cancelado, sin_reparacion
    @SerializedName("estado")
    private String estado;

    // Valores posibles: baja, normal, alta, urgente
    @SerializedName("prioridad")
    private String prioridad;

    @SerializedName("adelanto")
    private double adelanto;

    @SerializedName("descuento")
    private double descuento;

    @SerializedName("subtotal")
    private double subtotal;

    @SerializedName("total")
    private double total;

    @SerializedName("saldo_pendiente")
    private double saldoPendiente;

    @SerializedName("fecha_prometida")
    private String fechaPrometida;

    @SerializedName("observaciones")
    private String observaciones;

    @SerializedName("pdf_url")
    private String pdfUrl;

    // Fecha de creación en formato ISO 8601: "2026-03-23T08:15:00.000Z"
    @SerializedName("created_at")
    private String creadoEn;

    @SerializedName("updated_at")
    private String actualizadoEn;

    // ── Campos expandidos con JOIN de Supabase ────────────────────
    // Cuando la query incluye select=*,cliente:cliente_id(nombre,...)
    // Supabase devuelve el objeto cliente anidado en el JSON
    @SerializedName("cliente")
    private ClienteResumen cliente;

    @SerializedName("tecnico")
    private UsuarioResumen tecnico;

    @SerializedName("equipo")
    private Equipo equipo;

    // Constructor vacío requerido por Gson para deserializar JSON
    public Orden() {}

    // ── Getters ───────────────────────────────────────────────────

    public int getId() { return id; }
    public String getNumeroOrden() { return numeroOrden; }
    public int getEmpresaId() { return empresaId; }
    public int getClienteId() { return clienteId; }
    public int getTecnicoId() { return tecnicoId; }
    public String getEstado() { return estado; }
    public String getPrioridad() { return prioridad; }
    public double getAdelanto() { return adelanto; }
    public double getDescuento() { return descuento; }
    public double getSubtotal() { return subtotal; }
    public double getTotal() { return total; }
    public double getSaldoPendiente() { return saldoPendiente; }
    public String getFechaPrometida() { return fechaPrometida; }
    public String getObservaciones() { return observaciones; }
    public String getPdfUrl() { return pdfUrl; }
    public String getCreadoEn() { return creadoEn; }
    public String getActualizadoEn() { return actualizadoEn; }
    public ClienteResumen getCliente() { return cliente; }
    public UsuarioResumen getTecnico() { return tecnico; }
    public Equipo getEquipo() { return equipo; }

    // ── Setters ───────────────────────────────────────────────────

    public void setId(int id) { this.id = id; }
    public void setNumeroOrden(String n) { this.numeroOrden = n; }
    public void setEmpresaId(int id) { this.empresaId = id; }
    public void setClienteId(int id) { this.clienteId = id; }
    public void setTecnicoId(int id) { this.tecnicoId = id; }
    public void setEstado(String e) { this.estado = e; }
    public void setPrioridad(String p) { this.prioridad = p; }
    public void setAdelanto(double a) { this.adelanto = a; }
    public void setDescuento(double d) { this.descuento = d; }
    public void setSubtotal(double s) { this.subtotal = s; }
    public void setTotal(double t) { this.total = t; }
    public void setSaldoPendiente(double s) { this.saldoPendiente = s; }
    public void setFechaPrometida(String f) { this.fechaPrometida = f; }
    public void setObservaciones(String o) { this.observaciones = o; }
    public void setPdfUrl(String p) { this.pdfUrl = p; }
    public void setCliente(ClienteResumen c) { this.cliente = c; }
    public void setTecnico(UsuarioResumen t) { this.tecnico = t; }
    public void setEquipo(Equipo e) { this.equipo = e; }

    // ── Clase interna: resumen de cliente para mostrar en lista ───
    // Solo trae los campos necesarios para la UI (no todos)
    public static class ClienteResumen {

        @SerializedName("nombre")
        private String nombre;

        @SerializedName("apellido")
        private String apellido;

        @SerializedName("telefono")
        private String telefono;

        public String getNombre() { return nombre; }
        public String getApellido() { return apellido; }
        public String getTelefono() { return telefono; }
        public void setNombre(String n) { this.nombre = n; }
        public void setApellido(String a) { this.apellido = a; }
        public void setTelefono(String t) { this.telefono = t; }

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
    }

    // ── Clase interna: resumen del técnico ────────────────────────
    public static class UsuarioResumen {

        @SerializedName("nombre")
        private String nombre;

        @SerializedName("apellido")
        private String apellido;

        public String getNombre() { return nombre; }
        public String getApellido() { return apellido; }
        public void setNombre(String n) { this.nombre = n; }
        public void setApellido(String a) { this.apellido = a; }

        /** Retorna "Nelson Quispe" */
        public String getNombreCompleto() {
            String n = nombre != null ? nombre : "";
            String a = apellido != null ? apellido : "";
            return (n + " " + a).trim();
        }

        /** Retorna "NQ" para el avatar circular */
        public String getIniciales() {
            String ini = "";
            if (nombre != null && !nombre.isEmpty())
                ini += nombre.charAt(0);
            if (apellido != null && !apellido.isEmpty())
                ini += apellido.charAt(0);
            return ini.toUpperCase();
        }
    }
}