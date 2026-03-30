package com.example.tecrobsys.actividades;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.example.tecrobsys.databinding.ActivityLoginBinding;
import com.example.tecrobsys.red.SupabaseServicio;
import com.example.tecrobsys.red.SupabaseCliente;
import com.example.tecrobsys.utils.SesionManager;
import com.example.tecrobsys.R;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ActividadLogin — Primera pantalla de la app.
 *
 * Flujo:
 *  1. Usuario ingresa email + contraseña
 *  2. Se llama al endpoint de autenticación de Supabase
 *  3. Supabase devuelve un token JWT si las credenciales son correctas
 *  4. Guardamos el token en SesionManager (cifrado)
 *  5. Navegamos a ActividadPrincipal
 *
 * Si ya hay una sesión activa (token guardado), se salta
 * directamente a ActividadPrincipal sin mostrar el login.
 */
public class ActividadLogin extends AppCompatActivity {

    // ViewBinding — acceso seguro a las vistas sin findViewById
    private ActivityLoginBinding enlace;

    // Maneja la sesión del usuario (token JWT cifrado)
    private SesionManager sesionManager;

    @Override
    protected void onCreate(Bundle estadoGuardado) {
        super.onCreate(estadoGuardado);

        // Inflar el layout usando ViewBinding
        enlace = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(enlace.getRoot());

        sesionManager = SesionManager.obtenerInstancia(this);

        // Si ya hay sesión activa, ir directo al dashboard
        if (sesionManager.estaAutenticado()) {
            irAActividadPrincipal();
            return; // No continuar cargando el login
        }

        configurarCampos();
        configurarBotonIngresar();
    }

    /**
     * Configura el comportamiento de los campos de texto.
     * Al presionar "Done" en el teclado del campo contraseña,
     * se dispara el login automáticamente.
     */
    private void configurarCampos() {
        enlace.campoContrasena.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                intentarLogin();
                return true;
            }
            return false;
        });
    }

    /**
     * Configura el botón de ingresar.
     */
    private void configurarBotonIngresar() {
        enlace.botonIngresar.setOnClickListener(v -> intentarLogin());
    }

    /**
     * Valida los campos y llama al endpoint de autenticación de Supabase.
     *
     * Endpoint: POST /auth/v1/token?grant_type=password
     * Body: { "email": "...", "password": "..." }
     * Respuesta: { "access_token": "...", "user": { ... } }
     */
    private void intentarLogin() {
        String email = enlace.campoEmail.getText().toString().trim();
        String contrasena = enlace.campoContrasena.getText().toString().trim();

        // ── Validaciones ──────────────────────────────────────
        if (email.isEmpty()) {
            enlace.layoutEmail.setError(getString(R.string.error_email_vacio));
            return;
        } else {
            enlace.layoutEmail.setError(null);
        }

        if (contrasena.isEmpty()) {
            enlace.layoutContrasena.setError(getString(R.string.error_contrasena_vacia));
            return;
        } else {
            enlace.layoutContrasena.setError(null);
        }

        // ── Mostrar indicador de carga ────────────────────────
        mostrarCargando(true);

        // ── Construir el cuerpo del request ───────────────────
        Map<String, String> credenciales = new HashMap<>();
        credenciales.put("email", email);
        credenciales.put("password", contrasena);

        // ── Llamar al endpoint de auth de Supabase ────────────
        SupabaseCliente.obtenerServicioAuth()
                .iniciarSesion(credenciales)
                .enqueue(new Callback<SupabaseServicio.RespuestaAuth>() {

                    @Override
                    public void onResponse(
                            Call<SupabaseServicio.RespuestaAuth> llamada,
                            Response<SupabaseServicio.RespuestaAuth> respuesta) {

                        mostrarCargando(false);

                        if (respuesta.isSuccessful() && respuesta.body() != null) {
                            // ── Login exitoso ─────────────────
                            SupabaseServicio.RespuestaAuth auth = respuesta.body();

                            // Guardar el token JWT de forma segura
                            // Por ahora usamos empresa_id=1 y tecnico_id=1
                            // En producción estos vienen del perfil del usuario
                            sesionManager.guardarSesion(
                                    auth.tokenAcceso,
                                    1,          // empresa_id
                                    1,          // tecnico_id
                                    "Nelson",   // nombre (temporal)
                                    email
                            );

                            // Ir al dashboard
                            irAActividadPrincipal();

                        } else {
                            // ── Credenciales incorrectas ──────
                            mostrarError(getString(R.string.error_credenciales));
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<SupabaseServicio.RespuestaAuth> llamada,
                            Throwable error) {

                        mostrarCargando(false);
                        mostrarError(getString(R.string.error_sin_internet));
                    }
                });
    }

    /**
     * Navega a ActividadPrincipal y cierra el login.
     * finish() evita que el usuario vuelva al login con el botón Back.
     */
    private void irAActividadPrincipal() {
        Intent intent = new Intent(this, ActividadPrincipal.class);
        startActivity(intent);
        finish(); // Cerrar el login para que no aparezca en el BackStack
    }

    /**
     * Muestra u oculta el indicador de carga.
     * Mientras carga: deshabilita el botón y oculta el teclado.
     *
     * @param cargando true para mostrar el loader, false para ocultarlo
     */
    private void mostrarCargando(boolean cargando) {
        enlace.contenedorCargando.setVisibility(
                cargando ? View.VISIBLE : View.GONE);
        enlace.botonIngresar.setEnabled(!cargando);
        enlace.campoEmail.setEnabled(!cargando);
        enlace.campoContrasena.setEnabled(!cargando);
    }

    /**
     * Muestra un Snackbar con el mensaje de error.
     * Snackbar es el componente M3 para mensajes temporales.
     */
    private void mostrarError(String mensaje) {
        Snackbar.make(enlace.getRoot(), mensaje, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getColor(R.color.estado_cancelado_fondo))
                .setTextColor(getColor(R.color.estado_cancelado_texto))
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        enlace = null; // Evitar memory leaks
    }
}