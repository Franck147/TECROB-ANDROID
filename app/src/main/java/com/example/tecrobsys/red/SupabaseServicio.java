package com.example.tecrobsys.red;

import com.example.tecrobsys.modelos.Cliente;
import com.example.tecrobsys.modelos.Equipo;
import com.example.tecrobsys.modelos.Orden;
import com.example.tecrobsys.modelos.Pago;
import com.example.tecrobsys.modelos.ServicioCatalogo;
import com.example.tecrobsys.modelos.Tecnico;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseServicio {

    // ════════════════════════════════════════════════════════
    //  AUTENTICACIÓN
    // ════════════════════════════════════════════════════════

    @POST("token?grant_type=password")
    Call<RespuestaAuth> iniciarSesion(@Body Map<String, String> credenciales);

    // ════════════════════════════════════════════════════════
    //  ÓRDENES
    // ════════════════════════════════════════════════════════

    @GET("orden")
    Call<List<Orden>> listarOrdenes(
            @Query("empresa_id") String empresaIdFiltro,
            @Query("order") String ordenamiento,
            @Query("select") String seleccion
    );

    @GET("orden")
    Call<List<Orden>> listarOrdenesPorEstado(
            @Query("empresa_id") String empresaIdFiltro,
            @Query("estado") String estadoFiltro,
            @Query("order") String ordenamiento,
            @Query("select") String seleccion
    );

    @GET("orden")
    @Headers("Accept: application/vnd.pgrst.object+json")
    Call<Orden> obtenerOrdenPorId(
            @Query("id") String idFiltro,
            @Query("select") String seleccion
    );

    @POST("orden")
    @Headers("Accept: application/vnd.pgrst.object+json")
    Call<Orden> crearOrden(@Body Map<String, Object> datosOrden);

    @PATCH("orden")
    Call<Orden> actualizarOrden(
            @Query("id") String idFiltro,
            @Body Map<String, Object> datosActualizar
    );

    // ════════════════════════════════════════════════════════
    //  CLIENTES
    // ════════════════════════════════════════════════════════

    @GET("cliente")
    Call<List<Cliente>> listarClientes(
            @Query("empresa_id") String empresaIdFiltro,
            @Query("order") String ordenamiento
    );

    @GET("cliente")
    Call<List<Cliente>> buscarClientes(
            @Query("empresa_id") String empresaIdFiltro,
            @Query("or") String filtroOr
    );

    @GET("cliente")
    Call<List<Cliente>> buscarClientePorDni(
            @Query("empresa_id") String empresaIdFiltro,
            @Query("dni") String dniFiltro
    );

    @PATCH("cliente")
    @Headers("Prefer: return=minimal")
    Call<Void> actualizarCliente(
            @Query("id") String idFiltro,
            @Body Map<String, Object> datos
    );

    @POST("cliente")
    @Headers("Accept: application/vnd.pgrst.object+json")
    Call<Cliente> crearCliente(@Body Map<String, Object> datosCliente);

    // ════════════════════════════════════════════════════════
    //  EQUIPOS
    // ════════════════════════════════════════════════════════

    @POST("equipo")
    @Headers("Accept: application/vnd.pgrst.object+json")
    Call<Equipo> crearEquipo(@Body Map<String, Object> datosEquipo);

    @POST("orden_servicio")
    @Headers("Prefer: return=minimal")
    Call<Void> crearOrdenServicios(@Body List<Map<String, Object>> datos);

    // ════════════════════════════════════════════════════════
    //  PAGOS
    // ════════════════════════════════════════════════════════

    @POST("pago")
    Call<Pago> crearPago(@Body Map<String, Object> datosPago);

    // ════════════════════════════════════════════════════════
    //  CATÁLOGO DE SERVICIOS
    // ════════════════════════════════════════════════════════

    @GET("servicio_catalogo")
    Call<List<ServicioCatalogo>> listarServicios(
            @Query("empresa_id") String empresaIdFiltro,
            @Query("activo") String activoFiltro,
            @Query("order") String ordenamiento
    );

    @GET("servicio_catalogo")
    Call<List<ServicioCatalogo>> listarServiciosPorCategoria(
            @Query("empresa_id") String empresaIdFiltro,
            @Query("categoria") String categoriaFiltro,
            @Query("activo") String activoFiltro,
            @Query("order") String ordenamiento
    );

    // ── CRUD Catálogo ─────────────────────────────────────────────

    /** Agregar nuevo servicio al catálogo */
    @POST("servicio_catalogo")
    @Headers("Accept: application/vnd.pgrst.object+json")
    Call<ServicioCatalogo> agregarServicio(
            @Body Map<String, Object> datos);

    /** Editar un servicio existente por su ID */
    @PATCH("servicio_catalogo")
    Call<ServicioCatalogo> actualizarServicio(
            @Query("id") String idFiltro,
            @Body Map<String, Object> datos);

    /** Eliminar un servicio por su ID */
    @DELETE("servicio_catalogo")
    Call<Void> eliminarServicio(
            @Query("id") String idFiltro);

    // ════════════════════════════════════════════════════════
    //  TÉCNICOS / PERFILES DE USUARIO
    // ════════════════════════════════════════════════════════

    /**
     * Obtiene el perfil del técnico por su auth_user_id (UUID de Supabase Auth).
     * Usar con el prefijo "eq." → "eq.uuid-aqui"
     * Se llama después del login para obtener id real, nombre y rol.
     */
    @GET("tecnico")
    Call<List<Tecnico>> obtenerPerfilPorAuthId(
            @Query("auth_user_id") String authIdFiltro);

    /** @deprecated Usar obtenerPerfilPorAuthId — más confiable que buscar por email */
    @GET("tecnico")
    Call<List<Tecnico>> obtenerPerfilPorEmail(
            @Query("email") String emailFiltro);

    @GET("tecnico")
    Call<List<Tecnico>> listarTecnicos(
            @Query("empresa_id") String empresaIdFiltro,
            @Query("order") String orden);

    @POST("tecnico")
    @Headers("Accept: application/vnd.pgrst.object+json")
    Call<Tecnico> crearTecnico(@Body Map<String, Object> datos);

    @PATCH("tecnico")
    @Headers("Prefer: return=minimal")
    Call<Void> desactivarTecnico(
            @Query("id") String idFiltro,
            @Body Map<String, Object> datos);

    @POST("signup")
    Call<RespuestaAuth> registrarUsuario(@Body Map<String, Object> datos);

    // ════════════════════════════════════════════════════════
    //  RESPUESTA DE AUTENTICACIÓN
    // ════════════════════════════════════════════════════════

    class RespuestaAuth {

        @SerializedName("access_token")
        public String tokenAcceso;

        @SerializedName("token_type")
        public String tipoToken;

        @SerializedName("expires_in")
        public int expiraEn;

        @SerializedName("user")
        public UsuarioAuth usuario;

        // Cuando la confirmación de email está ACTIVADA en Supabase,
        // el signup devuelve el usuario directamente en la raíz (sin envoltura "user")
        @SerializedName("id")
        public String id;

        /** Devuelve el auth_user_id sin importar el formato de respuesta */
        public String getAuthUserId() {
            if (usuario != null && usuario.id != null) return usuario.id;
            return id;
        }

        public static class UsuarioAuth {
            @SerializedName("id")
            public String id;

            @SerializedName("email")
            public String email;
        }
    }
}