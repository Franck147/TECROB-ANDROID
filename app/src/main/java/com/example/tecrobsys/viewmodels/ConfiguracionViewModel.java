package com.example.tecrobsys.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.example.tecrobsys.modelos.Tecnico;
import com.example.tecrobsys.red.SupabaseCliente;
import com.example.tecrobsys.red.SupabaseServicio;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfiguracionViewModel extends AndroidViewModel {

    public final MutableLiveData<List<Tecnico>> tecnicos   = new MutableLiveData<>();
    public final MutableLiveData<Boolean>        cargando  = new MutableLiveData<>(false);
    public final MutableLiveData<String>          error    = new MutableLiveData<>();
    public final MutableLiveData<Tecnico>         creado   = new MutableLiveData<>();
    public final MutableLiveData<Boolean>         eliminado = new MutableLiveData<>();

    public ConfiguracionViewModel(@NonNull Application app) { super(app); }

    public void cargarTecnicos(int empresaId) {
        cargando.setValue(true);
        SupabaseCliente.obtenerServicio()
                .listarTecnicos("eq." + empresaId, "nombre.asc")
                .enqueue(new Callback<List<Tecnico>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Tecnico>> call,
                                           @NonNull Response<List<Tecnico>> resp) {
                        cargando.postValue(false);
                        if (resp.isSuccessful() && resp.body() != null)
                            tecnicos.postValue(resp.body());
                        else
                            error.postValue("Error al cargar técnicos");
                    }
                    @Override
                    public void onFailure(@NonNull Call<List<Tecnico>> call, @NonNull Throwable t) {
                        cargando.postValue(false);
                        error.postValue("Sin conexión");
                    }
                });
    }

    public void crearTecnico(int empresaId, String nombre, String apellido,
                              String email, String password, String rol) {
        cargando.setValue(true);

        Map<String, Object> authDatos = new HashMap<>();
        authDatos.put("email",    email);
        authDatos.put("password", password);

        SupabaseCliente.obtenerServicioSignup()
                .registrarUsuario(authDatos)
                .enqueue(new Callback<SupabaseServicio.RespuestaAuth>() {
                    @Override
                    public void onResponse(@NonNull Call<SupabaseServicio.RespuestaAuth> call,
                                           @NonNull Response<SupabaseServicio.RespuestaAuth> resp) {
                        if (!resp.isSuccessful() || resp.body() == null) {
                            cargando.postValue(false);
                            String detalle = "";
                            try {
                                if (resp.errorBody() != null)
                                    detalle = ": " + resp.errorBody().string();
                            } catch (Exception ignored) {}
                            error.postValue("Error al registrar usuario" + detalle);
                            return;
                        }
                        String authUserId = resp.body().getAuthUserId();
                        if (authUserId == null) {
                            cargando.postValue(false);
                            error.postValue("No se pudo obtener el ID del usuario");
                            return;
                        }
                        crearPerfil(empresaId, nombre, apellido, email, rol, authUserId);
                    }
                    @Override
                    public void onFailure(@NonNull Call<SupabaseServicio.RespuestaAuth> call,
                                          @NonNull Throwable t) {
                        cargando.postValue(false);
                        error.postValue("Sin conexión");
                    }
                });
    }

    private void crearPerfil(int empresaId, String nombre, String apellido,
                              String email, String rol, String authUserId) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("empresa_id",   empresaId);
        datos.put("auth_user_id", authUserId);
        datos.put("nombre",       nombre);
        datos.put("apellido",     apellido);
        datos.put("email",        email);
        datos.put("rol",          rol);
        datos.put("activo",       true);

        SupabaseCliente.obtenerServicio()
                .crearTecnico(datos)
                .enqueue(new Callback<Tecnico>() {
                    @Override
                    public void onResponse(@NonNull Call<Tecnico> call,
                                           @NonNull Response<Tecnico> resp) {
                        cargando.postValue(false);
                        if (resp.isSuccessful() && resp.body() != null)
                            creado.postValue(resp.body());
                        else
                            error.postValue("Error al crear perfil del técnico");
                    }
                    @Override
                    public void onFailure(@NonNull Call<Tecnico> call, @NonNull Throwable t) {
                        cargando.postValue(false);
                        error.postValue("Sin conexión");
                    }
                });
    }

    public void eliminarTecnico(int tecnicoId) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("activo", false);

        SupabaseCliente.obtenerServicio()
                .desactivarTecnico("eq." + tecnicoId, datos)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call,
                                           @NonNull Response<Void> resp) {
                        if (resp.isSuccessful()) eliminado.postValue(true);
                        else error.postValue("Error al eliminar técnico");
                    }
                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        error.postValue("Sin conexión");
                    }
                });
    }
}
