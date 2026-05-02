package com.example.tecrobsys.fragmentos.dashboard;

import android.os.Bundle;
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
import com.example.tecrobsys.databinding.FragmentoDashboardBinding;
import com.example.tecrobsys.modelos.Orden;
import com.example.tecrobsys.utils.SesionManager;
import com.example.tecrobsys.utils.UtilFecha;
import com.example.tecrobsys.viewmodels.DashboardViewModel;
import java.util.ArrayList;
import java.util.List;

public class FragmentoDashboard extends Fragment {

    private FragmentoDashboardBinding enlace;
    private AdaptadorOrden adaptador;
    private DashboardViewModel viewModel;

    private final List<Orden> listaOrdenes = new ArrayList<>();

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

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        SesionManager sesion = SesionManager.obtenerInstancia(requireContext());
        int empresaId = sesion.obtenerEmpresaId();

        // Saludo con nombre real del técnico (del perfil guardado en sesión)
        enlace.textBienvenida.setText(
                getString(R.string.lbl_hola) + " " + sesion.obtenerNombreTecnico());
        enlace.textFechaHoy.setText(UtilFecha.obtenerFechaHoy());

        configurarRecycler();
        observarViewModel();

        enlace.btnVerTodas.setOnClickListener(v -> {
            if (requireActivity() instanceof ActividadPrincipal) {
                ((ActividadPrincipal) requireActivity()).navegarA("ordenes");
            }
        });

        enlace.swipeRefresh.setColorSchemeResources(R.color.rojo_primario);
        enlace.swipeRefresh.setOnRefreshListener(() -> viewModel.cargarDatos(empresaId));

        viewModel.cargarDatos(empresaId);
    }

    private void configurarRecycler() {
        adaptador = new AdaptadorOrden(listaOrdenes, orden -> {
            if (requireActivity() instanceof ActividadPrincipal) {
                ((ActividadPrincipal) requireActivity()).abrirDetalleOrden(orden.getId());
            }
        });
        enlace.recyclerOrdenes.setLayoutManager(new LinearLayoutManager(requireContext()));
        enlace.recyclerOrdenes.setAdapter(adaptador);
        enlace.recyclerOrdenes.setNestedScrollingEnabled(false);
    }

    private void observarViewModel() {
        viewModel.cantidadActivas.observe(getViewLifecycleOwner(),
                n -> enlace.textOrdenesActivas.setText(String.valueOf(n)));

        viewModel.cantidadPendientes.observe(getViewLifecycleOwner(),
                n -> enlace.textOrdenesPendientes.setText(String.valueOf(n)));

        viewModel.ordenesRecientes.observe(getViewLifecycleOwner(), ordenes -> {
            enlace.swipeRefresh.setRefreshing(false);
            listaOrdenes.clear();
            listaOrdenes.addAll(ordenes);
            adaptador.notifyDataSetChanged();
            enlace.textoSinOrdenes.setVisibility(
                    ordenes.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.cargando.observe(getViewLifecycleOwner(),
                c -> enlace.swipeRefresh.setRefreshing(Boolean.TRUE.equals(c)));

        viewModel.error.observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                enlace.textoSinOrdenes.setVisibility(View.VISIBLE);
                enlace.textoSinOrdenes.setText(err);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        enlace = null;
    }
}
