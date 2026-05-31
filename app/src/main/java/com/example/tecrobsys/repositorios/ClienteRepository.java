package com.example.tecrobsys.repositorios;

import com.example.tecrobsys.modelos.Cliente;
import com.example.tecrobsys.red.SupabaseCliente;
import java.util.List;
import java.util.Map;
import retrofit2.Callback;

/**
 * ClienteRepository — Capa de datos para clientes del taller.
 *
 * No tiene lógica de UI. Solo encapsula las llamadas HTTP a Supabase.
 */
public class ClienteRepository {

    /**
     * Busca clientes por nombre, apellido o teléfono (búsqueda parcial).
     *
     * @param empresaId ID de la empresa
     * @param texto     Texto de búsqueda (mínimo 2 caracteres)
     * @param callback  Recibe la lista de clientes encontrados
     */
    public void buscarClientes(int empresaId, String texto,
                                Callback<List<Cliente>> callback) {
        String filtroOr = "(nombre.ilike.*" + texto + "*,"
                + "apellido.ilike.*" + texto + "*,"
                + "telefono.ilike.*" + texto + "*)";

        SupabaseCliente.obtenerServicio()
                .buscarClientes("eq." + empresaId, filtroOr)
                .enqueue(callback);
    }

    /**
     * Busca un cliente por DNI exacto.
     */
    public void buscarClientePorDni(int empresaId, String dni,
                                     Callback<List<Cliente>> callback) {
        SupabaseCliente.obtenerServicio()
                .buscarClientePorDni("eq." + empresaId, "eq." + dni)
                .enqueue(callback);
    }

    /**
     * Actualiza campos de un cliente existente.
     */
    public void actualizarCliente(int clienteId, Map<String, Object> datos,
                                   Callback<Void> callback) {
        SupabaseCliente.obtenerServicio()
                .actualizarCliente("eq." + clienteId, datos)
                .enqueue(callback);
    }

    /**
     * Crea un nuevo cliente en la base de datos.
     *
     * @param datos    Mapa con los campos del cliente
     * @param callback Recibe el cliente creado (o null si Supabase no devuelve body)
     */
    public void crearCliente(Map<String, Object> datos,
                              Callback<Cliente> callback) {
        SupabaseCliente.obtenerServicio()
                .crearCliente(datos)
                .enqueue(callback);
    }
}
