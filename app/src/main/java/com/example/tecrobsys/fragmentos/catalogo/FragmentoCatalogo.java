package com.example.tecrobsys.fragmentos.catalogo;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.tecrobsys.utils.MensajeUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import com.example.tecrobsys.R;
import com.example.tecrobsys.adaptadores.AdaptadorServicio;
import com.example.tecrobsys.databinding.FragmentoCatalogoBinding;
import com.example.tecrobsys.modelos.ServicioCatalogo;
import com.example.tecrobsys.utils.SesionManager;
import com.example.tecrobsys.viewmodels.CatalogoViewModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FragmentoCatalogo extends Fragment {

    private FragmentoCatalogoBinding enlace;
    private AdaptadorServicio adaptador;
    private CatalogoViewModel viewModel;

    private final List<ServicioCatalogo> serviciosMostrados = new ArrayList<>();

    private static final String[] CATEGORIAS_VALOR = {
            "mantenimiento", "reparacion", "software",
            "repuesto", "diagnostico", "otro"
    };
    private static final String[] CATEGORIAS_ETIQUETA = {
            "Mantenimiento", "Reparación", "Software",
            "Repuesto", "Diagnóstico", "Otro"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador,
                             @Nullable ViewGroup contenedor,
                             @Nullable Bundle estadoGuardado) {
        enlace = FragmentoCatalogoBinding.inflate(inflador, contenedor, false);
        return enlace.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View vista,
                              @Nullable Bundle estadoGuardado) {
        super.onViewCreated(vista, estadoGuardado);

        viewModel = new ViewModelProvider(this).get(CatalogoViewModel.class);
        int empresaId = SesionManager.obtenerInstancia(requireContext()).obtenerEmpresaId();

        configurarRecycler();
        configurarChips();
        configurarBusqueda();
        configurarBotones();
        observarViewModel();

        enlace.swipeRefresh.setColorSchemeResources(R.color.rojo_primario);
        enlace.swipeRefresh.setOnRefreshListener(() -> viewModel.cargarServicios(empresaId));

        viewModel.cargarServicios(empresaId);
    }

    private void configurarRecycler() {
        adaptador = new AdaptadorServicio(serviciosMostrados,
                servicio -> MensajeUtils.mostrar(requireContext(),
                        servicio.getNombre() + "\n" + servicio.getPrecioFormateado()),
                this::mostrarMenuEdicion);
        enlace.recyclerServicios.setLayoutManager(new LinearLayoutManager(requireContext()));
        enlace.recyclerServicios.setAdapter(adaptador);
    }

    private void configurarBotones() {
        enlace.botonAgregarNuevo.setOnClickListener(v -> mostrarDialogoServicio(null));
    }

    private void configurarChips() {
        enlace.chipTodos.setOnClickListener(v -> viewModel.setCategoria(null));
        enlace.chipMantenimiento.setOnClickListener(v -> viewModel.setCategoria("mantenimiento"));
        enlace.chipSoftware.setOnClickListener(v -> viewModel.setCategoria("software"));
        enlace.chipRepuesto.setOnClickListener(v -> viewModel.setCategoria("repuesto"));
        enlace.chipDiagnostico.setOnClickListener(v -> viewModel.setCategoria("diagnostico"));
    }

    private void configurarBusqueda() {
        enlace.campoBusqueda.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable e) {}
            @Override public void onTextChanged(CharSequence t, int i, int a, int c) {
                viewModel.setBusqueda(t.toString());
            }
        });
    }

    private void observarViewModel() {
        viewModel.serviciosFiltrados.observe(getViewLifecycleOwner(), servicios -> {
            serviciosMostrados.clear();
            serviciosMostrados.addAll(servicios);
            adaptador.notifyDataSetChanged();
            enlace.textoSinResultados.setVisibility(
                    servicios.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.cargando.observe(getViewLifecycleOwner(),
                c -> enlace.swipeRefresh.setRefreshing(Boolean.TRUE.equals(c)));

        viewModel.mensajeExito.observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                MensajeUtils.mostrar(requireContext(), msg);
                viewModel.mensajeExito.setValue(null);
            }
        });

        viewModel.error.observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                enlace.textoSinResultados.setVisibility(View.VISIBLE);
                enlace.textoSinResultados.setText(err);
                viewModel.error.setValue(null);
            }
        });
    }

    // ════════════════════════════════════════════════════════
    //  Diálogos CRUD
    // ════════════════════════════════════════════════════════

    public void mostrarDialogoServicio(@Nullable ServicioCatalogo servicio) {
        boolean esEdicion = servicio != null;
        int empresaId = SesionManager.obtenerInstancia(requireContext()).obtenerEmpresaId();

        android.content.Context ctxClaro = new android.view.ContextThemeWrapper(
                requireContext(),
                com.google.android.material.R.style.Theme_Material3_Light_NoActionBar);

        LinearLayout contenedor = new LinearLayout(ctxClaro);
        contenedor.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        contenedor.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (8 * getResources().getDisplayMetrics().density);

        TextInputLayout layoutNombre = new TextInputLayout(ctxClaro, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        layoutNombre.setHint("Nombre del servicio *");
        TextInputEditText campoNombre = new TextInputEditText(ctxClaro);
        campoNombre.setTextColor(Color.BLACK);
        layoutNombre.addView(campoNombre);
        contenedor.addView(layoutNombre);

        TextInputLayout layoutPrecio = new TextInputLayout(ctxClaro, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        layoutPrecio.setHint("Precio base (S/) *");
        TextInputEditText campoPrecio = new TextInputEditText(ctxClaro);
        campoPrecio.setTextColor(Color.BLACK);
        campoPrecio.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layoutPrecio.addView(campoPrecio);
        layoutPrecio.setLayoutParams(params);
        contenedor.addView(layoutPrecio);

        TextInputLayout layoutCategoria = new TextInputLayout(ctxClaro, null,
                com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle);
        layoutCategoria.setHint("Categoría *");
        AutoCompleteTextView campoCategoria = new AutoCompleteTextView(ctxClaro);
        campoCategoria.setTextColor(Color.BLACK);
        campoCategoria.setFocusable(false);
        campoCategoria.setAdapter(new ArrayAdapter<>(ctxClaro,
                android.R.layout.simple_dropdown_item_1line, CATEGORIAS_ETIQUETA));
        layoutCategoria.addView(campoCategoria);
        layoutCategoria.setLayoutParams(params);
        contenedor.addView(layoutCategoria);

        TextInputLayout layoutDesc = new TextInputLayout(ctxClaro, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        layoutDesc.setHint("Descripción (opcional)");
        TextInputEditText campoDesc = new TextInputEditText(ctxClaro);
        campoDesc.setTextColor(Color.BLACK);
        layoutDesc.addView(campoDesc);
        layoutDesc.setLayoutParams(params);
        contenedor.addView(layoutDesc);

        if (esEdicion) {
            campoNombre.setText(servicio.getNombre());
            campoPrecio.setText(String.valueOf(servicio.getPrecioBase()));
            campoDesc.setText(servicio.getDescripcion());
            for (int i = 0; i < CATEGORIAS_VALOR.length; i++) {
                if (CATEGORIAS_VALOR[i].equals(servicio.getCategoria())) {
                    campoCategoria.setText(CATEGORIAS_ETIQUETA[i], false);
                    break;
                }
            }
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(esEdicion ? "Editar servicio" : "Nuevo servicio")
                .setView(contenedor)
                .setPositiveButton(esEdicion ? "Guardar cambios" : "Agregar", (d, w) -> {
                    String nombre      = campoNombre.getText().toString().trim();
                    String precioStr   = campoPrecio.getText().toString().trim();
                    String catTexto    = campoCategoria.getText().toString().trim();
                    String descripcion = campoDesc.getText().toString().trim();

                    if (nombre.isEmpty() || precioStr.isEmpty() || catTexto.isEmpty()) {
                        MensajeUtils.mostrar(requireContext(),
                                "Completa los campos obligatorios");
                        return;
                    }

                    String catValor = "otro";
                    for (int i = 0; i < CATEGORIAS_ETIQUETA.length; i++) {
                        if (CATEGORIAS_ETIQUETA[i].equals(catTexto)) {
                            catValor = CATEGORIAS_VALOR[i];
                            break;
                        }
                    }

                    double precio;
                    try { precio = Double.parseDouble(precioStr); }
                    catch (NumberFormatException e) { precio = 0.0; }

                    Map<String, Object> datos = new HashMap<>();
                    datos.put("nombre",      nombre);
                    datos.put("precio_base", precio);
                    datos.put("categoria",   catValor);
                    datos.put("descripcion", descripcion);

                    if (esEdicion) {
                        viewModel.actualizarServicio(servicio.getId(), datos);
                    } else {
                        datos.put("empresa_id", empresaId);
                        datos.put("activo",     true);
                        viewModel.agregarServicio(datos);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarMenuEdicion(ServicioCatalogo servicio) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(servicio.getNombre())
                .setItems(new String[]{"✏️  Editar", "🗑️  Eliminar"}, (d, which) -> {
                    if (which == 0) mostrarDialogoServicio(servicio);
                    else confirmarEliminacion(servicio);
                }).show();
    }

    private void confirmarEliminacion(ServicioCatalogo servicio) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar servicio")
                .setMessage("¿Eliminar \"" + servicio.getNombre()
                        + "\"? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar",
                        (d, w) -> viewModel.eliminarServicio(servicio.getId()))
                .setNegativeButton("Cancelar", null).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        enlace = null;
    }
}
