package com.example.tecrobsys.fragmentos.catalogo;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.snackbar.Snackbar;
import com.example.tecrobsys.R;
import com.example.tecrobsys.adaptadores.AdaptadorServicio;
import com.example.tecrobsys.databinding.FragmentoCatalogoBinding;
import com.example.tecrobsys.modelos.ServicioCatalogo;
import com.example.tecrobsys.red.SupabaseCliente;
import com.example.tecrobsys.utils.SesionManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * FragmentoCatalogo — Catálogo de servicios y repuestos del taller.
 *
 * Funcionalidades:
 *   - Listar todos los servicios activos
 *   - Filtrar por categoría: mantenimiento, software, repuesto, diagnóstico
 *   - Buscar por nombre en tiempo real
 *   - Botón "+" en cada servicio para agregar a una orden futura
 *   - Jalar para refrescar
 */
public class FragmentoCatalogo extends Fragment {

    private FragmentoCatalogoBinding enlace;
    private AdaptadorServicio adaptador;

    // Lista completa desde Supabase
    private final List<ServicioCatalogo> todosLosServicios = new ArrayList<>();
    // Lista filtrada que se muestra
    private final List<ServicioCatalogo> serviciosFiltrados = new ArrayList<>();

    // Categoría activa (null = todas)
    private String categoriaActiva = null;
    private int empresaId;

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

        empresaId = SesionManager.obtenerInstancia(requireContext())
                .obtenerEmpresaId();

        configurarRecycler();
        configurarChips();
        configurarBusqueda();

        enlace.swipeRefresh.setColorSchemeResources(R.color.rojo_primario);
        enlace.swipeRefresh.setOnRefreshListener(this::cargarServicios);

        cargarServicios();
    }

    /** Configura el RecyclerView con el adaptador de servicios. */
    private void configurarRecycler() {
        adaptador = new AdaptadorServicio(serviciosFiltrados, servicio -> {
            // Al tocar "+" mostrar un Snackbar de confirmación
            // En el futuro: agregar el servicio a la orden actual
            Snackbar.make(enlace.getRoot(),
                    servicio.getNombre() + " — " + servicio.getPrecioFormateado(),
                    Snackbar.LENGTH_SHORT).show();
        });
        enlace.recyclerServicios.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        enlace.recyclerServicios.setAdapter(adaptador);
    }

    /**
     * Configura los chips de categoría.
     * Al seleccionar un chip filtra la lista local.
     */
    private void configurarChips() {
        enlace.chipTodos.setOnClickListener(v -> {
            categoriaActiva = null;
            aplicarFiltros();
        });
        enlace.chipMantenimiento.setOnClickListener(v -> {
            categoriaActiva = "mantenimiento";
            aplicarFiltros();
        });
        enlace.chipSoftware.setOnClickListener(v -> {
            categoriaActiva = "software";
            aplicarFiltros();
        });
        enlace.chipRepuesto.setOnClickListener(v -> {
            categoriaActiva = "repuesto";
            aplicarFiltros();
        });
        enlace.chipDiagnostico.setOnClickListener(v -> {
            categoriaActiva = "diagnostico";
            aplicarFiltros();
        });
    }

    /**
     * Configura la búsqueda en tiempo real.
     */
    private void configurarBusqueda() {
        enlace.campoBusqueda.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable e) {}
            @Override
            public void onTextChanged(CharSequence t, int i, int a, int c) {
                aplicarFiltros();
            }
        });
    }

    /**
     * Filtra la lista por categoría + texto de búsqueda.
     * Opera sobre la lista local sin llamadas de red.
     */
    private void aplicarFiltros() {
        String texto = enlace.campoBusqueda.getText()
                .toString().toLowerCase().trim();

        serviciosFiltrados.clear();

        for (ServicioCatalogo s : todosLosServicios) {
            boolean pasaCategoria = categoriaActiva == null
                    || categoriaActiva.equals(s.getCategoria());

            boolean pasaBusqueda = texto.isEmpty()
                    || (s.getNombre() != null
                    && s.getNombre().toLowerCase().contains(texto));

            if (pasaCategoria && pasaBusqueda) {
                serviciosFiltrados.add(s);
            }
        }

        adaptador.notifyDataSetChanged();
        enlace.textoSinResultados.setVisibility(
                serviciosFiltrados.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * Carga todos los servicios activos desde Supabase.
     */
    private void cargarServicios() {
        enlace.swipeRefresh.setRefreshing(true);

        SupabaseCliente.obtenerServicio()
                .listarServicios(
                        "eq." + empresaId,
                        "eq.true",
                        "nombre.asc")
                .enqueue(new Callback<List<ServicioCatalogo>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<List<ServicioCatalogo>> c,
                            @NonNull Response<List<ServicioCatalogo>> r) {
                        if (enlace == null) return;
                        enlace.swipeRefresh.setRefreshing(false);

                        if (r.isSuccessful() && r.body() != null) {
                            todosLosServicios.clear();
                            todosLosServicios.addAll(r.body());
                            aplicarFiltros();
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<List<ServicioCatalogo>> c,
                            @NonNull Throwable e) {
                        if (enlace == null) return;
                        enlace.swipeRefresh.setRefreshing(false);
                        enlace.textoSinResultados.setVisibility(View.VISIBLE);
                        enlace.textoSinResultados.setText(
                                getString(R.string.msg_error_cargar));
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        enlace = null;
    }
}