package com.example.tecrobsys.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.tecrobsys.modelos.ServicioCatalogo;
import com.example.tecrobsys.repositorios.ServicioRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * CatalogoViewModel — Lógica del catálogo de servicios y repuestos.
 *
 * Estrategia: carga TODOS los servicios activos una vez.
 * El filtro por categoría y búsqueda opera sobre la lista local.
 *
 * El CRUD (agregar, editar, eliminar) llama a Supabase y después
 * recarga la lista completa para mantener sincronía.
 */
public class CatalogoViewModel extends ViewModel {

    public final MutableLiveData<List<ServicioCatalogo>> serviciosFiltrados = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Boolean>                cargando           = new MutableLiveData<>(false);
    public final MutableLiveData<String>                 error              = new MutableLiveData<>();
    // Evento puntual: mensaje de éxito tras operación CRUD
    public final MutableLiveData<String>                 mensajeExito       = new MutableLiveData<>();

    // Lista completa en memoria — no expuesta directamente a la UI
    private final List<ServicioCatalogo> todos = new ArrayList<>();

    // Filtros activos
    private String categoriaActiva = null;
    private String textoBusqueda   = "";

    // empresaId guardado para recargar tras CRUD
    private int empresaIdCache = 0;

    private final ServicioRepository repo = new ServicioRepository();

    /**
     * Carga todos los servicios activos desde Supabase y aplica filtros.
     */
    public void cargarServicios(int empresaId) {
        this.empresaIdCache = empresaId;
        cargando.setValue(true);
        error.setValue(null);

        repo.listarServicios(empresaId, new Callback<List<ServicioCatalogo>>() {
            @Override
            public void onResponse(@NonNull Call<List<ServicioCatalogo>> c,
                                   @NonNull Response<List<ServicioCatalogo>> r) {
                cargando.postValue(false);
                if (r.isSuccessful() && r.body() != null) {
                    todos.clear();
                    todos.addAll(r.body());
                    aplicarFiltros();
                } else {
                    error.postValue("Error al cargar servicios");
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<ServicioCatalogo>> c,
                                   @NonNull Throwable e) {
                cargando.postValue(false);
                error.postValue("Sin conexión a internet");
            }
        });
    }

    /** Filtra por categoría. Pasar null para mostrar todas. */
    public void setCategoria(String categoria) {
        this.categoriaActiva = categoria;
        aplicarFiltros();
    }

    /** Filtra por nombre del servicio. */
    public void setBusqueda(String texto) {
        this.textoBusqueda = texto != null ? texto.toLowerCase().trim() : "";
        aplicarFiltros();
    }

    /** Agrega un nuevo servicio y recarga la lista. */
    public void agregarServicio(Map<String, Object> datos) {
        repo.agregarServicio(datos, new Callback<ServicioCatalogo>() {
            @Override
            public void onResponse(@NonNull Call<ServicioCatalogo> c,
                                   @NonNull Response<ServicioCatalogo> r) {
                if (r.code() == 201 || r.isSuccessful()) {
                    mensajeExito.postValue("✅ Servicio agregado");
                    cargarServicios(empresaIdCache);
                } else {
                    error.postValue("Error del servidor");
                }
            }
            @Override
            public void onFailure(@NonNull Call<ServicioCatalogo> c,
                                   @NonNull Throwable e) {
                error.postValue("Sin conexión a internet");
            }
        });
    }

    /** Edita un servicio existente y recarga la lista. */
    public void actualizarServicio(int id, Map<String, Object> datos) {
        repo.actualizarServicio(id, datos, new Callback<ServicioCatalogo>() {
            @Override
            public void onResponse(@NonNull Call<ServicioCatalogo> c,
                                   @NonNull Response<ServicioCatalogo> r) {
                if (r.code() == 200 || r.code() == 201 || r.code() == 204 || r.isSuccessful()) {
                    mensajeExito.postValue("✅ Servicio actualizado");
                    cargarServicios(empresaIdCache);
                }
            }
            @Override
            public void onFailure(@NonNull Call<ServicioCatalogo> c,
                                   @NonNull Throwable e) {
                error.postValue("Sin conexión a internet");
            }
        });
    }

    /** Elimina un servicio y recarga la lista. */
    public void eliminarServicio(int id) {
        repo.eliminarServicio(id, new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> r) {
                if (r.code() == 200 || r.code() == 201 || r.code() == 204 || r.isSuccessful()) {
                    mensajeExito.postValue("🗑️ Servicio eliminado");
                    cargarServicios(empresaIdCache);
                }
            }
            @Override
            public void onFailure(@NonNull Call<Void> c, @NonNull Throwable e) {
                error.postValue("Sin conexión a internet");
            }
        });
    }

    /** Combina filtro de categoría + búsqueda y publica el resultado. */
    private void aplicarFiltros() {
        List<ServicioCatalogo> resultado = new ArrayList<>();
        for (ServicioCatalogo s : todos) {
            boolean pasaCategoria = categoriaActiva == null
                    || categoriaActiva.equals(s.getCategoria());
            boolean pasaBusqueda  = textoBusqueda.isEmpty()
                    || (s.getNombre() != null
                    && s.getNombre().toLowerCase().contains(textoBusqueda));
            if (pasaCategoria && pasaBusqueda) resultado.add(s);
        }
        serviciosFiltrados.postValue(resultado);
    }
}
