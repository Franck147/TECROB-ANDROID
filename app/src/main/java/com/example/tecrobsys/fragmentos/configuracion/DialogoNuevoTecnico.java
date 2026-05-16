package com.example.tecrobsys.fragmentos.configuracion;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.example.tecrobsys.R;
import com.example.tecrobsys.databinding.DialogoNuevoTecnicoBinding;

public class DialogoNuevoTecnico extends BottomSheetDialogFragment {

    private static final String ARG_EMPRESA_ID = "empresa_id";

    public interface OnGuardarListener {
        void onGuardar(String nombre, String apellido, String email,
                       String password, String rol);
    }

    private DialogoNuevoTecnicoBinding enlace;
    private OnGuardarListener listener;

    public static DialogoNuevoTecnico nuevaInstancia(int empresaId) {
        DialogoNuevoTecnico d = new DialogoNuevoTecnico();
        Bundle args = new Bundle();
        args.putInt(ARG_EMPRESA_ID, empresaId);
        d.setArguments(args);
        return d;
    }

    public void setOnGuardarListener(OnGuardarListener l) { this.listener = l; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador,
                             @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        enlace = DialogoNuevoTecnicoBinding.inflate(inflador, contenedor, false);
        return enlace.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle estado) {
        super.onViewCreated(vista, estado);
        enlace.chipRolTecnico.setChecked(true);
        enlace.botonGuardarTecnico.setOnClickListener(v -> { if (validar()) enviar(); });
        enlace.botonCancelarTecnico.setOnClickListener(v -> dismiss());
    }

    private boolean validar() {
        boolean ok = true;
        String nombre   = enlace.campoNombreTecnico.getText().toString().trim();
        String email    = enlace.campoEmailTecnico.getText().toString().trim();
        String password = enlace.campoContrasenaTecnico.getText().toString();

        if (nombre.isEmpty()) {
            enlace.campoNombreTecnico.setError(getString(R.string.error_nombre_vacio));
            ok = false;
        } else { enlace.campoNombreTecnico.setError(null); }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            enlace.campoEmailTecnico.setError("Correo inválido");
            ok = false;
        } else { enlace.campoEmailTecnico.setError(null); }

        if (password.length() < 6) {
            enlace.campoContrasenaTecnico.setError(getString(R.string.error_password_corta));
            ok = false;
        } else { enlace.campoContrasenaTecnico.setError(null); }

        return ok;
    }

    private void enviar() {
        if (listener == null) return;
        String rol = enlace.chipRolAdmin.isChecked() ? "administrador" : "tecnico";
        listener.onGuardar(
                enlace.campoNombreTecnico.getText().toString().trim(),
                enlace.campoApellidoTecnico.getText().toString().trim(),
                enlace.campoEmailTecnico.getText().toString().trim(),
                enlace.campoContrasenaTecnico.getText().toString(),
                rol);
        dismiss();
    }

    public void mostrarCargando(boolean cargando) {
        if (enlace == null) return;
        enlace.progressGuardandoTecnico.setVisibility(cargando ? View.VISIBLE : View.GONE);
        enlace.botonGuardarTecnico.setEnabled(!cargando);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        enlace = null;
    }
}
