package com.example.tecrobsys.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import com.example.tecrobsys.R;

/**
 * MensajeUtils — Muestra mensajes flotantes centrados en pantalla.
 * Reemplaza Snackbar para notificaciones de éxito y error.
 * Se auto-descarta después del tiempo indicado.
 */
public class MensajeUtils {

    public static void mostrar(Context context, String mensaje) {
        mostrar(context, mensaje, 2200);
    }

    public static void mostrar(Context context, String mensaje, int duracionMs) {
        if (context == null || mensaje == null) return;
        try {
            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            View vista = LayoutInflater.from(context)
                    .inflate(R.layout.layout_mensaje_centro, null);
            dialog.setContentView(vista);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setGravity(Gravity.CENTER);
            ((TextView) vista.findViewById(R.id.text_mensaje_centro)).setText(mensaje);
            dialog.setCancelable(true);
            dialog.show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (dialog.isShowing()) dialog.dismiss();
            }, duracionMs);
        } catch (Exception ignored) {}
    }
}
