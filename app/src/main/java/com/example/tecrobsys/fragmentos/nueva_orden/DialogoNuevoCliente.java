package com.example.tecrobsys.fragmentos.nueva_orden;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.example.tecrobsys.utils.MensajeUtils;
import com.example.tecrobsys.R;
import com.example.tecrobsys.databinding.DialogoNuevoClienteBinding;
import com.example.tecrobsys.modelos.Cliente;
import com.example.tecrobsys.viewmodels.NuevoClienteViewModel;
import java.util.HashMap;
import java.util.Map;

/**
 * DialogoNuevoCliente — BottomSheet para registrar un nuevo cliente.
 *
 * Patrón de comunicación con el fragmento padre:
 *   FragmentoNuevaOrden implements OnClienteCreadoListener
 *   → este diálogo llama getParentFragment() para notificar al padre
 *
 * El empresaId se pasa via setArguments() (patrón correcto para Fragments).
 *
 * Uso:
 *   DialogoNuevoCliente d = DialogoNuevoCliente.nuevaInstancia(empresaId);
 *   d.show(getParentFragmentManager(), "nuevo_cliente");
 */
public class DialogoNuevoCliente extends BottomSheetDialogFragment {

    private static final String ARG_EMPRESA_ID = "empresa_id";

    /** El fragmento padre debe implementar esta interfaz para recibir el cliente creado. */
    public interface OnClienteCreadoListener {
        void alCrearCliente(Cliente cliente);
    }

    private DialogoNuevoClienteBinding enlace;
    private NuevoClienteViewModel viewModel;

    /** Método fábrica — forma correcta de instanciar este fragmento. */
    public static DialogoNuevoCliente nuevaInstancia(int empresaId) {
        DialogoNuevoCliente dialogo = new DialogoNuevoCliente();
        Bundle args = new Bundle();
        args.putInt(ARG_EMPRESA_ID, empresaId);
        dialogo.setArguments(args);
        return dialogo;
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

        viewModel = new ViewModelProvider(this).get(NuevoClienteViewModel.class);

        observarViewModel();
        configurarBotones();
    }

    private void observarViewModel() {
        viewModel.guardando.observe(getViewLifecycleOwner(), g -> {
            boolean guardando = Boolean.TRUE.equals(g);
            enlace.progressGuardandoCliente.setVisibility(
                    guardando ? View.VISIBLE : View.GONE);
            enlace.botonGuardarCliente.setEnabled(!guardando);
        });

        viewModel.clienteCreado.observe(getViewLifecycleOwner(), cliente -> {
            if (cliente != null) {
                // Notificar al fragmento padre vía interfaz
                if (getParentFragment() instanceof OnClienteCreadoListener) {
                    ((OnClienteCreadoListener) getParentFragment()).alCrearCliente(cliente);
                }
                dismiss();
                viewModel.clienteCreado.setValue(null);
            }
        });

        viewModel.error.observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                MensajeUtils.mostrar(requireContext(), err);
                viewModel.error.setValue(null);
            }
        });
    }

    private void configurarBotones() {
        enlace.botonGuardarCliente.setOnClickListener(v -> {
            if (validar()) guardar();
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

    private void guardar() {
        int empresaId = getArguments() != null
                ? getArguments().getInt(ARG_EMPRESA_ID, 1) : 1;

        Map<String, Object> datos = new HashMap<>();
        datos.put("empresa_id", empresaId);
        datos.put("nombre",    enlace.campoNombreCliente.getText().toString().trim());
        datos.put("apellido",  enlace.campoApellidoCliente.getText().toString().trim());
        datos.put("telefono",  enlace.campoTelefonoCliente.getText().toString().trim());
        datos.put("dni",       enlace.campoDniCliente.getText().toString().trim());
        datos.put("email",     enlace.campoEmailCliente.getText().toString().trim());
        datos.put("direccion", enlace.campoDireccionCliente.getText().toString().trim());

        viewModel.crearCliente(datos);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        enlace = null;
    }
}
