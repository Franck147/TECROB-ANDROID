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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.tecrobsys.R;
import com.example.tecrobsys.actividades.ActividadPrincipal;
import com.example.tecrobsys.adaptadores.AdaptadorOrden;
import com.example.tecrobsys.databinding.FragmentoOrdenesBinding;
import com.example.tecrobsys.modelos.Orden;
import com.example.tecrobsys.utils.SesionManager;
import com.example.tecrobsys.viewmodels.OrdenesViewModel;
import java.util.ArrayList;
import java.util.List;

public class FragmentoOrdenes extends Fragment {

    private FragmentoOrdenesBinding enlace;
    private AdaptadorOrden adaptador;
    private OrdenesViewModel viewModel;

    private final List<Orden> ordenesMostradas = new ArrayList<>();

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

        viewModel = new ViewModelProvider(this).get(OrdenesViewModel.class);
        int empresaId = SesionManager.obtenerInstancia(requireContext()).obtenerEmpresaId();

        configurarRecycler();
        configurarChips();
        configurarBusqueda();
        observarViewModel();

        enlace.swipeRefresh.setColorSchemeResources(R.color.rojo_primario);
        enlace.swipeRefresh.setOnRefreshListener(() -> viewModel.cargarOrdenes(empresaId));

        // Solo cargar si la lista está vacía (evita recargar al volver de detalle)
        if (viewModel.getTodasLasOrdenes().isEmpty()) {
            viewModel.cargarOrdenes(empresaId);
        }
    }

    private void configurarRecycler() {
        adaptador = new AdaptadorOrden(ordenesMostradas, orden -> {
            if (requireActivity() instanceof ActividadPrincipal) {
                ((ActividadPrincipal) requireActivity()).abrirDetalleOrden(orden.getId());
            }
        });
        enlace.recyclerOrdenes.setLayoutManager(new LinearLayoutManager(requireContext()));
        enlace.recyclerOrdenes.setAdapter(adaptador);
    }

    private void configurarChips() {
        enlace.chipTodas.setOnClickListener(v -> viewModel.setFiltroEstado(null));
        enlace.chipPendientes.setOnClickListener(v -> viewModel.setFiltroEstado("pendiente"));
        enlace.chipEnProgreso.setOnClickListener(v -> viewModel.setFiltroEstado("en_progreso"));
        enlace.chipListas.setOnClickListener(v -> viewModel.setFiltroEstado("listo"));
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
        viewModel.ordenesFiltradas.observe(getViewLifecycleOwner(), ordenes -> {
            ordenesMostradas.clear();
            ordenesMostradas.addAll(ordenes);
            adaptador.notifyDataSetChanged();

            enlace.textoSinResultados.setVisibility(
                    ordenes.isEmpty() ? View.VISIBLE : View.GONE);
            enlace.progressBar.setVisibility(View.GONE);

            actualizarContadoresChip();
        });

        viewModel.cargando.observe(getViewLifecycleOwner(), c -> {
            boolean cargando = Boolean.TRUE.equals(c);
            enlace.progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
            enlace.swipeRefresh.setRefreshing(false);
        });

        viewModel.error.observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                enlace.textoSinResultados.setVisibility(View.VISIBLE);
                enlace.textoSinResultados.setText(err);
                viewModel.error.setValue(null);
            }
        });
    }

    private void actualizarContadoresChip() {
        List<Orden> todas = viewModel.getTodasLasOrdenes();
        long pendientes = 0, enProgreso = 0, listas = 0;
        for (Orden o : todas) {
            if ("pendiente".equals(o.getEstado()))  pendientes++;
            if ("en_progreso".equals(o.getEstado())) enProgreso++;
            if ("listo".equals(o.getEstado()))       listas++;
        }
        enlace.chipTodas.setText(getString(R.string.chip_todas) + " (" + todas.size() + ")");
        enlace.chipPendientes.setText(getString(R.string.chip_pendientes) + " (" + pendientes + ")");
        enlace.chipEnProgreso.setText(getString(R.string.chip_en_progreso) + " (" + enProgreso + ")");
        enlace.chipListas.setText(getString(R.string.chip_listas) + " (" + listas + ")");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        enlace = null;
    }
}
