package com.example.tecrobsys.repositorios;

import com.example.tecrobsys.modelos.Tecnico;
import com.example.tecrobsys.red.SupabaseCliente;
import com.example.tecrobsys.red.SupabaseServicio;
import java.util.List;
import retrofit2.Callback;

/**
 * AuthRepository — Capa de datos para autenticación y perfiles.
 *
 * Responsabilidades:
 *   1. Iniciar sesión con email/password en Supabase Auth
 *   2. Obtener el perfil completo del técnico (nombre, rol, empresa)
 *
 * No tiene lógica de UI. Solo hace llamadas HTTP y devuelve
 * el resultado vía callbacks de Retrofit.
 */
public class AuthRepository {

    /**
     * Inicia sesión en Supabase Auth con email y contraseña.
     *
     * @param email      Email del usuario
     * @param password   Contraseña del usuario
     * @param callback   Recibe RespuestaAuth con el token JWT en éxito
     */
    public void iniciarSesion(String email, String password,
                               Callback<SupabaseServicio.RespuestaAuth> callback) {
        java.util.Map<String, String> credenciales = new java.util.HashMap<>();
        credenciales.put("email", email);
        credenciales.put("password", password);

        SupabaseCliente.obtenerServicioAuth()
                .iniciarSesion(credenciales)
                .enqueue(callback);
    }

    /**
     * Obtiene el perfil del técnico desde la tabla "tecnico" por auth_user_id.
     * Se llama DESPUÉS del login exitoso para obtener datos reales
     * (id, nombre, empresa, rol). Usar este método es más confiable que
     * buscar por email porque el UUID siempre coincide con Supabase Auth.
     *
     * @param authUserId UUID de Supabase Auth (viene en RespuestaAuth.usuario.id)
     * @param callback   Recibe List<Tecnico> — normalmente 1 elemento
     */
    public void obtenerPerfilPorAuthId(String authUserId,
                                        Callback<List<Tecnico>> callback) {
        SupabaseCliente.obtenerServicio()
                .obtenerPerfilPorAuthId("eq." + authUserId)
                .enqueue(callback);
    }

    /** @deprecated Usar obtenerPerfilPorAuthId */
    public void obtenerPerfilPorEmail(String email,
                                       Callback<List<Tecnico>> callback) {
        SupabaseCliente.obtenerServicio()
                .obtenerPerfilPorEmail("eq." + email)
                .enqueue(callback);
    }
}
