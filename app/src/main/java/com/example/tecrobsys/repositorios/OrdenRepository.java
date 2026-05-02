package com.example.tecrobsys.repositorios;

import com.example.tecrobsys.modelos.Equipo;
import com.example.tecrobsys.modelos.Orden;
import com.example.tecrobsys.modelos.Pago;
import com.example.tecrobsys.red.SupabaseCliente;
import java.util.List;
import java.util.Map;
import retrofit2.Callback;

/**
 * OrdenRepository — Capa de datos para órdenes de servicio.
 *
 * Todos los usuarios (administrador y técnico) ven TODAS las
 * órdenes de la empresa. No hay filtro por tecnico_id.
 *
 * No tiene lógica de UI. Solo encapsula las llamadas HTTP a Supabase.
 */
public class OrdenRepository {

    // Selección estándar con JOINs: trae cliente, técnico y equipo
    private static final String SELECCION_COMPLETA =
            "*,"
            + "cliente:cliente_id(nombre,apellido,telefono),"
            + "tecnico:tecnico_id(nombre,apellido),"
            + "equipo(tipo,marca,modelo,desperfecto)";

    // Selección para detalle completo: incluye equipo y servicios aplicados
    private static final String SELECCION_DETALLE =
            "*,"
            + "cliente:cliente_id(nombre,apellido,telefono,email,dni),"
            + "tecnico:tecnico_id(nombre,apellido),"
            + "equipo(*),"
            + "orden_servicio(precio_unitario,cantidad,"
            + "servicio_catalogo:servicio_id(nombre,precio_base))";

    /**
     * Lista todas las órdenes de la empresa, ordenadas por fecha desc.
     */
    public void listarOrdenes(int empresaId, Callback<List<Orden>> callback) {
        SupabaseCliente.obtenerServicio()
                .listarOrdenes(
                        "eq." + empresaId,
                        "created_at.desc",
                        SELECCION_COMPLETA)
                .enqueue(callback);
    }

    /**
     * Lista órdenes filtradas por estado.
     */
    public void listarOrdenesPorEstado(int empresaId, String estado,
                                        Callback<List<Orden>> callback) {
        SupabaseCliente.obtenerServicio()
                .listarOrdenesPorEstado(
                        "eq." + empresaId,
                        "eq." + estado,
                        "created_at.desc",
                        "id,estado")
                .enqueue(callback);
    }

    /**
     * Obtiene una orden específica con todos sus datos (detalle completo).
     */
    public void obtenerOrdenPorId(int ordenId, Callback<Orden> callback) {
        SupabaseCliente.obtenerServicio()
                .obtenerOrdenPorId("eq." + ordenId, SELECCION_DETALLE)
                .enqueue(callback);
    }

    /**
     * Crea una nueva orden.
     */
    public void crearOrden(Map<String, Object> datos, Callback<Orden> callback) {
        SupabaseCliente.obtenerServicio()
                .crearOrden(datos)
                .enqueue(callback);
    }

    /**
     * Actualiza campos de una orden (ej: estado, adelanto, observaciones).
     */
    public void actualizarOrden(int ordenId, Map<String, Object> datos,
                                 Callback<Orden> callback) {
        SupabaseCliente.obtenerServicio()
                .actualizarOrden("eq." + ordenId, datos)
                .enqueue(callback);
    }

    /**
     * Crea el equipo asociado a una orden.
     */
    public void crearEquipo(Map<String, Object> datos, Callback<Equipo> callback) {
        SupabaseCliente.obtenerServicio()
                .crearEquipo(datos)
                .enqueue(callback);
    }

    /**
     * Registra un pago parcial o total para una orden.
     * El trigger en Supabase actualiza orden.adelanto y orden.saldo_pendiente.
     */
    public void crearOrdenServicios(List<Map<String, Object>> datos, Callback<Void> callback) {
        SupabaseCliente.obtenerServicio()
                .crearOrdenServicios(datos)
                .enqueue(callback);
    }

    public void registrarPago(Map<String, Object> datos, Callback<Pago> callback) {
        SupabaseCliente.obtenerServicio()
                .crearPago(datos)
                .enqueue(callback);
    }
}
