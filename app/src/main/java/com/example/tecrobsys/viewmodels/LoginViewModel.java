package com.example.tecrobsys.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.example.tecrobsys.modelos.Tecnico;
import com.example.tecrobsys.red.SupabaseCliente;
import com.example.tecrobsys.red.SupabaseServicio;
import com.example.tecrobsys.repositorios.AuthRepository;
import com.example.tecrobsys.utils.SesionManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * LoginViewModel — Lógica de autenticación para ActividadLogin.
 *
 * Flujo del login:
 *   1. iniciarSesion(email, password) → Supabase Auth → token JWT
 *   2. obtenerPerfilTecnico(email)    → tabla "tecnico" → id, nombre, rol
 *   3. SesionManager.guardarSesion()  → guarda todo en prefs cifradas
 *   4. postea en perfilTecnico        → Activity navega a Principal
 *
 * Si la tabla "tecnico" no tiene el email (degradado gracioso):
 *   → usa valores por defecto y continúa con rol "tecnico"
 *
 * Extiende AndroidViewModel para poder acceder al Context
 * y obtener la instancia de SesionManager.
 */
public class LoginViewModel extends AndroidViewModel {

    public final MutableLiveData<Boolean> cargando      = new MutableLiveData<>(false);
    public final MutableLiveData<String>  error         = new MutableLiveData<>();
    public final MutableLiveData<Tecnico> perfilTecnico = new MutableLiveData<>();

    private final AuthRepository  authRepo;
    private final SesionManager   sesion;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        authRepo = new AuthRepository();
        sesion   = SesionManager.obtenerInstancia(application);
    }

    /**
     * Ejecuta el login en dos pasos:
     * primero autentica, luego obtiene el perfil del técnico.
     */
    public void iniciarSesion(String email, String password) {
        cargando.setValue(true);
        error.setValue(null);

        authRepo.iniciarSesion(email, password,
                new Callback<SupabaseServicio.RespuestaAuth>() {
                    @Override
                    public void onResponse(@NonNull Call<SupabaseServicio.RespuestaAuth> c,
                                           @NonNull Response<SupabaseServicio.RespuestaAuth> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            String token      = r.body().tokenAcceso;
                            String authUserId = r.body().usuario != null
                                    ? r.body().usuario.id : null;
                            // Registrar token para todos los requests siguientes
                            SupabaseCliente.establecerToken(token);
                            // Paso 2: obtener perfil real del técnico por su UUID
                            obtenerPerfilTecnico(token, email, authUserId);
                        } else {
                            cargando.postValue(false);
                            error.postValue("Email o contraseña incorrectos");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SupabaseServicio.RespuestaAuth> c,
                                          @NonNull Throwable e) {
                        cargando.postValue(false);
                        error.postValue("Sin conexión a internet");
                    }
                });
    }

    /**
     * Obtiene el perfil del técnico después de autenticar.
     * Usa auth_user_id (UUID) para la búsqueda — más confiable que email.
     * Si el perfil no se encuentra, hace degradado gracioso con rol "tecnico".
     */
    private void obtenerPerfilTecnico(String token, String email, String authUserId) {
        Callback<List<Tecnico>> callbackPerfil = new Callback<List<Tecnico>>() {
            @Override
            public void onResponse(@NonNull Call<List<Tecnico>> c,
                                   @NonNull Response<List<Tecnico>> r) {
                cargando.postValue(false);
                if (r.isSuccessful() && r.body() != null && !r.body().isEmpty()) {
                    Tecnico tecnico = r.body().get(0);
                    sesion.guardarSesion(
                            token,
                            tecnico.getEmpresaId(),
                            tecnico.getId(),
                            tecnico.getNombreCompleto(),
                            tecnico.getEmail() != null ? tecnico.getEmail() : email,
                            tecnico.getRol());
                    perfilTecnico.postValue(tecnico);
                } else {
                    guardarSesionFallback(token, email);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Tecnico>> c, @NonNull Throwable e) {
                cargando.postValue(false);
                guardarSesionFallback(token, email);
            }
        };

        if (authUserId != null && !authUserId.isEmpty()) {
            authRepo.obtenerPerfilPorAuthId(authUserId, callbackPerfil);
        } else {
            authRepo.obtenerPerfilPorEmail(email, callbackPerfil);
        }
    }

    /**
     * Guarda una sesión mínima cuando el perfil no se puede obtener de la BD.
     * Usa la parte local del email como nombre visible (ej: "adler147")
     * en lugar del email completo.
     */
    private void guardarSesionFallback(String token, String email) {
        String nombreFallback = email.contains("@")
                ? email.substring(0, email.indexOf('@'))
                : email;
        sesion.guardarSesion(token, 1, 1, nombreFallback, email, "tecnico");
        Tecnico tecnicoDefault = new Tecnico();
        tecnicoDefault.setEmail(email);
        tecnicoDefault.setRol("tecnico");
        perfilTecnico.postValue(tecnicoDefault);
    }

    /** Limpia el mensaje de error (ej: cuando el usuario empieza a corregir el campo). */
    public void limpiarError() {
        error.setValue(null);
    }
}
