package com.example.tecrobsys.modelos;

import com.google.gson.annotations.SerializedName;

public class DNIRespuesta {

    @SerializedName("dni")
    private String dni;

    @SerializedName("nombres")
    private String nombres;

    @SerializedName("apellidoPaterno")
    private String apellidoPaterno;

    @SerializedName("apellidoMaterno")
    private String apellidoMaterno;

    @SerializedName("codVerifica")
    private String codVerifica;

    public String getDni()             { return dni; }
    public String getNombres()         { return nombres; }
    public String getApellidoPaterno() { return apellidoPaterno; }
    public String getApellidoMaterno() { return apellidoMaterno; }
    public String getCodVerifica()     { return codVerifica; }
}
