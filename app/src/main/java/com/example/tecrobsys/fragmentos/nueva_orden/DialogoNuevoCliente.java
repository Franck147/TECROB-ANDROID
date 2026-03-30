package com.example.tecrobsys.fragmentos.nueva_orden;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.example.tecrobsys.R;
import com.example.tecrobsys.databinding.DialogoNuevoClienteBinding;
import com.example.tecrobsys.modelos.Cliente;
import com.example.tecrobsys.red.SupabaseCliente;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DialogoNuevoCliente extends BottomSheetDialogFragment {

    private static final String TAG = "TECROB_CLIENTE";

    public interface OnClienteCreadoListener {
        void alCrearCliente(Cliente cliente);
    }

    private DialogoNuevoClienteBinding enlace;
    private final int empresaId;
    private final OnClienteCreadoListener callback;

    // Datos del formulario guardados antes del request
    private String nombreIngresado;
    private String apellidoIngresado;
    private String telefonoIngresado;

    public DialogoNuevoCliente(int empresaId, OnClienteCreadoListener callback) {
        this.empresaId = empresaId;
        this.callback  = callback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador,
                             @Nullable ViewGroup contenedor,
                             @Nullable Bundle estadoGuardado) {
        enlace = DialogoNuevoClienteBinding.inflate(inflador, contenedor, false);
        return enlace.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View vista,
                              @Nullable Bundle estadoGuardado) {
        super.onViewCreated(vista, estadoGuardado);
        enlace.botonGuardarCliente.setOnClickListener(v -> {
            if (validar()) guardarCliente();
        });
        enlace.botonCancelarCliente.setOnClickListener(v -> dismiss());
    }

    private boolean validar() {
        boolean valido = true;

        if (enlace.campoNombreCliente.getText().toString().trim().isEmpty()) {
            enlace.campoNombreCliente.setError(getString(R.string.error_nombre_vacio));
            valido = false;
        } else {
            enlace.campoNombreCliente.setError(null);
        }

        if (enlace.campoTelefonoCliente.getText().toString().trim().isEmpty()) {
            enlace.layoutTelefonoCliente.setError(getString(R.string.error_telefono_vacio));
            valido = false;
        } else {
            enlace.layoutTelefonoCliente.setError(null);
        }

        return valido;
    }

    private void guardarCliente() {
        enlace.botonGuardarCliente.setEnabled(false);
        enlace.progressGuardandoCliente.setVisibility(View.VISIBLE);

        // Guardar valores antes del request para usarlos si body() es null
        nombreIngresado   = enlace.campoNombreCliente.getText().toString().trim();
        apellidoIngresado = enlace.campoApellidoCliente.getText().toString().trim();
        telefonoIngresado = enlace.campoTelefonoCliente.getText().toString().trim();

        Map<String, Object> datos = new HashMap<>();
        datos.put("empresa_id", empresaId);
        datos.put("nombre",    nombreIngresado);
        datos.put("apellido",  apellidoIngresado);
        datos.put("telefono",  telefonoIngresado);
        datos.put("dni",       enlace.campoDniCliente.getText().toString().trim());
        datos.put("email",     enlace.campoEmailCliente.getText().toString().trim());
        datos.put("direccion", enlace.campoDireccionCliente.getText().toString().trim());

        Log.d(TAG, "Guardando cliente: " + nombreIngresado);

        SupabaseCliente.obtenerServicio()
                .crearCliente(datos)
                .enqueue(new Callback<Cliente>() {
                    @Override
                    public void onResponse(@NonNull Call<Cliente> c,
                                           @NonNull Response<Cliente> r) {
                        if (enlace == null) return;
                        enlace.progressGuardandoCliente.setVisibility(View.GONE);
                        enlace.botonGuardarCliente.setEnabled(true);

                        Log.d(TAG, "Respuesta HTTP: " + r.code());

                        // Supabase devuelve 201 al insertar exitosamente
                        // body() puede ser null aunque el insert fue exitoso
                        if (r.code() == 201 || r.isSuccessful()) {
                            Cliente clienteCreado;

                            if (r.body() != null) {
                                clienteCreado = r.body();
                                Log.d(TAG, "Cliente creado con ID: " + clienteCreado.getId());
                            } else {
                                // Insert exitoso pero sin objeto de retorno
                                // Construimos cliente temporal con datos del formulario
                                Log.d(TAG, "Insert OK pero body null — usando datos del form");
                                clienteCreado = new Cliente();
                                clienteCreado.setNombre(nombreIngresado);
                                clienteCreado.setApellido(apellidoIngresado);
                                clienteCreado.setTelefono(telefonoIngresado);
                            }

                            callback.alCrearCliente(clienteCreado);
                            dismiss();

                        } else {
                            try {
                                String errorBody = r.errorBody() != null
                                        ? r.errorBody().string() : "sin error";
                                Log.e(TAG, "Error HTTP " + r.code() + ": " + errorBody);
                            } catch (Exception e) {
                                Log.e(TAG, "No se pudo leer error: " + e.getMessage());
                            }
                            Snackbar.make(enlace.getRoot(),
                                    getString(R.string.error_servidor),
                                    Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Cliente> c,
                                          @NonNull Throwable e) {
                        if (enlace == null) return;
                        enlace.progressGuardandoCliente.setVisibility(View.GONE);
                        enlace.botonGuardarCliente.setEnabled(true);
                        Log.e(TAG, "onFailure: " + e.getClass().getSimpleName()
                                + " — " + e.getMessage(), e);
                        Snackbar.make(enlace.getRoot(),
                                getString(R.string.error_sin_internet),
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        enlace = null;
    }
}