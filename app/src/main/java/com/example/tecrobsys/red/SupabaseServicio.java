package com.example.tecrobsys.red;

import com.example.tecrobsys.modelos.Cliente;
import com.example.tecrobsys.modelos.Equipo;
import com.example.tecrobsys.modelos.Orden;
import com.example.tecrobsys.modelos.ServicioCatalogo;
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

    @POST("cliente")
    Call<Cliente> crearCliente(@Body Map<String, Object> datosCliente);

    // ════════════════════════════════════════════════════════
    //  EQUIPOS
    // ════════════════════════════════════════════════════════

    @POST("equipo")
    Call<Equipo> crearEquipo(@Body Map<String, Object> datosEquipo);

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

        public static class UsuarioAuth {
            @SerializedName("id")
            public String id;

            @SerializedName("email")
            public String email;
        }
    }
}