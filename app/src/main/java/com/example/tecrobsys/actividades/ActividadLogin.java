package com.example.tecrobsys.actividades;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

public class ActividadLogin extends AppCompatActivity {

    private static final String TAG = "TECROB_LOGIN";

    private ActivityLoginBinding enlace;
    private SesionManager sesionManager;

    @Override
    protected void onCreate(Bundle estadoGuardado) {
        super.onCreate(estadoGuardado);
        enlace = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(enlace.getRoot());

        sesionManager = SesionManager.obtenerInstancia(this);

        if (sesionManager.estaAutenticado()) {
            Log.d(TAG, "Sesión activa encontrada, saltando al dashboard");
            irAActividadPrincipal();
            return;
        }

        configurarCampos();
        configurarBotonIngresar();
    }

    private void configurarCampos() {
        enlace.campoContrasena.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                intentarLogin();
                return true;
            }
            return false;
        });
    }

    private void configurarBotonIngresar() {
        enlace.botonIngresar.setOnClickListener(v -> intentarLogin());
    }

    private void intentarLogin() {
        String email = enlace.campoEmail.getText().toString().trim();
        String contrasena = enlace.campoContrasena.getText().toString().trim();

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

        mostrarCargando(true);
        Log.d(TAG, "Intentando login con email: " + email);

        Map<String, String> credenciales = new HashMap<>();
        credenciales.put("email", email);
        credenciales.put("password", contrasena);

        SupabaseCliente.obtenerServicioAuth()
                .iniciarSesion(credenciales)
                .enqueue(new Callback<SupabaseServicio.RespuestaAuth>() {

                    @Override
                    public void onResponse(
                            Call<SupabaseServicio.RespuestaAuth> llamada,
                            Response<SupabaseServicio.RespuestaAuth> respuesta) {

                        mostrarCargando(false);

                        Log.d(TAG, "Respuesta recibida. Código HTTP: "
                                + respuesta.code());

                        if (respuesta.isSuccessful() && respuesta.body() != null) {
                            Log.d(TAG, "Login exitoso. Token recibido.");
                            SupabaseServicio.RespuestaAuth auth = respuesta.body();
                            sesionManager.guardarSesion(
                                    auth.tokenAcceso,
                                    1,
                                    1,
                                    "Nelson",
                                    email
                            );
                            irAActividadPrincipal();
                        } else {
                            // Loguear el cuerpo del error de Supabase
                            try {
                                String errorBody = respuesta.errorBody() != null
                                        ? respuesta.errorBody().string()
                                        : "sin cuerpo de error";
                                Log.e(TAG, "Error HTTP " + respuesta.code()
                                        + " — " + errorBody);
                            } catch (Exception e) {
                                Log.e(TAG, "No se pudo leer el error: "
                                        + e.getMessage());
                            }
                            mostrarError(getString(R.string.error_credenciales));
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<SupabaseServicio.RespuestaAuth> llamada,
                            Throwable error) {

                        mostrarCargando(false);
                        // Este log nos dirá exactamente qué falló
                        Log.e(TAG, "onFailure — Error de red: "
                                + error.getClass().getSimpleName()
                                + " — " + error.getMessage(), error);
                        mostrarError(getString(R.string.error_sin_internet));
                    }
                });
    }

    private void irAActividadPrincipal() {
        startActivity(new Intent(this, ActividadPrincipal.class));
        finish();
    }

    private void mostrarCargando(boolean cargando) {
        enlace.contenedorCargando.setVisibility(
                cargando ? View.VISIBLE : View.GONE);
        enlace.botonIngresar.setEnabled(!cargando);
        enlace.campoEmail.setEnabled(!cargando);
        enlace.campoContrasena.setEnabled(!cargando);
    }

    private void mostrarError(String mensaje) {
        Snackbar.make(enlace.getRoot(), mensaje, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getColor(R.color.estado_cancelado_fondo))
                .setTextColor(getColor(R.color.estado_cancelado_texto))
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        enlace = null;
    }
}