package com.example.tecrobsys.fragmentos.ordenes;

import androidx.appcompat.app.AlertDialog;
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
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.tecrobsys.utils.MensajeUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.tecrobsys.R;
import com.example.tecrobsys.databinding.FragmentoDetalleOrdenBinding;
import com.example.tecrobsys.modelos.Orden;
import com.example.tecrobsys.utils.GeneradorPDF;
import com.example.tecrobsys.utils.UtilEstado;
import com.example.tecrobsys.utils.UtilFecha;
import com.example.tecrobsys.viewmodels.DetalleOrdenViewModel;

public class FragmentoDetalleOrden extends Fragment {

    private static final String ARG_ORDEN_ID = "orden_id";

    private FragmentoDetalleOrdenBinding enlace;
    private DetalleOrdenViewModel viewModel;
    private int ordenId;

    public static FragmentoDetalleOrden nuevaInstancia(int ordenId) {
        FragmentoDetalleOrden f = new FragmentoDetalleOrden();
        Bundle args = new Bundle();
        args.putInt(ARG_ORDEN_ID, ordenId);
        f.setArguments(args);
        return f;
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

        viewModel = new ViewModelProvider(this).get(DetalleOrdenViewModel.class);

        enlace.botonVolver.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        observarViewModel();
        viewModel.cargarOrden(ordenId);
    }

    private void observarViewModel() {
        viewModel.cargando.observe(getViewLifecycleOwner(), c -> {
            enlace.progressCargando.setVisibility(
                    Boolean.TRUE.equals(c) ? View.VISIBLE : View.GONE);
        });

        viewModel.orden.observe(getViewLifecycleOwner(), orden -> {
            if (orden != null) {
                enlace.contenidoOrden.setVisibility(View.VISIBLE);
                mostrarDatos(orden);
                configurarAcciones(orden);
            }
        });

        viewModel.estadoActualizado.observe(getViewLifecycleOwner(), ok -> {
            if (Boolean.TRUE.equals(ok)) {
                mostrarSnackbar(getString(R.string.msg_estado_actualizado));
                viewModel.estadoActualizado.setValue(null);
            }
        });

        viewModel.pagoRegistrado.observe(getViewLifecycleOwner(), ok -> {
            if (Boolean.TRUE.equals(ok)) {
                mostrarSnackbar(getString(R.string.msg_pago_registrado));
                // Refrescar los totales en pantalla
                Orden ordenActual = viewModel.orden.getValue();
                if (ordenActual != null) mostrarDatos(ordenActual);
                viewModel.pagoRegistrado.setValue(null);
            }
        });

        viewModel.error.observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                mostrarSnackbar(err);
                viewModel.error.setValue(null);
            }
        });
    }

    private void mostrarDatos(Orden orden) {
        enlace.textNumeroOrden.setText("#" + orden.getNumeroOrden());
        enlace.textFechaCreacion.setText(
                getString(R.string.lbl_creada_el) + " "
                        + UtilFecha.formatearFechaCorta(orden.getCreadoEn()));

        actualizarChipEstado(orden.getEstado());

        if (orden.getCliente() != null) {
            Orden.ClienteResumen cli = orden.getCliente();
            enlace.textInicialesCliente.setText(cli.getIniciales());
            enlace.textNombreCliente.setText(cli.getNombreCompleto());
            enlace.textTelefonoCliente.setText(
                    cli.getTelefono() != null ? cli.getTelefono() : "—");
        }

        if (orden.getEquipo() != null) {
            com.example.tecrobsys.modelos.Equipo eq = orden.getEquipo();
            enlace.textTipoEquipo.setText(eq.getTipoFormateado());
            enlace.textMarcaModelo.setText(eq.getNombreCompleto());
            enlace.textSerie.setText(eq.getNumeroSerie() != null ? eq.getNumeroSerie() : "—");
            enlace.textDesperfecto.setText(eq.getDesperfecto() != null ? eq.getDesperfecto() : "—");
        }

        enlace.textSubtotal.setText(String.format("S/ %.2f", orden.getSubtotal()));
        enlace.textDescuento.setText(String.format("S/ %.2f", orden.getDescuento()));
        enlace.textAdelanto.setText(String.format("- S/ %.2f", orden.getAdelanto()));
        enlace.textTotalCobrar.setText(String.format("S/ %.2f", orden.getSaldoPendiente()));
    }

    private void actualizarChipEstado(String estado) {
        enlace.chipEstado.setText(UtilEstado.obtenerTexto(estado));
        enlace.chipEstado.setChipBackgroundColor(
                ColorStateList.valueOf(ContextCompat.getColor(
                        requireContext(), UtilEstado.obtenerColorFondo(estado))));
        enlace.chipEstado.setTextColor(ContextCompat.getColor(
                requireContext(), UtilEstado.obtenerColorTexto(estado)));
    }

    private void configurarAcciones(Orden orden) {
        String telefono = orden.getCliente() != null
                ? orden.getCliente().getTelefono() : "";

        // Llamar al cliente
        enlace.botonLlamar.setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + telefono))));

        // Abrir WhatsApp
        enlace.botonWhatsapp.setOnClickListener(v -> abrirWhatsApp(telefono, ""));

        // Generar y compartir PDF (en hilo secundario para no bloquear la UI)
        enlace.botonPdf.setOnClickListener(v -> {
            Orden ordenActual = viewModel.orden.getValue();
            if (ordenActual == null) return;
            enlace.botonPdf.setEnabled(false);
            new Thread(() -> {
                Uri uri = GeneradorPDF.generarOrden(requireContext(), ordenActual);
                requireActivity().runOnUiThread(() -> {
                    enlace.botonPdf.setEnabled(true);
                    if (uri != null) {
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("application/pdf");
                        share.putExtra(Intent.EXTRA_STREAM, uri);
                        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(share, "Compartir orden PDF"));
                    } else {
                        mostrarSnackbar("No se pudo generar el PDF");
                    }
                });
            }).start();
        });

        // Avisar que está listo
        enlace.botonAvisarListo.setOnClickListener(v -> mostrarDialogoCompletada(orden));

        // Cambiar estado tocando el chip
        enlace.chipEstado.setOnClickListener(v -> mostrarSelectorEstado());

        // Botón principal: marcar completada
        enlace.botonMarcarCompletada.setOnClickListener(v -> mostrarDialogoCompletada(orden));

        // Registrar pago
        enlace.botonRegistrarPago.setOnClickListener(v -> mostrarDialogoPago());
    }

    private void abrirWhatsApp(String telefono, String mensaje) {
        String numero = telefono.replaceAll("[^\\d+]", "");
        try {
            String url = "https://api.whatsapp.com/send?phone=" + numero;
            if (!mensaje.isEmpty()) url += "&text=" + Uri.encode(mensaje);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            mostrarSnackbar(getString(R.string.msg_whatsapp_no_disponible));
        }
    }

    private void mostrarDialogoCompletada(Orden orden) {
        String nombre  = orden.getCliente() != null
                ? orden.getCliente().getNombreCompleto() : "cliente";
        String equipo  = orden.getEquipo()  != null
                ? orden.getEquipo().getNombreCompleto() : "su equipo";
        String numero  = orden.getNumeroOrden();
        String mensajeWa = String.format(
                getString(R.string.msg_wa_orden_lista), nombre, equipo, numero);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialogo_marcar_lista_titulo))
                .setMessage(mensajeWa)
                .setPositiveButton(getString(R.string.btn_confirmar), (d, w) -> {
                    viewModel.actualizarEstado(ordenId, "listo");
                    abrirWhatsApp(orden.getCliente() != null
                            ? orden.getCliente().getTelefono() : "", mensajeWa);
                })
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .show();
    }

    private void mostrarSelectorEstado() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialogo_cambiar_estado_titulo))
                .setItems(UtilEstado.obtenerEtiquetasEstados(), (d, indice) ->
                        viewModel.actualizarEstado(
                                ordenId,
                                UtilEstado.obtenerTodosLosEstados()[indice]))
                .show();
    }

    private void mostrarDialogoPago() {
        View vista = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialogo_registrar_pago, null);

        TextInputLayout layoutMonto  = vista.findViewById(R.id.layout_monto);
        TextInputEditText campMonto  = vista.findViewById(R.id.campo_monto);
        TextInputEditText campNota   = vista.findViewById(R.id.campo_nota_pago);
        ChipGroup chipMetodo         = vista.findViewById(R.id.chip_group_metodo);

        final String[] metodo = {"efectivo"};
        chipMetodo.setOnCheckedStateChangeListener((grupo, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if      (id == vista.findViewById(R.id.chip_efectivo).getId())     metodo[0] = "efectivo";
            else if (id == vista.findViewById(R.id.chip_yape).getId())         metodo[0] = "yape";
            else if (id == vista.findViewById(R.id.chip_plin).getId())         metodo[0] = "plin";
            else if (id == vista.findViewById(R.id.chip_transferencia).getId())metodo[0] = "transferencia";
            else                                                                metodo[0] = "tarjeta";
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.titulo_registrar_pago))
                .setView(vista)
                .setPositiveButton(getString(R.string.btn_registrar_pago), null)
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String montoStr = campMonto.getText() != null
                            ? campMonto.getText().toString().trim() : "";
                    if (montoStr.isEmpty()) {
                        layoutMonto.setError(getString(R.string.error_monto_vacio));
                        return;
                    }
                    try {
                        double monto = Double.parseDouble(montoStr);
                        if (monto <= 0) {
                            layoutMonto.setError(getString(R.string.error_monto_invalido));
                            return;
                        }
                        String nota = campNota.getText() != null
                                ? campNota.getText().toString().trim() : "";
                        viewModel.registrarPago(ordenId, monto, metodo[0], nota);
                        dialog.dismiss();
                    } catch (NumberFormatException e) {
                        layoutMonto.setError(getString(R.string.error_monto_invalido));
                    }
                }));

        dialog.show();
    }

    private void mostrarSnackbar(String mensaje) {
        if (getContext() != null)
            MensajeUtils.mostrar(requireContext(), mensaje);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        enlace = null;
    }
}
