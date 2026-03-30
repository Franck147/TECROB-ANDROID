package com.example.tecrobsys.red;

import com.example.tecrobsys.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

/**
 * SupabaseCliente — Singleton que configura la conexión HTTP con Supabase.
 *
 * Supabase expone una API REST sobre PostgreSQL.
 * Cada request necesita estos headers obligatorios:
 *   - apikey:        identifica el proyecto Supabase
 *   - Authorization: Bearer <token>  autentica al usuario
 *   - Content-Type:  application/json
 *
 * Tenemos DOS instancias de Retrofit:
 *   1. obtenerServicio()     → para /rest/v1/ (datos: órdenes, clientes, etc.)
 *   2. obtenerServicioAuth() → para /auth/v1/ (login)
 *
 * La URL y la apikey vienen de BuildConfig (definidas en build.gradle)
 * así nunca quedan hardcodeadas en el código fuente.
 */
public class SupabaseCliente {

    // ── Instancias Singleton ───────────────────────────────────────
    private static Retrofit instanciaRest = null;   // Para /rest/v1/
    private static Retrofit instanciaAuth = null;   // Para /auth/v1/

    // Token JWT del usuario autenticado (se actualiza al hacer login)
    private static String tokenSesion = null;

    // ──────────────────────────────────────────────────────────────

    /**
     * Guarda el token JWT después del login exitoso.
     * Resetea las instancias para que usen el nuevo token.
     */
    public static void establecerToken(String token) {
        tokenSesion = token;
        instanciaRest = null; // Forzar recreación con el nuevo token
        instanciaAuth = null;
    }

    /**
     * Borra el token al cerrar sesión.
     */
    public static void limpiarToken() {
        tokenSesion = null;
        instanciaRest = null;
        instanciaAuth = null;
    }

    /**
     * Construye el cliente HTTP con los interceptores de Supabase.
     * Se comparte entre la instancia REST y Auth.
     */
    private static OkHttpClient construirClienteHttp() {

        // Interceptor de logs — muestra requests/responses en Logcat
        // Solo activo en debug, en release no genera logs
        HttpLoggingInterceptor interceptorLog = new HttpLoggingInterceptor();
        interceptorLog.setLevel(
                BuildConfig.DEBUG
                        ? HttpLoggingInterceptor.Level.BODY
                        : HttpLoggingInterceptor.Level.NONE
        );

        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(interceptorLog)
                // Interceptor de autenticación:
                // agrega los headers de Supabase a CADA request
                .addInterceptor(cadena -> {
                    Request original = cadena.request();
                    Request.Builder constructor = original.newBuilder()
                            // apikey: identifica tu proyecto Supabase
                            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                            // Content-Type: para enviar y recibir JSON
                            .header("Content-Type", "application/json")
                            // Prefer: le dice a Supabase que devuelva
                            // el registro creado/actualizado en la respuesta
                            .header("Prefer", "return=representation");

                    // Si hay sesión activa agregar el token del usuario
                    if (tokenSesion != null && !tokenSesion.isEmpty()) {
                        constructor.header("Authorization",
                                "Bearer " + tokenSesion);
                    } else {
                        // Sin sesión usar la anon key como Bearer
                        constructor.header("Authorization",
                                "Bearer " + BuildConfig.SUPABASE_ANON_KEY);
                    }

                    return cadena.proceed(constructor.build());
                })
                .build();
    }

    /**
     * Retorna la instancia de Retrofit para la API REST de datos.
     * URL base: https://TU_PROYECTO.supabase.co/rest/v1/
     *
     * Uso: SupabaseCliente.obtenerServicio().listarOrdenes(...)
     */
    public static SupabaseServicio obtenerServicio() {
        if (instanciaRest == null) {
            instanciaRest = new Retrofit.Builder()
                    .baseUrl(BuildConfig.SUPABASE_URL + "/rest/v1/")
                    .client(construirClienteHttp())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return instanciaRest.create(SupabaseServicio.class);
    }

    /**
     * Retorna la instancia de Retrofit para la API de autenticación.
     * URL base: https://TU_PROYECTO.supabase.co/auth/v1/
     *
     * Uso: SupabaseCliente.obtenerServicioAuth().iniciarSesion(...)
     */
    public static SupabaseServicio obtenerServicioAuth() {
        if (instanciaAuth == null) {
            instanciaAuth = new Retrofit.Builder()
                    .baseUrl(BuildConfig.SUPABASE_URL + "/auth/v1/")
                    .client(construirClienteHttp())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return instanciaAuth.create(SupabaseServicio.class);
    }
}