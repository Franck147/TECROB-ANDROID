package com.example.tecrobsys.modelos;

import com.google.gson.annotations.SerializedName;

/** Mapea una fila de orden_servicio con el servicio anidado (JOIN via PostgREST). */
public class ItemOrden {

    @SerializedName("precio_unitario")
    private double precioUnitario;

    @SerializedName("cantidad")
    private int cantidad;

    // PostgREST retorna el join como "servicio_catalogo": {...}
    @SerializedName("servicio_catalogo")
    private ServicioCatalogo servicio;

    public double getPrecioUnitario() { return precioUnitario; }
    public int getCantidad()          { return cantidad; }
    public ServicioCatalogo getServicio() { return servicio; }
    public double getSubtotal()       { return precioUnitario * cantidad; }
}
