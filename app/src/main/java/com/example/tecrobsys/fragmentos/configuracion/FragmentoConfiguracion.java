package com.example.tecrobsys.fragmentos.configuracion;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.tecrobsys.R;
import com.example.tecrobsys.actividades.ActividadLogin;
import com.example.tecrobsys.databinding.FragmentoConfiguracionBinding;
import com.example.tecrobsys.utils.SesionManager;

public class FragmentoConfiguracion extends Fragment {

    private FragmentoConfiguracionBinding enlace;

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

        SesionManager sesion = SesionManager.obtenerInstancia(requireContext());

        String nombre = sesion.obtenerNombreTecnico();
        String email  = sesion.obtenerEmail();
        boolean esAdmin = sesion.esAdministrador();

        // Inicial del nombre en el avatar
        String inicial = (nombre != null && !nombre.isEmpty())
                ? nombre.substring(0, 1).toUpperCase()
                : "?";
        enlace.textAvatar.setText(inicial);
        enlace.textNombre.setText(nombre);
        enlace.textEmail.setText(email);
        enlace.textRol.setText(esAdmin
                ? getString(R.string.lbl_rol_admin)
                : getString(R.string.lbl_rol_tecnico));

        enlace.botonCerrarSesion.setOnClickListener(v -> confirmarCierreSesion());
    }

    private void confirmarCierreSesion() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.lbl_confirmar_cierre)
                .setMessage(R.string.msg_confirmar_cierre)
                .setPositiveButton(R.string.btn_cerrar_sesion, (dialog, which) -> ejecutarCierreSesion())
                .setNegativeButton(R.string.btn_cancelar, null)
                .show();
    }

    private void ejecutarCierreSesion() {
        SesionManager.obtenerInstancia(requireContext()).cerrarSesion();
        Intent intent = new Intent(requireContext(), ActividadLogin.class);
        // Limpia el back stack completo: el usuario no puede volver con "atrás"
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        enlace = null;
    }
}
