package com.example.tecrobsys.fragmentos.ordenes;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.example.tecrobsys.R;
import com.example.tecrobsys.databinding.FragmentoDetalleOrdenBinding;
import com.example.tecrobsys.modelos.Orden;
import com.example.tecrobsys.red.SupabaseCliente;
import com.example.tecrobsys.utils.UtilEstado;
import com.example.tecrobsys.utils.UtilFecha;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * FragmentoDetalleOrden — Pantalla de detalle de una orden.
 *
 * Funcionalidades:
 *   - Ver todos los datos de la orden (cliente, equipo, totales)
 *   - Cambiar el estado tocando el chip de estado
 *   - Llamar directamente al cliente
 *   - Abrir WhatsApp con mensaje predefinido
 *   - Enviar PDF por WhatsApp
 *   - Marcar como completada y enviar aviso automático
 *
 * Se crea con el método fábrica nuevaInstancia(ordenId).
 * Nunca usar el constructor con parámetros en Fragments.
 */
public class FragmentoDetalleOrden extends Fragment {

    // Clave del argumento para el ID de la orden
    private static final String ARG_ORDEN_ID = "orden_id";

    private FragmentoDetalleOrdenBinding enlace;
    private int ordenId;
    private Orden ordenActual;

    /**
     * Método fábrica — forma correcta de crear este fragmento.
     * Los argumentos sobreviven a rotaciones de pantalla.
     *
     * Uso: FragmentoDetalleOrden.nuevaInstancia(41)
     */
    public static FragmentoDetalleOrden nuevaInstancia(int ordenId) {
        FragmentoDetalleOrden fragmento = new FragmentoDetalleOrden();
        Bundle args = new Bundle();
        args.putInt(ARG_ORDEN_ID, ordenId);
        fragmento.setArguments(args);
        return fragmento;
    }

    @Override
    public void onCreate(@Nullable Bundle estadoGuardado) {
        super.onCreate(estadoGuardado);
        if (getArguments() != null) {
            ordenId = getArguments().getInt(ARG_ORDEN_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador,
                             @Nullable ViewGroup contenedor,
                             @Nullable Bundle estadoGuardado) {
        enlace = FragmentoDetalleOrdenBinding.inflate(inflador, contenedor, false);
        return enlace.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View vista,
                              @Nullable Bundle estadoGuardado) {
        super.onViewCreated(vista, estadoGuardado);

        // Botón volver
        enlace.botonVolver.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        cargarOrden();
    }

    /**
     * Carga la orden completa desde Supabase con un solo request.
     * Incluye cliente, técnico y equipo via JOIN de Supabase.
     */
    private void cargarOrden() {
        enlace.progressCargando.setVisibility(View.VISIBLE);
        enlace.contenidoOrden.setVisibility(View.GONE);

        String seleccion = "*,"
                + "cliente:cliente_id(nombre,apellido,telefono,email,dni),"
                + "tecnico:tecnico_id(nombre,apellido),"
                + "equipo(*)";

        SupabaseCliente.obtenerServicio()
                .obtenerOrdenPorId("eq." + ordenId, seleccion)
                .enqueue(new Callback<Orden>() {
                    @Override
                    public void onResponse(@NonNull Call<Orden> c,
                                           @NonNull Response<Orden> r) {
                        if (enlace == null) return;
                        enlace.progressCargando.setVisibility(View.GONE);

                        if (r.isSuccessful() && r.body() != null) {
                            ordenActual = r.body();
                            enlace.contenidoOrden.setVisibility(View.VISIBLE);
                            mostrarDatos();
                            configurarAcciones();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Orden> c,
                                          @NonNull Throwable e) {
                        if (enlace == null) return;
                        enlace.progressCargando.setVisibility(View.GONE);
                        mostrarSnackbar(getString(R.string.error_sin_internet));
                    }
                });
    }

    /**
     * Llena todas las vistas con los datos de la orden cargada.
     */
    private void mostrarDatos() {
        // ── Encabezado ────────────────────────────────────────────
        enlace.textNumeroOrden.setText("#" + ordenActual.getNumeroOrden());
        enlace.textFechaCreacion.setText(
                getString(R.string.lbl_creada_el) + " "
                        + UtilFecha.formatearFechaCorta(ordenActual.getCreadoEn()));

        // ── Estado con color dinámico ─────────────────────────────
        actualizarChipEstado(ordenActual.getEstado());

        // ── Cliente ───────────────────────────────────────────────
        if (ordenActual.getCliente() != null) {
            Orden.ClienteResumen cli = ordenActual.getCliente();
            enlace.textInicialesCliente.setText(cli.getIniciales());
            enlace.textNombreCliente.setText(cli.getNombreCompleto());
            enlace.textTelefonoCliente.setText(
                    cli.getTelefono() != null ? cli.getTelefono() : "—");
        }

        // ── Equipo ────────────────────────────────────────────────
        if (ordenActual.getEquipo() != null) {
            com.example.tecrobsys.modelos.Equipo eq = ordenActual.getEquipo();
            enlace.textTipoEquipo.setText(eq.getTipoFormateado());
            enlace.textMarcaModelo.setText(eq.getNombreCompleto());
            enlace.textSerie.setText(
                    eq.getNumeroSerie() != null ? eq.getNumeroSerie() : "—");
            enlace.textDesperfecto.setText(
                    eq.getDesperfecto() != null ? eq.getDesperfecto() : "—");
        }

        // ── Totales ───────────────────────────────────────────────
        enlace.textSubtotal.setText(
                String.format("S/ %.2f", ordenActual.getSubtotal()));
        enlace.textDescuento.setText(
                String.format("S/ %.2f", ordenActual.getDescuento()));
        enlace.textAdelanto.setText(
                String.format("- S/ %.2f", ordenActual.getAdelanto()));
        enlace.textTotalCobrar.setText(
                String.format("S/ %.2f", ordenActual.getSaldoPendiente()));
    }

    /**
     * Actualiza el chip de estado con el color correcto.
     */
    private void actualizarChipEstado(String estado) {
        enlace.chipEstado.setText(UtilEstado.obtenerTexto(estado));
        enlace.chipEstado.setChipBackgroundColor(
                ColorStateList.valueOf(ContextCompat.getColor(
                        requireContext(), UtilEstado.obtenerColorFondo(estado))));
        enlace.chipEstado.setTextColor(ContextCompat.getColor(
                requireContext(), UtilEstado.obtenerColorTexto(estado)));
    }

    /**
     * Configura todos los botones de acción.
     */
    private void configurarAcciones() {
        String telefono = ordenActual.getCliente() != null
                ? ordenActual.getCliente().getTelefono() : "";

        // ── Llamar al cliente ─────────────────────────────────────
        enlace.botonLlamar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL,
                    Uri.parse("tel:" + telefono));
            startActivity(intent);
        });

        // ── Abrir WhatsApp ────────────────────────────────────────
        enlace.botonWhatsapp.setOnClickListener(v ->
                abrirWhatsApp(telefono, ""));

        // ── Enviar PDF por WhatsApp ───────────────────────────────
        enlace.botonPdf.setOnClickListener(v -> {
            String mensaje = String.format(
                    getString(R.string.msg_wa_orden_recibida),
                    ordenActual.getCliente() != null
                            ? ordenActual.getCliente().getNombreCompleto() : "",
                    ordenActual.getEquipo() != null
                            ? ordenActual.getEquipo().getNombreCompleto() : "su equipo",
                    ordenActual.getNumeroOrden());
            abrirWhatsApp(telefono, mensaje);
        });

        // ── Avisar que está listo ─────────────────────────────────
        enlace.botonAvisarListo.setOnClickListener(v ->
                mostrarDialogoCompletada());

        // ── Cambiar estado tocando el chip ────────────────────────
        enlace.chipEstado.setOnClickListener(v -> mostrarSelectorEstado());

        // ── Botón principal: marcar completada ────────────────────
        enlace.botonMarcarCompletada.setOnClickListener(v ->
                mostrarDialogoCompletada());
    }

    /**
     * Abre WhatsApp con el número y mensaje indicados.
     * Si WhatsApp no está instalado abre el navegador.
     */
    private void abrirWhatsApp(String telefono, String mensaje) {
        // Limpiar el número: quitar espacios, guiones, paréntesis
        String numero = telefono.replaceAll("[^\\d+]", "");
        try {
            String url = "https://api.whatsapp.com/send?phone=" + numero;
            if (!mensaje.isEmpty()) {
                url += "&text=" + Uri.encode(mensaje);
            }
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            mostrarSnackbar(getString(R.string.msg_whatsapp_no_disponible));
        }
    }

    /**
     * Muestra el diálogo de confirmación antes de marcar como completada.
     * Incluye la vista previa del mensaje de WhatsApp.
     */
    private void mostrarDialogoCompletada() {
        String nombre = ordenActual.getCliente() != null
                ? ordenActual.getCliente().getNombreCompleto() : "cliente";
        String equipo = ordenActual.getEquipo() != null
                ? ordenActual.getEquipo().getNombreCompleto() : "su equipo";
        String numero = ordenActual.getNumeroOrden();

        String mensajeWa = String.format(
                getString(R.string.msg_wa_orden_lista), nombre, equipo, numero);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialogo_marcar_lista_titulo))
                .setMessage(mensajeWa)
                .setPositiveButton(getString(R.string.btn_confirmar), (d, w) -> {
                    actualizarEstado("listo");
                    abrirWhatsApp(
                            ordenActual.getCliente() != null
                                    ? ordenActual.getCliente().getTelefono() : "",
                            mensajeWa);
                })
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .show();
    }

    /**
     * Muestra el diálogo selector de estados.
     */
    private void mostrarSelectorEstado() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialogo_cambiar_estado_titulo))
                .setItems(UtilEstado.obtenerEtiquetasEstados(), (d, indice) -> {
                    String nuevoEstado = UtilEstado.obtenerTodosLosEstados()[indice];
                    actualizarEstado(nuevoEstado);
                })
                .show();
    }

    /**
     * Actualiza el estado de la orden en Supabase.
     */
    private void actualizarEstado(String nuevoEstado) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("estado", nuevoEstado);

        SupabaseCliente.obtenerServicio()
                .actualizarOrden("eq." + ordenId, datos)
                .enqueue(new Callback<Orden>() {
                    @Override
                    public void onResponse(@NonNull Call<Orden> c,
                                           @NonNull Response<Orden> r) {
                        if (enlace == null) return;
                        if (r.isSuccessful()) {
                            ordenActual.setEstado(nuevoEstado);
                            actualizarChipEstado(nuevoEstado);
                            mostrarSnackbar(
                                    getString(R.string.msg_estado_actualizado));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Orden> c,
                                          @NonNull Throwable e) {
                        if (enlace != null)
                            mostrarSnackbar(getString(R.string.msg_error_estado));
                    }
                });
    }

    /** Muestra un Snackbar con el mensaje indicado. */
    private void mostrarSnackbar(String mensaje) {
        Snackbar.make(enlace.getRoot(), mensaje, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        enlace = null;
    }
}