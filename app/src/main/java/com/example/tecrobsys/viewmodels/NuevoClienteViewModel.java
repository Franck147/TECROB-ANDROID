package com.example.tecrobsys.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.tecrobsys.modelos.Cliente;
import com.example.tecrobsys.repositorios.ClienteRepository;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * NuevoClienteViewModel — Lógica para crear un nuevo cliente.
 *
 * Maneja el caso especial de Supabase donde el POST devuelve HTTP 201
 * pero el body puede ser null. En ese caso se construye un Cliente
 * temporal con los datos enviados para que el callback funcione.
 */
public class NuevoClienteViewModel extends ViewModel {

    // Evento puntual: cliente creado exitosamente
    public final MutableLiveData<Cliente> clienteCreado = new MutableLiveData<>();
    public final MutableLiveData<Boolean> guardando     = new MutableLiveData<>(false);
    public final MutableLiveData<String>  error         = new MutableLiveData<>();

    private final ClienteRepository repo = new ClienteRepository();

    /**
     * Crea un nuevo cliente en Supabase.
     *
     * Si Supabase devuelve body null (comportamiento conocido con ciertos
     * headers), construye un Cliente temporal con los datos enviados
     * para que la UI pueda continuar.
     *
     * @param datos Mapa con: empresa_id, nombre, apellido, telefono, dni, email, direccion
     */
    public void crearCliente(Map<String, Object> datos) {
        guardando.setValue(true);
        error.setValue(null);

        // Guardar los datos por si el body viene null
        final String nombre   = datos.get("nombre")   != null ? String.valueOf(datos.get("nombre"))   : "";
        final String apellido = datos.get("apellido") != null ? String.valueOf(datos.get("apellido")) : "";
        final String telefono = datos.get("telefono") != null ? String.valueOf(datos.get("telefono")) : "";

        repo.crearCliente(datos, new Callback<Cliente>() {
            @Override
            public void onResponse(@NonNull Call<Cliente> c,
                                   @NonNull Response<Cliente> r) {
                guardando.postValue(false);
                if (r.code() == 201 || r.isSuccessful()) {
                    Cliente resultado = r.body();
                    if (resultado == null) {
                        // Body null — construir cliente temporal con los datos enviados
                        resultado = new Cliente();
                        resultado.setNombre(nombre);
                        resultado.setApellido(apellido);
                        resultado.setTelefono(telefono);
                    }
                    clienteCreado.postValue(resultado);
                } else {
                    error.postValue("Error del servidor");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Cliente> c, @NonNull Throwable e) {
                guardando.postValue(false);
                error.postValue("Sin conexión a internet");
            }
        });
    }
}
