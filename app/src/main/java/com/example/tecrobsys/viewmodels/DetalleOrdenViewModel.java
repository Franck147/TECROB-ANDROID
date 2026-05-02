package com.example.tecrobsys.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.tecrobsys.modelos.Orden;
import com.example.tecrobsys.modelos.Pago;
import com.example.tecrobsys.repositorios.OrdenRepository;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * DetalleOrdenViewModel — Lógica del detalle de una orden.
 *
 * Maneja:
 *   - Carga de la orden completa con JOINs (cliente, equipo, técnico)
 *   - Actualización del estado desde el selector o el botón de completada
 */
public class DetalleOrdenViewModel extends ViewModel {

    public final MutableLiveData<Orden>   orden             = new MutableLiveData<>();
    public final MutableLiveData<Boolean> cargando          = new MutableLiveData<>(false);
    public final MutableLiveData<String>  error             = new MutableLiveData<>();
    public final MutableLiveData<Boolean> estadoActualizado = new MutableLiveData<>();
    public final MutableLiveData<Boolean> pagoRegistrado    = new MutableLiveData<>();

    private final OrdenRepository repo = new OrdenRepository();

    /**
     * Carga la orden completa desde Supabase.
     * Incluye cliente (con DNI y email), técnico y todos los campos del equipo.
     */
    public void cargarOrden(int ordenId) {
        cargando.setValue(true);
        error.setValue(null);

        repo.obtenerOrdenPorId(ordenId, new Callback<Orden>() {
            @Override
            public void onResponse(@NonNull Call<Orden> c,
                                   @NonNull Response<Orden> r) {
                cargando.postValue(false);
                if (r.isSuccessful() && r.body() != null) {
                    orden.postValue(r.body());
                } else {
                    error.postValue("No se pudo cargar la orden");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Orden> c, @NonNull Throwable e) {
                cargando.postValue(false);
                error.postValue("Sin conexión a internet");
            }
        });
    }

    /**
     * Actualiza el estado de la orden en Supabase.
     * Al éxito, actualiza el LiveData orden localmente (sin recargar)
     * y postea estadoActualizado = true como evento.
     */
    public void actualizarEstado(int ordenId, String nuevoEstado) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("estado", nuevoEstado);

        repo.actualizarOrden(ordenId, datos, new Callback<Orden>() {
            @Override
            public void onResponse(@NonNull Call<Orden> c,
                                   @NonNull Response<Orden> r) {
                if (r.isSuccessful()) {
                    // Actualizar el estado localmente sin recargar toda la orden
                    Orden actual = orden.getValue();
                    if (actual != null) {
                        actual.setEstado(nuevoEstado);
                        orden.postValue(actual);
                    }
                    estadoActualizado.postValue(true);
                } else {
                    error.postValue("Error al actualizar el estado");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Orden> c, @NonNull Throwable e) {
                error.postValue("Sin conexión a internet");
            }
        });
    }

    /**
     * Registra un pago parcial o total para la orden.
     * Actualiza adelanto y saldo_pendiente localmente sin recargar.
     * El trigger en Supabase también los actualiza en la BD.
     */
    public void registrarPago(int ordenId, double monto, String metodo, String nota) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("orden_id", ordenId);
        datos.put("monto", monto);
        datos.put("metodo", metodo);
        if (nota != null && !nota.isEmpty()) datos.put("nota", nota);

        repo.registrarPago(datos, new Callback<Pago>() {
            @Override
            public void onResponse(@NonNull Call<Pago> c,
                                   @NonNull Response<Pago> r) {
                if (r.isSuccessful()) {
                    Orden actual = orden.getValue();
                    if (actual != null) {
                        double nuevoAdelanto = actual.getAdelanto() + monto;
                        actual.setAdelanto(nuevoAdelanto);
                        actual.setSaldoPendiente(
                                actual.getSubtotal() - actual.getDescuento() - nuevoAdelanto);
                        orden.postValue(actual);
                    }
                    pagoRegistrado.postValue(true);
                } else {
                    error.postValue("Error al registrar el pago");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Pago> c, @NonNull Throwable e) {
                error.postValue("Sin conexión a internet");
            }
        });
    }
}
