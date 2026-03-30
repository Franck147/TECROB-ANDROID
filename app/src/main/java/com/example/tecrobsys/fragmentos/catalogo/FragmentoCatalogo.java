package com.example.tecrobsys.fragmentos.catalogo;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import com.example.tecrobsys.R;
import com.example.tecrobsys.adaptadores.AdaptadorServicio;
import com.example.tecrobsys.databinding.FragmentoCatalogoBinding;
import com.example.tecrobsys.modelos.ServicioCatalogo;
import com.example.tecrobsys.red.SupabaseCliente;
import com.example.tecrobsys.utils.SesionManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentoCatalogo extends Fragment {

    private static final String TAG = "TECROB_CAT";

    private FragmentoCatalogoBinding enlace;
    private AdaptadorServicio adaptador;

    private final List<ServicioCatalogo> todosLosServicios = new ArrayList<>();
    private final List<ServicioCatalogo> serviciosFiltrados = new ArrayList<>();

    private String categoriaActiva = null;
    private int empresaId;

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
        empresaId = SesionManager.obtenerInstancia(requireContext()).obtenerEmpresaId();
        configurarRecycler();
        configurarChips();
        configurarBusqueda();
        configurarBotones();
        enlace.swipeRefresh.setColorSchemeResources(R.color.rojo_primario);
        enlace.swipeRefresh.setOnRefreshListener(this::cargarServicios);
        cargarServicios();
    }

    private void configurarRecycler() {
        adaptador = new AdaptadorServicio(serviciosFiltrados,
                servicio -> Snackbar.make(enlace.getRoot(),
                        servicio.getNombre() + " — " + servicio.getPrecioFormateado(),
                        Snackbar.LENGTH_SHORT).show(),
                this::mostrarMenuEdicion);
        enlace.recyclerServicios.setLayoutManager(new LinearLayoutManager(requireContext()));
        enlace.recyclerServicios.setAdapter(adaptador);
    }

    private void configurarBotones() {
        enlace.botonAgregarNuevo.setOnClickListener(v -> mostrarDialogoServicio(null));
    }

    private void configurarChips() {
        enlace.chipTodos.setOnClickListener(v -> { categoriaActiva = null; aplicarFiltros(); });
        enlace.chipMantenimiento.setOnClickListener(v -> { categoriaActiva = "mantenimiento"; aplicarFiltros(); });
        enlace.chipSoftware.setOnClickListener(v -> { categoriaActiva = "software"; aplicarFiltros(); });
        enlace.chipRepuesto.setOnClickListener(v -> { categoriaActiva = "repuesto"; aplicarFiltros(); });
        enlace.chipDiagnostico.setOnClickListener(v -> { categoriaActiva = "diagnostico"; aplicarFiltros(); });
    }

    private void configurarBusqueda() {
        enlace.campoBusqueda.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable e) {}
            @Override public void onTextChanged(CharSequence t, int i, int a, int c) { aplicarFiltros(); }
        });
    }

    private void aplicarFiltros() {
        String texto = enlace.campoBusqueda.getText().toString().toLowerCase().trim();
        serviciosFiltrados.clear();
        for (ServicioCatalogo s : todosLosServicios) {
            boolean pasaCategoria = categoriaActiva == null || categoriaActiva.equals(s.getCategoria());
            boolean pasaBusqueda = texto.isEmpty() || (s.getNombre() != null && s.getNombre().toLowerCase().contains(texto));
            if (pasaCategoria && pasaBusqueda) serviciosFiltrados.add(s);
        }
        adaptador.notifyDataSetChanged();
        enlace.textoSinResultados.setVisibility(serviciosFiltrados.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ════════════════════════════════════════════════════════
    //  CRUD — Diálogos
    // ════════════════════════════════════════════════════════

    public void mostrarDialogoServicio(@Nullable ServicioCatalogo servicio) {
        boolean esEdicion = servicio != null;

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
        campoPrecio.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
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
                    String nombre = campoNombre.getText().toString().trim();
                    String precioStr = campoPrecio.getText().toString().trim();
                    String categoriaTexto = campoCategoria.getText().toString().trim();
                    String descripcion = campoDesc.getText().toString().trim();

                    if (nombre.isEmpty() || precioStr.isEmpty() || categoriaTexto.isEmpty()) {
                        Snackbar.make(enlace.getRoot(), "Completa los campos obligatorios", Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    String categoriaValor = "otro";
                    for (int i = 0; i < CATEGORIAS_ETIQUETA.length; i++) {
                        if (CATEGORIAS_ETIQUETA[i].equals(categoriaTexto)) { categoriaValor = CATEGORIAS_VALOR[i]; break; }
                    }

                    double precio;
                    try { precio = Double.parseDouble(precioStr); } catch (NumberFormatException e) { precio = 0.0; }

                    if (esEdicion) editarServicio(servicio.getId(), nombre, precio, categoriaValor, descripcion);
                    else agregarServicio(nombre, precio, categoriaValor, descripcion);
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
                .setMessage("¿Eliminar \"" + servicio.getNombre() + "\"? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) -> eliminarServicio(servicio.getId()))
                .setNegativeButton("Cancelar", null).show();
    }

    // ════════════════════════════════════════════════════════
    //  LLAMADAS A SUPABASE
    // ════════════════════════════════════════════════════════

    private void cargarServicios() {
        enlace.swipeRefresh.setRefreshing(true);
        Log.d(TAG, "Cargando servicios empresa_id=" + empresaId);
        SupabaseCliente.obtenerServicio()
                .listarServicios("eq." + empresaId, "eq.true", "nombre.asc")
                .enqueue(new Callback<List<ServicioCatalogo>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ServicioCatalogo>> c, @NonNull Response<List<ServicioCatalogo>> r) {
                        if (enlace == null) return;
                        enlace.swipeRefresh.setRefreshing(false);
                        if (r.isSuccessful() && r.body() != null) {
                            todosLosServicios.clear();
                            todosLosServicios.addAll(r.body());
                            aplicarFiltros();
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<List<ServicioCatalogo>> c, @NonNull Throwable e) {
                        if (enlace == null) return;
                        enlace.swipeRefresh.setRefreshing(false);
                        enlace.textoSinResultados.setVisibility(View.VISIBLE);
                        enlace.textoSinResultados.setText(getString(R.string.msg_error_cargar));
                    }
                });
    }

    private void agregarServicio(String nombre, double precio, String categoria, String descripcion) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("empresa_id", empresaId);
        datos.put("nombre", nombre);
        datos.put("precio_base", precio);
        datos.put("categoria", categoria);
        datos.put("descripcion", descripcion);
        datos.put("activo", true);
        SupabaseCliente.obtenerServicio().agregarServicio(datos).enqueue(new Callback<ServicioCatalogo>() {
            @Override
            public void onResponse(@NonNull Call<ServicioCatalogo> c, @NonNull Response<ServicioCatalogo> r) {
                if (enlace == null) return;
                // 201 = creado, 200 = ok — ambos son éxito
                if (r.code() == 201 || r.isSuccessful()) {
                    Snackbar.make(enlace.getRoot(), "✅ Servicio agregado", Snackbar.LENGTH_SHORT).show();
                    cargarServicios();
                } else {
                    Snackbar.make(enlace.getRoot(), getString(R.string.error_servidor), Snackbar.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<ServicioCatalogo> c, @NonNull Throwable e) {
                if (enlace == null) return;
                Snackbar.make(enlace.getRoot(), getString(R.string.error_sin_internet), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void editarServicio(int id, String nombre, double precio, String categoria, String descripcion) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nombre);
        datos.put("precio_base", precio);
        datos.put("categoria", categoria);
        datos.put("descripcion", descripcion);
        SupabaseCliente.obtenerServicio().actualizarServicio("eq." + id, datos).enqueue(new Callback<ServicioCatalogo>() {
            @Override
            public void onResponse(@NonNull Call<ServicioCatalogo> c, @NonNull Response<ServicioCatalogo> r) {
                if (enlace == null) return;
                // Supabase PATCH devuelve 200, 201 o 204 según config
                if (r.code() == 200 || r.code() == 201 || r.code() == 204 || r.isSuccessful()) {
                    Snackbar.make(enlace.getRoot(), "✅ Servicio actualizado", Snackbar.LENGTH_SHORT).show();
                    cargarServicios();
                }
            }
            @Override
            public void onFailure(@NonNull Call<ServicioCatalogo> c, @NonNull Throwable e) {
                if (enlace == null) return;
                Snackbar.make(enlace.getRoot(), getString(R.string.error_sin_internet), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void eliminarServicio(int id) {
        SupabaseCliente.obtenerServicio().eliminarServicio("eq." + id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> r) {
                if (enlace == null) return;
                // Supabase DELETE devuelve 200, 201 o 204
                if (r.code() == 200 || r.code() == 201 || r.code() == 204 || r.isSuccessful()) {
                    Snackbar.make(enlace.getRoot(), "🗑️ Servicio eliminado", Snackbar.LENGTH_SHORT).show();
                    cargarServicios();
                }
            }
            @Override
            public void onFailure(@NonNull Call<Void> c, @NonNull Throwable e) {
                if (enlace == null) return;
                Snackbar.make(enlace.getRoot(), getString(R.string.error_sin_internet), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        enlace = null;
    }
}