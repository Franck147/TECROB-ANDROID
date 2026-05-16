package com.example.tecrobsys.red;

import com.example.tecrobsys.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ApiDNICliente {

    private static final String URL_BASE = "https://dniruc.apisperu.com/api/v1/";
    private static Retrofit instancia = null;

    public static ApiDNI obtenerServicio() {
        if (instancia == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient cliente = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor(log)
                    .build();

            instancia = new Retrofit.Builder()
                    .baseUrl(URL_BASE)
                    .client(cliente)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return instancia.create(ApiDNI.class);
    }
}
