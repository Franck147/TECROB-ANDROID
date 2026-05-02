package com.example.tecrobsys.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.tecrobsys.modelos.Orden;
import com.example.tecrobsys.repositorios.OrdenRepository;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * DashboardViewModel — Lógica del panel de inicio.
 *
 * Hace dos llamadas a Supabase en paralelo:
 *   1. Órdenes en_progreso → para la tarjeta "activas"
 *   2. Todas las órdenes   → para contar pendientes + lista de 5 recientes
 */
public class DashboardViewModel extends ViewModel {

    public final MutableLiveData<List<Orden>> ordenesRecientes   = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Integer>     cantidadActivas    = new MutableLiveData<>(0);
    public final MutableLiveData<Integer>     cantidadPendientes = new MutableLiveData<>(0);
    public final MutableLiveData<Boolean>     cargando           = new MutableLiveData<>(false);
    public final MutableLiveData<String>      error              = new MutableLiveData<>();

    private final OrdenRepository repo = new OrdenRepository();

    /**
     * Carga los datos del dashboard.
     * Se puede llamar al entrar al fragmento y al hacer SwipeRefresh.
     */
    public void cargarDatos(int empresaId) {
        cargando.setValue(true);
        error.setValue(null);
        cargarActivas(empresaId);
        cargarRecientes(empresaId);
    }

    /** Llamada 1: cuenta órdenes en_progreso para la tarjeta "activas". */
    private void cargarActivas(int empresaId) {
        repo.listarOrdenesPorEstado(empresaId, "en_progreso",
                new Callback<List<Orden>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Orden>> c,
                                           @NonNull Response<List<Orden>> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            cantidadActivas.postValue(r.body().size());
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<List<Orden>> c,
                                          @NonNull Throwable e) {
                        cantidadActivas.postValue(0);
                    }
                });
    }

    /**
     * Llamada 2: trae todas las órdenes para calcular pendientes
     * y construir la lista de las últimas 5.
     */
    private void cargarRecientes(int empresaId) {
        repo.listarOrdenes(empresaId, new Callback<List<Orden>>() {
            @Override
            public void onResponse(@NonNull Call<List<Orden>> c,
                                   @NonNull Response<List<Orden>> r) {
                cargando.postValue(false);
                if (r.isSuccessful() && r.body() != null) {
                    List<Orden> todas = r.body();

                    // Contar pendientes
                    long pendientes = 0;
                    for (Orden o : todas) {
                        if ("pendiente".equals(o.getEstado())) pendientes++;
                    }
                    cantidadPendientes.postValue((int) pendientes);

                    // Últimas 5 órdenes
                    int limite = Math.min(5, todas.size());
                    ordenesRecientes.postValue(new ArrayList<>(todas.subList(0, limite)));
                } else {
                    error.postValue("Error al cargar órdenes");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Orden>> c,
                                   @NonNull Throwable e) {
                cargando.postValue(false);
                error.postValue("Sin conexión a internet");
            }
        });
    }
}
