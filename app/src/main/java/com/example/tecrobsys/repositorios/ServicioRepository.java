package com.example.tecrobsys.repositorios;

import com.example.tecrobsys.modelos.ServicioCatalogo;
import com.example.tecrobsys.red.SupabaseCliente;
import java.util.List;
import java.util.Map;
import retrofit2.Callback;

/**
 * ServicioRepository — Capa de datos para el catálogo de servicios y repuestos.
 *
 * Solo lista servicios activos (activo = true), ordenados por nombre.
 * No tiene lógica de UI. Solo encapsula las llamadas HTTP a Supabase.
 */
public class ServicioRepository {

    /**
     * Lista todos los servicios activos de la empresa, ordenados por nombre.
     */
    public void listarServicios(int empresaId,
                                 Callback<List<ServicioCatalogo>> callback) {
        SupabaseCliente.obtenerServicio()
                .listarServicios(
                        "eq." + empresaId,
                        "eq.true",
                        "nombre.asc")
                .enqueue(callback);
    }

    /**
     * Agrega un nuevo servicio al catálogo.
     */
    public void agregarServicio(Map<String, Object> datos,
                                 Callback<ServicioCatalogo> callback) {
        SupabaseCliente.obtenerServicio()
                .agregarServicio(datos)
                .enqueue(callback);
    }

    /**
     * Actualiza los datos de un servicio existente.
     *
     * @param id       ID del servicio a actualizar
     * @param datos    Campos a modificar (nombre, precio, categoría, descripción)
     * @param callback Recibe el servicio actualizado
     */
    public void actualizarServicio(int id, Map<String, Object> datos,
                                    Callback<ServicioCatalogo> callback) {
        SupabaseCliente.obtenerServicio()
                .actualizarServicio("eq." + id, datos)
                .enqueue(callback);
    }

    /**
     * Elimina un servicio del catálogo.
     *
     * @param id       ID del servicio a eliminar
     * @param callback Recibe Void (sin body en DELETE exitoso)
     */
    public void eliminarServicio(int id, Callback<Void> callback) {
        SupabaseCliente.obtenerServicio()
                .eliminarServicio("eq." + id)
                .enqueue(callback);
    }
}
