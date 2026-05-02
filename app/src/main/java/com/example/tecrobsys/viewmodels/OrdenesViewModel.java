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
 * OrdenesViewModel — Lógica de la lista de órdenes con filtros.
 *
 * Estrategia: carga TODAS las órdenes una vez desde Supabase.
 * Los filtros por estado y búsqueda operan sobre la lista local
 * sin hacer llamadas adicionales a la red.
 *
 * Ventaja MVVM: la lista sobrevive a rotaciones de pantalla.
 */
public class OrdenesViewModel extends ViewModel {

    public final MutableLiveData<List<Orden>> ordenesFiltradas = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Boolean>     cargando         = new MutableLiveData<>(false);
    public final MutableLiveData<String>      error            = new MutableLiveData<>();

    // Lista completa en memoria — no expuesta directamente a la UI
    private final List<Orden> todasLasOrdenes = new ArrayList<>();

    // Filtros activos
    private String filtroEstado  = null;
    private String textoBusqueda = "";

    private final OrdenRepository repo = new OrdenRepository();

    /**
     * Carga todas las órdenes desde Supabase.
     * Después aplica los filtros activos sobre la lista completa.
     */
    public void cargarOrdenes(int empresaId) {
        cargando.setValue(true);
        error.setValue(null);

        repo.listarOrdenes(empresaId, new Callback<List<Orden>>() {
            @Override
            public void onResponse(@NonNull Call<List<Orden>> c,
                                   @NonNull Response<List<Orden>> r) {
                cargando.postValue(false);
                if (r.isSuccessful() && r.body() != null) {
                    todasLasOrdenes.clear();
                    todasLasOrdenes.addAll(r.body());
                    aplicarFiltros();
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

    /**
     * Aplica un filtro por estado. Pasar null para mostrar todas.
     * Ejemplo: setFiltroEstado("pendiente") o setFiltroEstado(null)
     */
    public void setFiltroEstado(String estado) {
        this.filtroEstado = estado;
        aplicarFiltros();
    }

    /**
     * Aplica un filtro de búsqueda por número de orden o nombre de cliente.
     */
    public void setBusqueda(String texto) {
        this.textoBusqueda = texto != null ? texto.toLowerCase().trim() : "";
        aplicarFiltros();
    }

    /**
     * Retorna la lista completa (para calcular contadores de chips).
     */
    public List<Orden> getTodasLasOrdenes() {
        return todasLasOrdenes;
    }

    /** Combina filtro de estado + búsqueda y publica el resultado. */
    private void aplicarFiltros() {
        List<Orden> resultado = new ArrayList<>();

        for (Orden o : todasLasOrdenes) {
            boolean pasaEstado = filtroEstado == null
                    || filtroEstado.equals(o.getEstado());

            boolean pasaBusqueda = textoBusqueda.isEmpty();
            if (!textoBusqueda.isEmpty()) {
                if (o.getNumeroOrden() != null
                        && o.getNumeroOrden().toLowerCase().contains(textoBusqueda)) {
                    pasaBusqueda = true;
                }
                if (!pasaBusqueda && o.getCliente() != null
                        && o.getCliente().getNombreCompleto()
                        .toLowerCase().contains(textoBusqueda)) {
                    pasaBusqueda = true;
                }
            }

            if (pasaEstado && pasaBusqueda) resultado.add(o);
        }

        ordenesFiltradas.postValue(resultado);
    }
}
