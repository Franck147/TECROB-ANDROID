package com.example.tecrobsys.fragmentos.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.tecrobsys.R;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.tecrobsys.actividades.ActividadPrincipal;
import com.example.tecrobsys.adaptadores.AdaptadorOrden;
import com.example.tecrobsys.databinding.FragmentoDashboardBinding;
import com.example.tecrobsys.modelos.Orden;
import com.example.tecrobsys.red.SupabaseCliente;
import com.example.tecrobsys.utils.SesionManager;
import com.example.tecrobsys.utils.UtilFecha;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * FragmentoDashboard — Pantalla de inicio con métricas del día.
 *
 * Muestra:
 *   - Saludo personalizado con el nombre del técnico
 *   - Fecha de hoy
 *   - Conteo de órdenes activas y pendientes
 *   - Lista de las últimas 5 órdenes
 *
 * Llama a Supabase dos veces:
 *   1. Para contar órdenes en_progreso (tarjeta activas)
 *   2. Para contar y listar órdenes pendientes
 */
public class FragmentoDashboard extends Fragment {

    // ViewBinding — generado desde fragmento_dashboard.xml
    private FragmentoDashboardBinding enlace;

    // Adaptador y lista para el RecyclerView de órdenes recientes
    private AdaptadorOrden adaptador;
    private final List<Orden> listaOrdenes = new ArrayList<>();

    private int empresaId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador,
                             @Nullable ViewGroup contenedor,
                             @Nullable Bundle estadoGuardado) {
        enlace = FragmentoDashboardBinding.inflate(inflador, contenedor, false);
        return enlace.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View vista,
                              @Nullable Bundle estadoGuardado) {
        super.onViewCreated(vista, estadoGuardado);

        SesionManager sesion = SesionManager.obtenerInstancia(requireContext());
        empresaId = sesion.obtenerEmpresaId();

        // Saludo con el nombre del técnico
        enlace.textBienvenida.setText(
                getString(R.string.lbl_hola) + " " + sesion.obtenerNombreTecnico());

        // Fecha de hoy
        enlace.textFechaHoy.setText(UtilFecha.obtenerFechaHoy());

        // Configurar la lista de órdenes recientes
        configurarRecycler();

        // Botón "Ver todas" navega a la lista de órdenes
        enlace.btnVerTodas.setOnClickListener(v -> {
            if (requireActivity() instanceof ActividadPrincipal) {
                ((ActividadPrincipal) requireActivity()).navegarA("ordenes");
            }
        });

        // Configurar SwipeRefresh
        enlace.swipeRefresh.setColorSchemeResources(R.color.rojo_primario);
        enlace.swipeRefresh.setOnRefreshListener(this::cargarDatos);

        // Cargar datos iniciales
        cargarDatos();
    }

    /**
     * Configura el RecyclerView con el adaptador de órdenes.
     * Al tocar una orden abre el detalle.
     */
    private void configurarRecycler() {
        adaptador = new AdaptadorOrden(listaOrdenes, orden -> {
            if (requireActivity() instanceof ActividadPrincipal) {
                ((ActividadPrincipal) requireActivity())
                        .abrirDetalleOrden(orden.getId());
            }
        });
        enlace.recyclerOrdenes.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        enlace.recyclerOrdenes.setAdapter(adaptador);
        enlace.recyclerOrdenes.setNestedScrollingEnabled(false);
    }

    /**
     * Carga los datos del dashboard desde Supabase.
     * Hace dos llamadas en paralelo:
     *   1. Órdenes en progreso (para la tarjeta "activas")
     *   2. Órdenes recientes (para la lista + tarjeta "pendientes")
     */
    private void cargarDatos() {
        String filtroEmpresa = "eq." + empresaId;
        String seleccion = "*,"
                + "cliente:cliente_id(nombre,apellido,telefono),"
                + "tecnico:tecnico_id(nombre,apellido),"
                + "equipo(tipo,marca,modelo,desperfecto)";

        // ── Tarjeta: Órdenes activas (en_progreso) ────────────────
        SupabaseCliente.obtenerServicio()
                .listarOrdenesPorEstado(
                        filtroEmpresa, "eq.en_progreso",
                        "created_at.desc", "id,estado")
                .enqueue(new Callback<List<Orden>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Orden>> c,
                                           @NonNull Response<List<Orden>> r) {
                        if (enlace == null) return;
                        if (r.isSuccessful() && r.body() != null) {
                            enlace.textOrdenesActivas.setText(
                                    String.valueOf(r.body().size()));
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<List<Orden>> c,
                                          @NonNull Throwable e) {
                        if (enlace != null)
                            enlace.textOrdenesActivas.setText("--");
                    }
                });

        // ── Tarjeta: Pendientes + lista de recientes ───────────────
        SupabaseCliente.obtenerServicio()
                .listarOrdenes(filtroEmpresa, "created_at.desc", seleccion)
                .enqueue(new Callback<List<Orden>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Orden>> c,
                                           @NonNull Response<List<Orden>> r) {
                        if (enlace == null) return;
                        enlace.swipeRefresh.setRefreshing(false);

                        if (r.isSuccessful() && r.body() != null) {
                            List<Orden> todas = r.body();

                            // Contar pendientes para la tarjeta
                            long pendientes = todas.stream()
                                    .filter(o -> "pendiente".equals(o.getEstado()))
                                    .count();
                            enlace.textOrdenesPendientes.setText(
                                    String.valueOf(pendientes));

                            // Mostrar solo las últimas 5 en la lista
                            listaOrdenes.clear();
                            int limite = Math.min(5, todas.size());
                            listaOrdenes.addAll(todas.subList(0, limite));
                            adaptador.notifyDataSetChanged();

                            // Mostrar/ocultar mensaje vacío
                            enlace.textoSinOrdenes.setVisibility(
                                    listaOrdenes.isEmpty()
                                            ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Orden>> c,
                                          @NonNull Throwable e) {
                        if (enlace == null) return;
                        enlace.swipeRefresh.setRefreshing(false);
                        enlace.textoSinOrdenes.setVisibility(View.VISIBLE);
                        enlace.textoSinOrdenes.setText(
                                getString(R.string.msg_sin_conexion));
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        enlace = null; // Evitar memory leaks
    }
}