package com.example.tecrobsys.fragmentos.nueva_orden;

import android.os.Bundle;
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

/**
 * DialogoNuevoCliente — BottomSheet para registrar un cliente nuevo.
 *
 * Se abre desde FragmentoNuevaOrden cuando el usuario no encuentra
 * al cliente en la búsqueda.
 *
 * Flujo:
 *   1. Usuario llena nombre, teléfono y datos opcionales
 *   2. Al guardar: POST /cliente en Supabase
 *   3. Llama al callback con el cliente creado
 *   4. El fragmento padre lo selecciona automáticamente
 *
 * Usa BottomSheetDialogFragment de Material 3 para un look moderno
 * que aparece desde la parte inferior de la pantalla.
 */
public class DialogoNuevoCliente extends BottomSheetDialogFragment {

    // Callback para devolver el cliente creado al fragmento padre
    public interface OnClienteCreadoListener {
        void alCrearCliente(Cliente cliente);
    }

    private DialogoNuevoClienteBinding enlace;
    private final int empresaId;
    private final OnClienteCreadoListener callback;

    /**
     * Constructor con los datos necesarios.
     * @param empresaId ID de la empresa para asignar el cliente
     * @param callback  Se llama con el cliente creado al guardar
     */
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

        // Botón guardar
        enlace.botonGuardarCliente.setOnClickListener(v -> {
            if (validar()) guardarCliente();
        });

        // Botón cancelar — cerrar el diálogo
        enlace.botonCancelarCliente.setOnClickListener(v -> dismiss());
    }

    /**
     * Valida que los campos obligatorios estén llenos.
     * @return true si el formulario es válido
     */
    private boolean validar() {
        boolean valido = true;

        // Nombre es obligatorio
        if (enlace.campoNombreCliente.getText().toString().trim().isEmpty()) {
            enlace.campoNombreCliente.setError(
                    getString(R.string.error_nombre_vacio));
            valido = false;
        } else {
            enlace.campoNombreCliente.setError(null);
        }

        // Teléfono es obligatorio
        if (enlace.campoTelefonoCliente.getText().toString().trim().isEmpty()) {
            enlace.layoutTelefonoCliente.setError(
                    getString(R.string.error_telefono_vacio));
            valido = false;
        } else {
            enlace.layoutTelefonoCliente.setError(null);
        }

        return valido;
    }

    /**
     * Guarda el cliente en Supabase.
     * POST /cliente con los datos del formulario.
     */
    private void guardarCliente() {
        enlace.botonGuardarCliente.setEnabled(false);
        enlace.progressGuardandoCliente.setVisibility(View.VISIBLE);

        // Construir el mapa de datos para Supabase
        Map<String, Object> datos = new HashMap<>();
        datos.put("empresa_id", empresaId);
        datos.put("nombre",
                enlace.campoNombreCliente.getText().toString().trim());
        datos.put("apellido",
                enlace.campoApellidoCliente.getText().toString().trim());
        datos.put("telefono",
                enlace.campoTelefonoCliente.getText().toString().trim());
        datos.put("dni",
                enlace.campoDniCliente.getText().toString().trim());
        datos.put("email",
                enlace.campoEmailCliente.getText().toString().trim());
        datos.put("direccion",
                enlace.campoDireccionCliente.getText().toString().trim());

        SupabaseCliente.obtenerServicio()
                .crearCliente(datos)
                .enqueue(new Callback<Cliente>() {
                    @Override
                    public void onResponse(@NonNull Call<Cliente> c,
                                           @NonNull Response<Cliente> r) {
                        if (enlace == null) return;
                        enlace.progressGuardandoCliente.setVisibility(View.GONE);
                        enlace.botonGuardarCliente.setEnabled(true);

                        if (r.isSuccessful() && r.body() != null) {
                            // Notificar al fragmento padre con el cliente creado
                            callback.alCrearCliente(r.body());
                            // Cerrar el diálogo
                            dismiss();
                        } else {
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