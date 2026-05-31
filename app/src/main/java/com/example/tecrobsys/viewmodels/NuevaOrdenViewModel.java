package com.example.tecrobsys.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.tecrobsys.modelos.Cliente;
import com.example.tecrobsys.modelos.Equipo;
import com.example.tecrobsys.modelos.Orden;
import com.example.tecrobsys.modelos.ServicioCatalogo;
import com.example.tecrobsys.repositorios.ClienteRepository;
import com.example.tecrobsys.repositorios.OrdenRepository;
import com.example.tecrobsys.repositorios.ServicioRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NuevaOrdenViewModel extends ViewModel {

    public final MutableLiveData<List<ServicioCatalogo>> catalogoDisponible     = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Boolean>               guardando              = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean>               ordenGuardada          = new MutableLiveData<>();
    public final MutableLiveData<String>                error                  = new MutableLiveData<>();
    public final MutableLiveData<Cliente>               clienteEncontrado      = new MutableLiveData<>();
    public final MutableLiveData<Boolean>               clienteNoEncontrado    = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean>               buscandoDni            = new MutableLiveData<>(false);
    public final MutableLiveData<Cliente>               clienteCreadoRegistrado = new MutableLiveData<>();
    public final MutableLiveData<Boolean>               registrandoCliente     = new MutableLiveData<>(false);

    private final ClienteRepository  clienteRepo  = new ClienteRepository();
    private final OrdenRepository    ordenRepo    = new OrdenRepository();
    private final ServicioRepository servicioRepo = new ServicioRepository();

    public void buscarClientePorDni(int empresaId, String dni) {
        buscandoDni.setValue(true);
        clienteEncontrado.setValue(null);
        clienteNoEncontrado.setValue(false);
        clienteRepo.buscarClientePorDni(empresaId, dni, new Callback<List<Cliente>>() {
            @Override
            public void onResponse(@NonNull Call<List<Cliente>> c,
                                   @NonNull Response<List<Cliente>> r) {
                buscandoDni.postValue(false);
                if (r.isSuccessful() && r.body() != null && !r.body().isEmpty()) {
                    clienteEncontrado.postValue(r.body().get(0));
                } else {
                    clienteNoEncontrado.postValue(true);
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Cliente>> c, @NonNull Throwable e) {
                buscandoDni.postValue(false);
                clienteNoEncontrado.postValue(true);
            }
        });
    }

    public void actualizarTelefonoCliente(int clienteId, String nuevoTelefono) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("telefono", nuevoTelefono);
        clienteRepo.actualizarCliente(clienteId, datos, new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> r) {}
            @Override
            public void onFailure(@NonNull Call<Void> c, @NonNull Throwable e) {}
        });
    }

    public void registrarCliente(Map<String, Object> datos) {
        registrandoCliente.setValue(true);
        final String nombre   = datos.get("nombre")   != null ? String.valueOf(datos.get("nombre"))   : "";
        final String apellido = datos.get("apellido") != null ? String.valueOf(datos.get("apellido")) : "";
        final String telefono = datos.get("telefono") != null ? String.valueOf(datos.get("telefono")) : "";
        final String dni      = datos.get("dni")      != null ? String.valueOf(datos.get("dni"))      : "";
        clienteRepo.crearCliente(datos, new Callback<Cliente>() {
            @Override
            public void onResponse(@NonNull Call<Cliente> c, @NonNull Response<Cliente> r) {
                registrandoCliente.postValue(false);
                if (r.code() == 201 || r.isSuccessful()) {
                    Cliente resultado = r.body();
                    if (resultado == null) {
                        resultado = new Cliente();
                        resultado.setNombre(nombre);
                        resultado.setApellido(apellido);
                        resultado.setTelefono(telefono);
                        if (!dni.isEmpty()) resultado.setDni(dni);
                    }
                    clienteCreadoRegistrado.postValue(resultado);
                } else {
                    error.postValue("Error al registrar el cliente");
                }
            }
            @Override
            public void onFailure(@NonNull Call<Cliente> c, @NonNull Throwable e) {
                registrandoCliente.postValue(false);
                error.postValue("Sin conexión a internet");
            }
        });
    }

    public void cargarCatalogo(int empresaId) {
        servicioRepo.listarServicios(empresaId,
                new Callback<List<ServicioCatalogo>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ServicioCatalogo>> c,
                                           @NonNull Response<List<ServicioCatalogo>> r) {
                        if (r.isSuccessful() && r.body() != null)
                            catalogoDisponible.postValue(r.body());
                    }
                    @Override
                    public void onFailure(@NonNull Call<List<ServicioCatalogo>> c,
                                          @NonNull Throwable e) {}
                });
    }

    /**
     * Guarda la orden en tres pasos encadenados:
     *   1. Crear orden         → obtener ordenId
     *   2. Crear equipo        → asociar al ordenId
     *   3. Crear orden_servicio → uno por cada servicio seleccionado
     */
    public void guardarOrden(int empresaId, int tecnicoId,
                              Map<String, Object> datosOrden,
                              Map<String, Object> datosEquipo,
                              List<ServicioCatalogo> servicios) {
        guardando.setValue(true);
        error.setValue(null);

        ordenRepo.crearOrden(datosOrden, new Callback<Orden>() {
            @Override
            public void onResponse(@NonNull Call<Orden> c, @NonNull Response<Orden> r) {
                if ((r.code() == 201 || r.isSuccessful()) && r.body() != null
                        && r.body().getId() > 0) {
                    int ordenId = r.body().getId();
                    datosEquipo.put("orden_id", ordenId);
                    crearEquipo(datosEquipo, ordenId, servicios);
                } else {
                    guardando.postValue(false);
                    error.postValue("Error al crear la orden. Intenta de nuevo.");
                }
            }
            @Override
            public void onFailure(@NonNull Call<Orden> c, @NonNull Throwable e) {
                guardando.postValue(false);
                error.postValue("Sin conexión a internet");
            }
        });
    }

    private void crearEquipo(Map<String, Object> datosEquipo,
                              int ordenId,
                              List<ServicioCatalogo> servicios) {
        ordenRepo.crearEquipo(datosEquipo, new Callback<Equipo>() {
            @Override
            public void onResponse(@NonNull Call<Equipo> c, @NonNull Response<Equipo> r) {
                if (servicios != null && !servicios.isEmpty()) {
                    crearOrdenServicios(ordenId, servicios);
                } else {
                    guardando.postValue(false);
                    ordenGuardada.postValue(true);
                }
            }
            @Override
            public void onFailure(@NonNull Call<Equipo> c, @NonNull Throwable e) {
                // Equipo falló pero la orden ya existe — navegar igual
                guardando.postValue(false);
                ordenGuardada.postValue(true);
            }
        });
    }

    private void crearOrdenServicios(int ordenId, List<ServicioCatalogo> servicios) {
        List<Map<String, Object>> filas = new ArrayList<>();
        for (ServicioCatalogo s : servicios) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("orden_id",        ordenId);
            fila.put("servicio_id",     s.getId());
            fila.put("precio_unitario", s.getPrecioBase());
            fila.put("cantidad",        1);
            filas.add(fila);
        }

        ordenRepo.crearOrdenServicios(filas, new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> r) {
                guardando.postValue(false);
                ordenGuardada.postValue(true);
            }
            @Override
            public void onFailure(@NonNull Call<Void> c, @NonNull Throwable e) {
                // Servicios fallaron pero la orden existe — navegar igual
                guardando.postValue(false);
                ordenGuardada.postValue(true);
            }
        });
    }
}
