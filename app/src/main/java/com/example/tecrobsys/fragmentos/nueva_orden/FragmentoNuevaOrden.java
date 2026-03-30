package com.example.tecrobsys.fragmentos.nueva_orden;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.snackbar.Snackbar;
import com.example.tecrobsys.R;
import com.example.tecrobsys.actividades.ActividadPrincipal;
import com.example.tecrobsys.databinding.FragmentoNuevaOrdenBinding;
import com.example.tecrobsys.modelos.Cliente;
import com.example.tecrobsys.modelos.Equipo;
import com.example.tecrobsys.modelos.Orden;
import com.example.tecrobsys.red.SupabaseCliente;
import com.example.tecrobsys.utils.SesionManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * FragmentoNuevaOrden — Formulario para crear una orden de servicio.
 *
 * Flujo:
 *   1. Buscar cliente existente o registrar uno nuevo
 *   2. Seleccionar tipo de equipo
 *   3. Ingresar marca, modelo, serie
 *   4. Describir el desperfecto
 *   5. Seleccionar prioridad
 *   6. Marcar accesorios incluidos
 *   7. Guardar → POST /orden + POST /equipo en Supabase
 *   8. Navegar a la lista de órdenes con mensaje de éxito
 */
public class FragmentoNuevaOrden extends Fragment {

    private FragmentoNuevaOrdenBinding enlace;

    // Cliente seleccionado del autocompletado
    private Cliente clienteSeleccionado = null;

    // Lista de clientes para el autocompletado
    private final List<Cliente> listaClientes = new ArrayList<>();

    // Valores seleccionados en los chips
    private String tipoEquipo = "laptop";
    private String prioridad  = "normal";
    private final List<String> accesorios = new ArrayList<>();

    private int empresaId;
    private int tecnicoId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador,
                             @Nullable ViewGroup contenedor,
                             @Nullable Bundle estadoGuardado) {
        enlace = FragmentoNuevaOrdenBinding.inflate(inflador, contenedor, false);
        return enlace.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View vista,
                              @Nullable Bundle estadoGuardado) {
        super.onViewCreated(vista, estadoGuardado);

        SesionManager sesion = SesionManager.obtenerInstancia(requireContext());
        empresaId = sesion.obtenerEmpresaId();
        tecnicoId = sesion.obtenerTecnicoId();

        configurarBuscadorCliente();
        configurarChipsEquipo();
        configurarChipsPrioridad();
        configurarChipsAccesorios();
        configurarBotones();
    }

    /**
     * Configura el campo de búsqueda de cliente con autocompletado.
     * Busca en Supabase cuando el usuario escribe 2+ caracteres.
     */
    private void configurarBuscadorCliente() {
        // El ID en XML es "campo_cliente" → ViewBinding lo convierte en "campoCliente"
        enlace.campoCliente.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable e) {}

            @Override
            public void onTextChanged(CharSequence texto, int i, int a, int c) {
                if (texto.length() >= 2) {
                    buscarClientes(texto.toString());
                }
                if (texto.length() == 0) {
                    clienteSeleccionado = null;
                }
            }
        });

        // Al seleccionar una sugerencia del dropdown
        enlace.campoCliente.setOnItemClickListener((parent, v, pos, id) -> {
            if (pos < listaClientes.size()) {
                clienteSeleccionado = listaClientes.get(pos);
                enlace.campoCliente.setText(
                        clienteSeleccionado.getNombreCompleto());
            }
        });

        // Botón registrar nuevo cliente → abre el BottomSheet
        enlace.botonNuevoCliente.setOnClickListener(v ->
                mostrarDialogoNuevoCliente());
    }

    /**
     * Busca clientes en Supabase por nombre, apellido o teléfono.
     */
    private void buscarClientes(String texto) {
        String filtroOr = "(nombre.ilike.*" + texto + "*,"
                + "apellido.ilike.*" + texto + "*,"
                + "telefono.ilike.*" + texto + "*)";

        SupabaseCliente.obtenerServicio()
                .buscarClientes("eq." + empresaId, filtroOr)
                .enqueue(new Callback<List<Cliente>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Cliente>> c,
                                           @NonNull Response<List<Cliente>> r) {
                        if (enlace == null || !r.isSuccessful()
                                || r.body() == null) return;

                        listaClientes.clear();
                        listaClientes.addAll(r.body());

                        List<String> sugerencias = new ArrayList<>();
                        for (Cliente cliente : listaClientes) {
                            sugerencias.add(cliente.getTextoAutocompletado());
                        }

                        ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                sugerencias);
                        enlace.campoCliente.setAdapter(adaptador);
                        enlace.campoCliente.showDropDown();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Cliente>> c,
                                          @NonNull Throwable e) { }
                });
    }

    /**
     * Configura los chips de tipo de equipo — selección única.
     */
    private void configurarChipsEquipo() {
        enlace.chipGroupEquipo.setOnCheckedStateChangeListener((grupo, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if      (id == enlace.chipLaptop.getId())      tipoEquipo = "laptop";
            else if (id == enlace.chipComputadora.getId()) tipoEquipo = "computadora";
            else if (id == enlace.chipImpresora.getId())   tipoEquipo = "impresora";
            else if (id == enlace.chipTablet.getId())      tipoEquipo = "tablet";
            else if (id == enlace.chipCelular.getId())     tipoEquipo = "celular";
            else                                            tipoEquipo = "otro";
        });
        enlace.chipLaptop.setChecked(true);
    }

    /**
     * Configura los chips de prioridad — selección única.
     */
    private void configurarChipsPrioridad() {
        enlace.chipGroupPrioridad.setOnCheckedStateChangeListener((grupo, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if      (id == enlace.chipBaja.getId())    prioridad = "baja";
            else if (id == enlace.chipNormal.getId())  prioridad = "normal";
            else if (id == enlace.chipAlta.getId())    prioridad = "alta";
            else                                        prioridad = "urgente";
        });
        enlace.chipNormal.setChecked(true);
    }

    /**
     * Configura los chips de accesorios — selección múltiple.
     */
    private void configurarChipsAccesorios() {
        enlace.chipFunda.setOnCheckedChangeListener((c, m) ->
                actualizarAccesorio("Funda", m));
        enlace.chipMouse.setOnCheckedChangeListener((c, m) ->
                actualizarAccesorio("Mouse", m));
        enlace.chipCargador.setOnCheckedChangeListener((c, m) ->
                actualizarAccesorio("Cargador", m));
        enlace.chipMochila.setOnCheckedChangeListener((c, m) ->
                actualizarAccesorio("Mochila", m));
        enlace.chipCableDatos.setOnCheckedChangeListener((c, m) ->
                actualizarAccesorio("Cable datos", m));
        enlace.chipEstabilizador.setOnCheckedChangeListener((c, m) ->
                actualizarAccesorio("Estabilizador", m));
    }

    private void actualizarAccesorio(String nombre, boolean agregar) {
        if (agregar) accesorios.add(nombre);
        else accesorios.remove(nombre);
    }

    /**
     * Configura los botones Guardar y Cancelar.
     */
    private void configurarBotones() {
        enlace.botonGuardar.setOnClickListener(v -> {
            if (validarFormulario()) guardarOrden();
        });
        enlace.botonCancelar.setOnClickListener(v -> {
            if (requireActivity() instanceof ActividadPrincipal) {
                ((ActividadPrincipal) requireActivity()).navegarA("ordenes");
            }
        });
    }

    /**
     * Valida los campos obligatorios.
     * @return true si el formulario es válido
     */
    private boolean validarFormulario() {
        boolean valido = true;

        if (clienteSeleccionado == null) {
            enlace.layoutCliente.setError(
                    getString(R.string.error_selecciona_cliente));
            valido = false;
        } else {
            enlace.layoutCliente.setError(null);
        }

        if (enlace.campoMarca.getText().toString().trim().isEmpty()) {
            enlace.layoutMarca.setError(
                    getString(R.string.error_ingresa_marca));
            valido = false;
        } else {
            enlace.layoutMarca.setError(null);
        }

        if (enlace.campoDesperfecto.getText().toString().trim().isEmpty()) {
            enlace.layoutDesperfecto.setError(
                    getString(R.string.error_ingresa_desperfecto));
            valido = false;
        } else {
            enlace.layoutDesperfecto.setError(null);
        }

        return valido;
    }

    /**
     * Guarda la orden en Supabase.
     * Paso 1: POST /orden
     * Paso 2: POST /equipo con el orden_id recibido
     */
    private void guardarOrden() {
        enlace.botonGuardar.setEnabled(false);
        enlace.progressGuardando.setVisibility(View.VISIBLE);

        Map<String, Object> datosOrden = new HashMap<>();
        datosOrden.put("empresa_id", empresaId);
        datosOrden.put("cliente_id", clienteSeleccionado.getId());
        datosOrden.put("tecnico_id", tecnicoId);
        datosOrden.put("estado",     "pendiente");
        datosOrden.put("prioridad",  prioridad);
        datosOrden.put("adelanto",   0.0);
        datosOrden.put("descuento",  0.0);

        String contrasena = enlace.campoContrasena.getText().toString().trim();
        if (!contrasena.isEmpty()) {
            datosOrden.put("contrasena_equipo", contrasena);
        }

        String fechaPrometida = enlace.campoFechaPrometida.getText()
                .toString().trim();
        if (!fechaPrometida.isEmpty()) {
            datosOrden.put("fecha_prometida", fechaPrometida);
        }

        SupabaseCliente.obtenerServicio()
                .crearOrden(datosOrden)
                .enqueue(new Callback<Orden>() {
                    @Override
                    public void onResponse(@NonNull Call<Orden> c,
                                           @NonNull Response<Orden> r) {
                        if (enlace == null) return;
                        if (r.isSuccessful() && r.body() != null) {
                            crearEquipo(r.body().getId());
                        } else {
                            mostrarError(getString(R.string.error_servidor));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Orden> c,
                                          @NonNull Throwable e) {
                        mostrarError(getString(R.string.error_sin_internet));
                    }
                });
    }

    /**
     * Crea el equipo asociado a la orden recién creada.
     */
    private void crearEquipo(int ordenId) {
        Map<String, Object> datosEquipo = new HashMap<>();
        datosEquipo.put("orden_id",   ordenId);
        datosEquipo.put("tipo",       tipoEquipo);
        datosEquipo.put("marca",      enlace.campoMarca.getText().toString().trim());
        datosEquipo.put("modelo",     enlace.campoModelo.getText().toString().trim());
        datosEquipo.put("numero_serie", enlace.campoSerie.getText().toString().trim());
        datosEquipo.put("desperfecto",  enlace.campoDesperfecto.getText().toString().trim());
        datosEquipo.put("descripcion_general",
                enlace.campoDescripcion.getText().toString().trim());

        SupabaseCliente.obtenerServicio()
                .crearEquipo(datosEquipo)
                .enqueue(new Callback<Equipo>() {
                    @Override
                    public void onResponse(@NonNull Call<Equipo> c,
                                           @NonNull Response<Equipo> r) {
                        onOrdenCreada();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Equipo> c,
                                          @NonNull Throwable e) {
                        // La orden se creó aunque el equipo fallara
                        onOrdenCreada();
                    }
                });
    }

    /**
     * Orden creada exitosamente — muestra mensaje y navega a la lista.
     */
    private void onOrdenCreada() {
        if (enlace == null) return;
        enlace.progressGuardando.setVisibility(View.GONE);
        enlace.botonGuardar.setEnabled(true);

        Snackbar.make(enlace.getRoot(),
                getString(R.string.msg_orden_guardada),
                Snackbar.LENGTH_LONG).show();

        limpiarFormulario();

        enlace.getRoot().postDelayed(() -> {
            if (getActivity() instanceof ActividadPrincipal) {
                ((ActividadPrincipal) getActivity()).navegarA("ordenes");
            }
        }, 1500);
    }

    private void mostrarError(String mensaje) {
        if (enlace == null) return;
        enlace.progressGuardando.setVisibility(View.GONE);
        enlace.botonGuardar.setEnabled(true);
        Snackbar.make(enlace.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
    }

    private void mostrarDialogoNuevoCliente() {
        DialogoNuevoCliente dialogo = new DialogoNuevoCliente(
                empresaId, clienteCreado -> {
            clienteSeleccionado = clienteCreado;
            enlace.campoCliente.setText(
                    clienteCreado.getNombreCompleto());
        });
        dialogo.show(getParentFragmentManager(), "nuevo_cliente");
    }

    private void limpiarFormulario() {
        enlace.campoCliente.setText("");
        enlace.campoMarca.setText("");
        enlace.campoModelo.setText("");
        enlace.campoSerie.setText("");
        enlace.campoDesperfecto.setText("");
        enlace.campoDescripcion.setText("");
        enlace.campoContrasena.setText("");
        enlace.campoFechaPrometida.setText("");
        enlace.chipLaptop.setChecked(true);
        enlace.chipNormal.setChecked(true);
        enlace.chipGroupAccesorios.clearCheck();
        clienteSeleccionado = null;
        accesorios.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        enlace = null;
    }
}