package com.example.tecrobsys.actividades;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.tecrobsys.utils.MensajeUtils;
import com.example.tecrobsys.databinding.ActivityLoginBinding;
import com.example.tecrobsys.viewmodels.LoginViewModel;
import com.example.tecrobsys.R;

/**
 * ActividadLogin — Primera pantalla: login con Supabase Auth.
 *
 * Flujo (manejado por LoginViewModel):
 *   1. Usuario ingresa email + contraseña
 *   2. ViewModel autentica con Supabase Auth → token JWT
 *   3. ViewModel obtiene perfil del técnico (id real, nombre, rol)
 *   4. SesionManager guarda todo en EncryptedSharedPreferences
 *   5. Activity observa perfilTecnico → navega a ActividadPrincipal
 */
public class ActividadLogin extends AppCompatActivity {

    private ActivityLoginBinding enlace;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle estadoGuardado) {
        super.onCreate(estadoGuardado);
        enlace = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(enlace.getRoot());

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Si ya hay sesión activa, saltar directamente al dashboard
        com.example.tecrobsys.utils.SesionManager sesion =
                com.example.tecrobsys.utils.SesionManager.obtenerInstancia(this);
        if (sesion.estaAutenticado()) {
            irAActividadPrincipal();
            return;
        }

        configurarCampos();
        configurarBotonIngresar();
        observarViewModel();
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

    private void observarViewModel() {
        viewModel.cargando.observe(this, c -> mostrarCargando(Boolean.TRUE.equals(c)));

        viewModel.error.observe(this, err -> {
            if (err != null && !err.isEmpty()) {
                mostrarError(err);
                viewModel.limpiarError();
            }
        });

        // Cuando el perfil llega → navegar a la pantalla principal
        viewModel.perfilTecnico.observe(this, perfil -> {
            if (perfil != null) irAActividadPrincipal();
        });
    }

    private void intentarLogin() {
        String email     = enlace.campoEmail.getText().toString().trim();
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

        viewModel.iniciarSesion(email, contrasena);
    }

    private void irAActividadPrincipal() {
        startActivity(new Intent(this, ActividadPrincipal.class));
        finish();
    }

    private void mostrarCargando(boolean cargando) {
        enlace.contenedorCargando.setVisibility(cargando ? View.VISIBLE : View.GONE);
        enlace.botonIngresar.setEnabled(!cargando);
        enlace.campoEmail.setEnabled(!cargando);
        enlace.campoContrasena.setEnabled(!cargando);
    }

    private void mostrarError(String mensaje) {
        MensajeUtils.mostrar(this, mensaje);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        enlace = null;
    }
}
