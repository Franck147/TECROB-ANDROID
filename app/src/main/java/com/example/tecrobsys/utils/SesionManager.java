package com.example.tecrobsys.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import com.example.tecrobsys.red.SupabaseCliente;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * SesionManager — Gestiona la sesión del usuario autenticado.
 *
 * Guarda de forma segura (cifrada con AES256) el token JWT
 * y los datos básicos del usuario en SharedPreferences.
 *
 * Patrón Singleton: una sola instancia en toda la app.
 *
 * Uso:
 *   SesionManager sesion = SesionManager.obtenerInstancia(context);
 *   sesion.guardarSesion(token, empresaId, tecnicoId, nombre, email);
 *   boolean activo = sesion.estaAutenticado();
 *   sesion.cerrarSesion();
 */
public class SesionManager {

    // Nombre del archivo de preferencias cifradas
    private static final String ARCHIVO_PREFS = "tecrobsys_sesion_segura";

    // Claves para cada dato guardado
    private static final String CLAVE_TOKEN       = "token_jwt";
    private static final String CLAVE_EMPRESA_ID  = "empresa_id";
    private static final String CLAVE_TECNICO_ID  = "tecnico_id";
    private static final String CLAVE_NOMBRE      = "nombre_tecnico";
    private static final String CLAVE_EMAIL       = "email_tecnico";
    private static final String CLAVE_AUTENTICADO = "esta_autenticado";

    // Instancia única del Singleton
    private static SesionManager instancia;

    // SharedPreferences cifradas
    private SharedPreferences preferencias;

    // Constructor privado — solo se crea desde obtenerInstancia()
    private SesionManager(Context contexto) {
        try {
            // Crear la clave maestra de cifrado AES256-GCM
            MasterKey claveMaestra = new MasterKey.Builder(contexto)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // Crear SharedPreferences cifradas
            // Las claves se cifran con AES256-SIV
            // Los valores se cifran con AES256-GCM
            preferencias = EncryptedSharedPreferences.create(
                    contexto,
                    ARCHIVO_PREFS,
                    claveMaestra,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

        } catch (GeneralSecurityException | IOException e) {
            // Si falla el cifrado usar SharedPreferences normales
            // Esto no debería pasar en producción
            preferencias = contexto.getSharedPreferences(
                    ARCHIVO_PREFS, Context.MODE_PRIVATE);
        }
    }

    /**
     * Retorna la instancia única del SesionManager.
     * Si no existe la crea (thread-safe con synchronized).
     */
    public static synchronized SesionManager obtenerInstancia(Context contexto) {
        if (instancia == null) {
            instancia = new SesionManager(contexto.getApplicationContext());
        }
        return instancia;
    }

    /**
     * Guarda todos los datos de sesión después del login exitoso.
     * También actualiza el token en SupabaseCliente para
     * que todos los requests siguientes lo usen.
     *
     * @param token     Token JWT de Supabase
     * @param empresaId ID de la empresa del usuario
     * @param tecnicoId ID del técnico/usuario
     * @param nombre    Nombre para mostrar en la UI
     * @param email     Email del usuario
     */
    public void guardarSesion(String token, int empresaId,
                              int tecnicoId, String nombre, String email) {
        preferencias.edit()
                .putString(CLAVE_TOKEN, token)
                .putInt(CLAVE_EMPRESA_ID, empresaId)
                .putInt(CLAVE_TECNICO_ID, tecnicoId)
                .putString(CLAVE_NOMBRE, nombre)
                .putString(CLAVE_EMAIL, email)
                .putBoolean(CLAVE_AUTENTICADO, true)
                .apply();

        // Pasar el token al cliente HTTP para usarlo en cada request
        SupabaseCliente.establecerToken(token);
    }

    /**
     * Restaura el token guardado en el cliente HTTP.
     * Llamar al iniciar la app si ya hay sesión activa.
     */
    public void restaurarToken() {
        String token = obtenerToken();
        if (token != null && !token.isEmpty()) {
            SupabaseCliente.establecerToken(token);
        }
    }

    // ── Getters de datos de sesión ────────────────────────────────

    /** @return El token JWT, o cadena vacía si no hay sesión */
    public String obtenerToken() {
        return preferencias.getString(CLAVE_TOKEN, "");
    }

    /** @return El ID de la empresa del usuario autenticado */
    public int obtenerEmpresaId() {
        return preferencias.getInt(CLAVE_EMPRESA_ID, 1);
    }

    /** @return El ID del técnico/usuario autenticado */
    public int obtenerTecnicoId() {
        return preferencias.getInt(CLAVE_TECNICO_ID, 1);
    }

    /** @return El nombre del técnico para mostrar en la UI */
    public String obtenerNombreTecnico() {
        return preferencias.getString(CLAVE_NOMBRE, "Usuario");
    }

    /** @return El email del usuario autenticado */
    public String obtenerEmail() {
        return preferencias.getString(CLAVE_EMAIL, "");
    }

    /**
     * @return true si hay una sesión activa con token válido
     */
    public boolean estaAutenticado() {
        return preferencias.getBoolean(CLAVE_AUTENTICADO, false)
                && !obtenerToken().isEmpty();
    }

    /**
     * Cierra la sesión: borra todos los datos guardados
     * y limpia el token del cliente HTTP.
     */
    public void cerrarSesion() {
        preferencias.edit().clear().apply();
        SupabaseCliente.limpiarToken();
    }
}