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
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * SupabaseServicio — Define todos los endpoints de la API REST de Supabase.
 *
 * Supabase convierte cada tabla de PostgreSQL en endpoints REST:
 *   GET    /rest/v1/orden           → SELECT * FROM orden
 *   POST   /rest/v1/orden           → INSERT INTO orden
 *   PATCH  /rest/v1/orden?id=eq.5   → UPDATE orden WHERE id = 5
 *
 * Filtros usando parámetros de query (@Query):
 *   empresa_id=eq.1        → WHERE empresa_id = 1
 *   estado=eq.pendiente    → WHERE estado = 'pendiente'
 *   select=*,cliente(*)    → JOIN con la tabla cliente
 *   order=created_at.desc  → ORDER BY created_at DESC
 */
public interface SupabaseServicio {

    // ════════════════════════════════════════════════════════
    //  AUTENTICACIÓN
    //  Endpoint: /auth/v1/token?grant_type=password
    // ════════════════════════════════════════════════════════

    /**
     * Inicia sesión con email y contraseña.
     * Retorna el token JWT si las credenciales son correctas.
     * Se usa con obtenerServicioAuth() que apunta a /auth/v1/
     */
    @POST("token?grant_type=password")
    Call<RespuestaAuth> iniciarSesion(@Body Map<String, String> credenciales);

    // ════════════════════════════════════════════════════════
    //  ÓRDENES
    // ════════════════════════════════════════════════════════

    /**
     * Lista todas las órdenes de una empresa.
     * Incluye datos de cliente, técnico y equipo en una sola llamada.
     *
     * Ejemplo de selección con JOIN:
     * "*,cliente:cliente_id(nombre,apellido,telefono),equipo(*)"
     *
     * @param empresaIdFiltro  "eq.1" → WHERE empresa_id = 1
     * @param ordenamiento     "created_at.desc" → ORDER BY created_at DESC
     * @param seleccion        Columnas + relaciones a traer
     */
    @GET("orden")
    Call<List<Orden>> listarOrdenes(
            @Query("empresa_id") String empresaIdFiltro,
            @Query("order") String ordenamiento,
            @Query("select") String seleccion
    );

    /**
     * Filtra órdenes por estado.
     * @param estadoFiltro  "eq.pendiente" | "eq.en_progreso" | "eq.listo"
     */
    @GET("orden")
    Call<List<Orden>> listarOrdenesPorEstado(
            @Query("empresa_id") String empresaIdFiltro,
            @Query("estado") String estadoFiltro,
            @Query("order") String ordenamiento,
            @Query("select") String seleccion
    );

    /**
     * Obtiene una orden específica por su ID.
     * El header "Accept: application/vnd.pgrst.object+json"
     * le dice a Supabase que devuelva un objeto en vez de una lista.
     */
    @GET("orden")
    @Headers("Accept: application/vnd.pgrst.object+json")
    Call<Orden> obtenerOrdenPorId(
            @Query("id") String idFiltro,
            @Query("select") String seleccion
    );

    /**
     * Crea una nueva orden.
     * El trigger en PostgreSQL genera automáticamente el numero_orden.
     */
    @POST("orden")
    Call<Orden> crearOrden(@Body Map<String, Object> datosOrden);

    /**
     * Actualiza campos de una orden (estado, precio, etc.)
     * @param idFiltro  "eq.41" → WHERE id = 41
     */
    @PATCH("orden")
    Call<Orden> actualizarOrden(
            @Query("id") String idFiltro,
            @Body Map<String, Object> datosActualizar
    );

    // ════════════════════════════════════════════════════════
    //  CLIENTES
    // ════════════════════════════════════════════════════════

    /**
     * Lista todos los clientes de una empresa.
     */
    @GET("cliente")
    Call<List<Cliente>> listarClientes(
            @Query("empresa_id") String empresaIdFiltro,
            @Query("order") String ordenamiento
    );

    /**
     * Busca clientes por nombre, apellido o teléfono.
     * Usa el operador "or" de Supabase para búsqueda múltiple.
     *
     * Ejemplo de filtroOr:
     * "(nombre.ilike.*carlos*,apellido.ilike.*carlos*,telefono.ilike.*987*)"
     */
    @GET("cliente")
    Call<List<Cliente>> buscarClientes(
            @Query("empresa_id") String empresaIdFiltro,
            @Query("or") String filtroOr
    );

    /**
     * Crea un nuevo cliente.
     */
    @POST("cliente")
    Call<Cliente> crearCliente(@Body Map<String, Object> datosCliente);

    // ════════════════════════════════════════════════════════
    //  EQUIPOS
    // ════════════════════════════════════════════════════════

    /**
     * Crea el equipo asociado a una orden.
     * Se llama inmediatamente después de crear la orden.
     */
    @POST("equipo")
    Call<Equipo> crearEquipo(@Body Map<String, Object> datosEquipo);

    // ════════════════════════════════════════════════════════
    //  CATÁLOGO DE SERVICIOS
    // ════════════════════════════════════════════════════════

    /**
     * Lista todos los servicios activos del catálogo.
     * @param activoFiltro  "eq.true" → WHERE activo = true
     */
    @GET("servicio_catalogo")
    Call<List<ServicioCatalogo>> listarServicios(
            @Query("empresa_id") String empresaIdFiltro,
            @Query("activo") String activoFiltro,
            @Query("order") String ordenamiento
    );

    /**
     * Filtra servicios por categoría.
     * @param categoriaFiltro "eq.mantenimiento" | "eq.software" | etc.
     */
    @GET("servicio_catalogo")
    Call<List<ServicioCatalogo>> listarServiciosPorCategoria(
            @Query("empresa_id") String empresaIdFiltro,
            @Query("categoria") String categoriaFiltro,
            @Query("activo") String activoFiltro,
            @Query("order") String ordenamiento
    );

    // ════════════════════════════════════════════════════════
    //  CLASE: RESPUESTA DE AUTENTICACIÓN
    //  Mapea el JSON que devuelve Supabase al hacer login
    // ════════════════════════════════════════════════════════

    class RespuestaAuth {

        // El token JWT que usaremos en todos los requests siguientes
        @SerializedName("access_token")
        public String tokenAcceso;

        // Tipo de token (siempre "bearer")
        @SerializedName("token_type")
        public String tipoToken;

        // Segundos hasta que expira el token
        @SerializedName("expires_in")
        public int expiraEn;

        // Datos básicos del usuario autenticado
        @SerializedName("user")
        public UsuarioAuth usuario;

        public static class UsuarioAuth {

            // ID único del usuario en Supabase Auth
            @SerializedName("id")
            public String id;

            // Email del usuario
            @SerializedName("email")
            public String email;
        }
    }
}