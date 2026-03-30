package com.example.tecrobsys.fragmentos.ordenes;

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
import com.example.tecrobsys.R;
import com.example.tecrobsys.actividades.ActividadPrincipal;
import com.example.tecrobsys.adaptadores.AdaptadorOrden;
import com.example.tecrobsys.databinding.FragmentoOrdenesBinding;
import com.example.tecrobsys.modelos.Orden;
import com.example.tecrobsys.red.SupabaseCliente;
import com.example.tecrobsys.utils.SesionManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * FragmentoOrdenes — Lista completa de órdenes con filtros y búsqueda.
 *
 * Funcionalidades:
 *   - Ver todas las órdenes o filtrar por estado
 *   - Buscar en tiempo real por número de orden o nombre de cliente
 *   - Jalar para refrescar (SwipeRefresh)
 *   - Tap en una tarjeta → abre el detalle de la orden
 *
 * Estrategia de filtrado:
 *   Carga TODAS las órdenes desde Supabase una sola vez.
 *   Los filtros de estado y búsqueda operan sobre la lista local
 *   sin hacer llamadas adicionales a la red.
 */
public class FragmentoOrdenes extends Fragment {

    private FragmentoOrdenesBinding enlace;
    private AdaptadorOrden adaptador;

    // Lista completa cargada desde Supabase
    private final List<Orden> todasLasOrdenes = new ArrayList<>();
    // Lista filtrada que se muestra en el RecyclerView
    private final List<Orden> ordenesFiltradas = new ArrayList<>();

    // Filtro de estado activo (null = mostrar todas)
    private String filtroEstado = null;
    private int empresaId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador,
                             @Nullable ViewGroup contenedor,
                             @Nullable Bundle estadoGuardado) {
        enlace = FragmentoOrdenesBinding.inflate(inflador, contenedor, false);
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
        enlace.swipeRefresh.setOnRefreshListener(this::cargarOrdenes);

        cargarOrdenes();
    }

    /** Configura el RecyclerView con el adaptador. */
    private void configurarRecycler() {
        adaptador = new AdaptadorOrden(ordenesFiltradas, orden -> {
            if (requireActivity() instanceof ActividadPrincipal) {
                ((ActividadPrincipal) requireActivity())
                        .abrirDetalleOrden(orden.getId());
            }
        });
        enlace.recyclerOrdenes.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        enlace.recyclerOrdenes.setAdapter(adaptador);
    }

    /**
     * Configura los chips de filtro.
     * Al seleccionar un chip se filtra la lista local
     * sin llamar a Supabase de nuevo.
     */
    private void configurarChips() {
        enlace.chipTodas.setOnClickListener(v -> {
            filtroEstado = null;
            aplicarFiltros();
        });
        enlace.chipPendientes.setOnClickListener(v -> {
            filtroEstado = "pendiente";
            aplicarFiltros();
        });
        enlace.chipEnProgreso.setOnClickListener(v -> {
            filtroEstado = "en_progreso";
            aplicarFiltros();
        });
        enlace.chipListas.setOnClickListener(v -> {
            filtroEstado = "listo";
            aplicarFiltros();
        });
    }

    /**
     * Configura el campo de búsqueda.
     * Filtra en tiempo real mientras el usuario escribe.
     */
    private void configurarBusqueda() {
        enlace.campoBusqueda.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override
            public void afterTextChanged(Editable e) {}
            @Override
            public void onTextChanged(CharSequence texto, int i, int a, int c) {
                aplicarFiltros();
            }
        });
    }

    /**
     * Filtra la lista combinando estado + texto de búsqueda.
     * Opera solo sobre la lista local, sin llamadas de red.
     */
    private void aplicarFiltros() {
        String texto = enlace.campoBusqueda.getText()
                .toString().toLowerCase().trim();

        ordenesFiltradas.clear();

        for (Orden orden : todasLasOrdenes) {
            // Filtro por estado
            boolean pasaEstado = filtroEstado == null
                    || filtroEstado.equals(orden.getEstado());

            // Filtro por texto (número de orden o nombre del cliente)
            boolean pasaBusqueda = texto.isEmpty();
            if (!texto.isEmpty()) {
                if (orden.getNumeroOrden() != null
                        && orden.getNumeroOrden().toLowerCase().contains(texto)) {
                    pasaBusqueda = true;
                }
                if (!pasaBusqueda && orden.getCliente() != null
                        && orden.getCliente().getNombreCompleto()
                        .toLowerCase().contains(texto)) {
                    pasaBusqueda = true;
                }
            }

            if (pasaEstado && pasaBusqueda) {
                ordenesFiltradas.add(orden);
            }
        }

        adaptador.notifyDataSetChanged();

        // Mostrar/ocultar mensaje de sin resultados
        enlace.textoSinResultados.setVisibility(
                ordenesFiltradas.isEmpty() ? View.VISIBLE : View.GONE);

        // Actualizar contadores en los chips
        actualizarContadoresChip();
    }

    /**
     * Actualiza el texto de cada chip con el conteo actual.
     * Ejemplo: "Pendientes (6)"
     */
    private void actualizarContadoresChip() {
        long pendientes = todasLasOrdenes.stream()
                .filter(o -> "pendiente".equals(o.getEstado())).count();
        long enProgreso = todasLasOrdenes.stream()
                .filter(o -> "en_progreso".equals(o.getEstado())).count();
        long listas = todasLasOrdenes.stream()
                .filter(o -> "listo".equals(o.getEstado())).count();

        enlace.chipTodas.setText(
                getString(R.string.chip_todas) + " (" + todasLasOrdenes.size() + ")");
        enlace.chipPendientes.setText(
                getString(R.string.chip_pendientes) + " (" + pendientes + ")");
        enlace.chipEnProgreso.setText(
                getString(R.string.chip_en_progreso) + " (" + enProgreso + ")");
        enlace.chipListas.setText(
                getString(R.string.chip_listas) + " (" + listas + ")");
    }

    /**
     * Carga todas las órdenes desde Supabase.
     * Se llama al entrar al fragmento y al hacer SwipeRefresh.
     */
    private void cargarOrdenes() {
        enlace.progressBar.setVisibility(View.VISIBLE);

        String filtroEmpresa = "eq." + empresaId;
        String seleccion = "*,"
                + "cliente:cliente_id(nombre,apellido,telefono),"
                + "tecnico:tecnico_id(nombre,apellido),"
                + "equipo(tipo,marca,modelo,desperfecto)";

        SupabaseCliente.obtenerServicio()
                .listarOrdenes(filtroEmpresa, "created_at.desc", seleccion)
                .enqueue(new Callback<List<Orden>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Orden>> c,
                                           @NonNull Response<List<Orden>> r) {
                        if (enlace == null) return;
                        enlace.progressBar.setVisibility(View.GONE);
                        enlace.swipeRefresh.setRefreshing(false);

                        if (r.isSuccessful() && r.body() != null) {
                            todasLasOrdenes.clear();
                            todasLasOrdenes.addAll(r.body());
                            aplicarFiltros();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Orden>> c,
                                          @NonNull Throwable e) {
                        if (enlace == null) return;
                        enlace.progressBar.setVisibility(View.GONE);
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