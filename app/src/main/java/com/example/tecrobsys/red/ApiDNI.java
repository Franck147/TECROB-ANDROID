package com.example.tecrobsys.red;

import com.example.tecrobsys.modelos.DNIRespuesta;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiDNI {

    @GET("dni/{numero}")
    Call<DNIRespuesta> consultar(
            @Path("numero") String numero,
            @Query("token") String token);
}
