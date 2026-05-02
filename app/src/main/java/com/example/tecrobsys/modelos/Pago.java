package com.example.tecrobsys.modelos;

import com.google.gson.annotations.SerializedName;

public class Pago {

    @SerializedName("id")
    private int id;

    @SerializedName("orden_id")
    private int ordenId;

    @SerializedName("monto")
    private double monto;

    // efectivo | yape | plin | transferencia | tarjeta
    @SerializedName("metodo")
    private String metodo;

    @SerializedName("nota")
    private String nota;

    @SerializedName("created_at")
    private String creadoEn;

    public Pago() {}

    public int getId() { return id; }
    public int getOrdenId() { return ordenId; }
    public double getMonto() { return monto; }
    public String getMetodo() { return metodo; }
    public String getNota() { return nota; }
    public String getCreadoEn() { return creadoEn; }

    public void setId(int id) { this.id = id; }
    public void setOrdenId(int ordenId) { this.ordenId = ordenId; }
    public void setMonto(double monto) { this.monto = monto; }
    public void setMetodo(String metodo) { this.metodo = metodo; }
    public void setNota(String nota) { this.nota = nota; }
}
