package com.example.tecrobsys.fragmentos.configuracion;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.tecrobsys.R;
import com.example.tecrobsys.actividades.ActividadLogin;
import com.example.tecrobsys.databinding.FragmentoConfiguracionBinding;
import com.example.tecrobsys.modelos.Tecnico;
import com.example.tecrobsys.utils.MensajeUtils;
import com.example.tecrobsys.utils.SesionManager;
import com.example.tecrobsys.viewmodels.ConfiguracionViewModel;

public class FragmentoConfiguracion extends Fragment {

    private FragmentoConfiguracionBinding enlace;
    private ConfiguracionViewModel viewModel;
    private TecnicoAdapter adapter;
    private int miTecnicoId;
    private int empresaId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador,
                             @Nullable ViewGroup contenedor,
                             @Nullable Bundle estadoGuardado) {
        enlace = FragmentoConfiguracionBinding.inflate(inflador, contenedor, false);
        return enlace.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle estadoGuardado) {
        super.onViewCreated(vista, estadoGuardado);

        viewModel = new ViewModelProvider(this).get(ConfiguracionViewModel.class);
        SesionManager sesion = SesionManager.obtenerInstancia(requireContext());

        String nombre   = sesion.obtenerNombreTecnico();
        String email    = sesion.obtenerEmail();
        boolean esAdmin = sesion.esAdministrador();
        miTecnicoId     = sesion.obtenerTecnicoId();
        empresaId       = sesion.obtenerEmpresaId();

        String inicial = (nombre != null && !nombre.isEmpty())
                ? nombre.substring(0, 1).toUpperCase() : "?";
        enlace.textAvatar.setText(inicial);
        enlace.textNombre.setText(nombre);
        enlace.textEmail.setText(email);
        enlace.textRol.setText(esAdmin
                ? getString(R.string.lbl_rol_admin)
                : getString(R.string.lbl_rol_tecnico));

        enlace.botonCerrarSesion.setOnClickListener(v -> confirmarCierreSesion());

        if (esAdmin) {
            configurarSeccionAdmin();
        }
    }

    private void configurarSeccionAdmin() {
        enlace.seccionTecnicos.setVisibility(View.VISIBLE);

        adapter = new TecnicoAdapter(tecnico -> confirmarEliminar(tecnico));
        enlace.listaTecnicos.setLayoutManager(new LinearLayoutManager(requireContext()));
        enlace.listaTecnicos.setAdapter(adapter);

        enlace.botonAgregarTecnico.setOnClickListener(v -> mostrarDialogoNuevoTecnico());

        observarViewModel();
        viewModel.cargarTecnicos(empresaId);
    }

    private void observarViewModel() {
        viewModel.tecnicos.observe(getViewLifecycleOwner(), lista -> {
            if (lista != null) adapter.actualizar(lista);
        });

        viewModel.cargando.observe(getViewLifecycleOwner(), cargando -> {
            // La lista muestra sus propios estados; no hay ProgressBar global aquí
        });

        viewModel.creado.observe(getViewLifecycleOwner(), tecnico -> {
            if (tecnico != null) {
                adapter.agregar(tecnico);
                MensajeUtils.mostrar(requireContext(), getString(R.string.msg_tecnico_creado));
                viewModel.creado.setValue(null);
            }
        });

        viewModel.eliminado.observe(getViewLifecycleOwner(), ok -> {
            if (Boolean.TRUE.equals(ok)) {
                MensajeUtils.mostrar(requireContext(), getString(R.string.msg_tecnico_eliminado));
                viewModel.eliminado.setValue(null);
            }
        });

        viewModel.error.observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                MensajeUtils.mostrar(requireContext(), err);
                viewModel.error.setValue(null);
            }
        });
    }

    private void mostrarDialogoNuevoTecnico() {
        DialogoNuevoTecnico dialogo = DialogoNuevoTecnico.nuevaInstancia(empresaId);
        dialogo.setOnGuardarListener((nombre, apellido, email, password, rol) ->
                viewModel.crearTecnico(empresaId, nombre, apellido, email, password, rol));
        dialogo.show(getParentFragmentManager(), "nuevo_tecnico");
    }

    private void confirmarEliminar(Tecnico tecnico) {
        if (tecnico.getId() == miTecnicoId) {
            MensajeUtils.mostrar(requireContext(), getString(R.string.error_no_eliminarse));
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.lbl_confirmar_eliminar)
                .setMessage(tecnico.getNombreCompleto() + "\n" +
                        getString(R.string.msg_confirmar_eliminar))
                .setPositiveButton(R.string.btn_eliminar, (d, w) -> {
                    adapter.eliminar(tecnico.getId());
                    viewModel.eliminarTecnico(tecnico.getId());
                })
                .setNegativeButton(R.string.btn_cancelar, null)
                .show();
    }

    private void confirmarCierreSesion() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.lbl_confirmar_cierre)
                .setMessage(R.string.msg_confirmar_cierre)
                .setPositiveButton(R.string.btn_cerrar_sesion, (d, w) -> ejecutarCierreSesion())
                .setNegativeButton(R.string.btn_cancelar, null)
                .show();
    }

    private void ejecutarCierreSesion() {
        SesionManager.obtenerInstancia(requireContext()).cerrarSesion();
        Intent intent = new Intent(requireContext(), ActividadLogin.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        enlace = null;
    }
}
