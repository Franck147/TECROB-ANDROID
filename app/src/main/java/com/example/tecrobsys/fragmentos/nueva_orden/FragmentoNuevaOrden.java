package com.example.tecrobsys.fragmentos.nueva_orden;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.example.tecrobsys.utils.MensajeUtils;
import com.example.tecrobsys.R;
import com.example.tecrobsys.actividades.ActividadPrincipal;
import com.example.tecrobsys.databinding.FragmentoNuevaOrdenBinding;
import com.example.tecrobsys.modelos.Cliente;
import com.example.tecrobsys.modelos.ServicioCatalogo;
import com.example.tecrobsys.utils.SesionManager;
import com.example.tecrobsys.viewmodels.NuevaOrdenViewModel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class FragmentoNuevaOrden extends Fragment
        implements DialogoNuevoCliente.OnClienteCreadoListener {

    private FragmentoNuevaOrdenBinding enlace;
    private NuevaOrdenViewModel viewModel;

    private Cliente clienteSeleccionado = null;
    private final List<Cliente>          listaClientes        = new ArrayList<>();
    private final List<ServicioCatalogo> serviciosSeleccionados = new ArrayList<>();
    private final Set<Integer>           idsSeleccionados     = new HashSet<>();

    private String tipoEquipo    = "laptop";
    private String prioridad     = "normal";
    private String fechaIso      = "";          // "yyyy-MM-dd" para la API
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
    public void onViewCreated(@NonNull View vista, @Nullable Bundle estadoGuardado) {
        super.onViewCreated(vista, estadoGuardado);

        viewModel = new ViewModelProvider(this).get(NuevaOrdenViewModel.class);
        SesionManager sesion = SesionManager.obtenerInstancia(requireContext());
        empresaId = sesion.obtenerEmpresaId();
        tecnicoId = sesion.obtenerTecnicoId();

        ajustarPaddingConTeclado();
        forzarMayusculas();
        configurarBuscadorCliente();
        configurarChipsEquipo();
        configurarChipsPrioridad();
        configurarChipsAccesorios();
        configurarFechaPicker();
        configurarServicios();
        configurarBotones();
        observarViewModel();

        viewModel.cargarCatalogo(empresaId);
    }

    @Override
    public void alCrearCliente(Cliente cliente) {
        clienteSeleccionado = cliente;
        enlace.campoCliente.setText(cliente.getNombreCompleto());
        enlace.layoutCliente.setError(null);
    }

    // ── Configuración de UI ───────────────────────────────────────────

    private void ajustarPaddingConTeclado() {
        ViewCompat.setOnApplyWindowInsetsListener(enlace.getRoot(), (v, insets) -> {
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(0, 0, 0, Math.max(imeBottom, navBottom));
            return insets;
        });
    }

    private void forzarMayusculas() {
        InputFilter[] filtro = { new InputFilter.AllCaps() };
        enlace.campoMarca.setFilters(filtro);
        enlace.campoModelo.setFilters(filtro);
        enlace.campoSerie.setFilters(filtro);
        enlace.campoDesperfecto.setFilters(filtro);
        enlace.campoDescripcion.setFilters(filtro);
        enlace.campoContrasena.setFilters(filtro);
    }

    private void configurarBuscadorCliente() {
        enlace.campoCliente.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable e) {}
            @Override
            public void onTextChanged(CharSequence texto, int i, int a, int c) {
                if (texto.length() >= 2) viewModel.buscarClientes(empresaId, texto.toString());
                if (texto.length() == 0) clienteSeleccionado = null;
            }
        });
        enlace.campoCliente.setOnItemClickListener((parent, v, pos, id) -> {
            if (pos < listaClientes.size()) {
                clienteSeleccionado = listaClientes.get(pos);
                enlace.campoCliente.setText(clienteSeleccionado.getNombreCompleto());
                enlace.layoutCliente.setError(null);
            }
        });
        enlace.botonNuevoCliente.setOnClickListener(v -> mostrarDialogoNuevoCliente());
    }

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

    private void configurarChipsPrioridad() {
        enlace.chipGroupPrioridad.setOnCheckedStateChangeListener((grupo, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if      (id == enlace.chipBaja.getId())   prioridad = "baja";
            else if (id == enlace.chipNormal.getId()) prioridad = "normal";
            else if (id == enlace.chipAlta.getId())   prioridad = "alta";
            else                                       prioridad = "urgente";
        });
        enlace.chipNormal.setChecked(true);
    }

    private void configurarChipsAccesorios() {
        enlace.chipFunda.setOnCheckedChangeListener((c, m)         -> actualizarAccesorio("Funda", m));
        enlace.chipMouse.setOnCheckedChangeListener((c, m)         -> actualizarAccesorio("Mouse", m));
        enlace.chipCargador.setOnCheckedChangeListener((c, m)      -> actualizarAccesorio("Cargador", m));
        enlace.chipMochila.setOnCheckedChangeListener((c, m)       -> actualizarAccesorio("Mochila", m));
        enlace.chipCableDatos.setOnCheckedChangeListener((c, m)    -> actualizarAccesorio("Cable datos", m));
        enlace.chipEstabilizador.setOnCheckedChangeListener((c, m) -> actualizarAccesorio("Estabilizador", m));
    }

    private void actualizarAccesorio(String nombre, boolean agregar) {
        if (agregar) accesorios.add(nombre);
        else accesorios.remove(nombre);
    }

    private void configurarFechaPicker() {
        View.OnClickListener abrirCalendario = v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(getString(R.string.hint_fecha_prometida))
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            picker.addOnPositiveButtonClickListener(seleccion -> {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                cal.setTimeInMillis(seleccion);
                int anio = cal.get(Calendar.YEAR);
                int mes  = cal.get(Calendar.MONTH) + 1;
                int dia  = cal.get(Calendar.DAY_OF_MONTH);
                fechaIso = String.format("%04d-%02d-%02d", anio, mes, dia);
                // Mostrar en formato local al usuario
                enlace.campoFechaPrometida.setText(
                        String.format("%02d/%02d/%04d", dia, mes, anio));
            });

            picker.show(getParentFragmentManager(), "fecha_picker");
        };

        enlace.campoFechaPrometida.setOnClickListener(abrirCalendario);
        // También al tocar el ícono del campo
        enlace.campoFechaPrometida.setFocusable(false);
    }

    private void configurarServicios() {
        enlace.botonAgregarServicio.setOnClickListener(v -> mostrarDialogoServicios());
    }

    private void mostrarDialogoServicios() {
        List<ServicioCatalogo> catalogo = viewModel.catalogoDisponible.getValue();
        if (catalogo == null || catalogo.isEmpty()) {
            MensajeUtils.mostrar(requireContext(), getString(R.string.msg_sin_catalogo));
            return;
        }

        String[] etiquetas = new String[catalogo.size()];
        boolean[] marcados = new boolean[catalogo.size()];
        for (int i = 0; i < catalogo.size(); i++) {
            ServicioCatalogo s = catalogo.get(i);
            etiquetas[i] = s.getNombre() + "  —  " + s.getPrecioFormateado();
            marcados[i]  = idsSeleccionados.contains(s.getId());
        }

        // Copia temporal para cambios en el diálogo
        Set<Integer> seleccionTemp = new HashSet<>(idsSeleccionados);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.titulo_seleccionar_servicios))
                .setMultiChoiceItems(etiquetas, marcados, (dialog, which, checked) -> {
                    if (checked) seleccionTemp.add(catalogo.get(which).getId());
                    else         seleccionTemp.remove(catalogo.get(which).getId());
                })
                .setPositiveButton(getString(R.string.btn_aceptar), (d, w) -> {
                    idsSeleccionados.clear();
                    idsSeleccionados.addAll(seleccionTemp);
                    serviciosSeleccionados.clear();
                    for (ServicioCatalogo s : catalogo) {
                        if (idsSeleccionados.contains(s.getId()))
                            serviciosSeleccionados.add(s);
                    }
                    actualizarVistaServicios();
                })
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .show();
    }

    private void actualizarVistaServicios() {
        enlace.contenedorServiciosSeleccionados.removeAllViews();

        double subtotal = 0;
        int dp = (int) (getResources().getDisplayMetrics().density);

        for (ServicioCatalogo s : serviciosSeleccionados) {
            LinearLayout fila = new LinearLayout(requireContext());
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
            fila.setPadding(0, 8 * dp, 0, 8 * dp);

            TextView texto = new TextView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            texto.setLayoutParams(lp);
            texto.setText(s.getNombre() + "  " + s.getPrecioFormateado());
            texto.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_principal));
            texto.setTextSize(13f);

            TextView btnQuitar = new TextView(requireContext());
            btnQuitar.setText("✕");
            btnQuitar.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_muted));
            btnQuitar.setTextSize(16f);
            btnQuitar.setPadding(12 * dp, 4 * dp, 4 * dp, 4 * dp);
            btnQuitar.setClickable(true);
            btnQuitar.setFocusable(true);
            final ServicioCatalogo srv = s;
            btnQuitar.setOnClickListener(v -> {
                idsSeleccionados.remove(srv.getId());
                serviciosSeleccionados.remove(srv);
                actualizarVistaServicios();
            });

            fila.addView(texto);
            fila.addView(btnQuitar);
            enlace.contenedorServiciosSeleccionados.addView(fila);
            subtotal += s.getPrecioBase();
        }

        boolean hayServicios = !serviciosSeleccionados.isEmpty();
        enlace.seccionSubtotalServicios.setVisibility(hayServicios ? View.VISIBLE : View.GONE);
        enlace.textSubtotalServicios.setText(String.format("S/ %.2f", subtotal));
    }

    private void configurarBotones() {
        enlace.botonGuardar.setOnClickListener(v -> {
            if (validarFormulario()) guardarOrden();
        });
        enlace.botonCancelar.setOnClickListener(v -> {
            if (requireActivity() instanceof ActividadPrincipal)
                ((ActividadPrincipal) requireActivity()).navegarA("ordenes");
        });
    }

    private void observarViewModel() {
        viewModel.clientesSugeridos.observe(getViewLifecycleOwner(), clientes -> {
            listaClientes.clear();
            listaClientes.addAll(clientes);
            List<String> sugerencias = new ArrayList<>();
            for (Cliente c : clientes) sugerencias.add(c.getTextoAutocompletado());
            enlace.campoCliente.setAdapter(new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    sugerencias));
            if (!sugerencias.isEmpty()) enlace.campoCliente.showDropDown();
        });

        viewModel.guardando.observe(getViewLifecycleOwner(), g -> {
            boolean cargando = Boolean.TRUE.equals(g);
            enlace.botonGuardar.setEnabled(!cargando);
            enlace.progressGuardando.setVisibility(cargando ? View.VISIBLE : View.GONE);
        });

        viewModel.ordenGuardada.observe(getViewLifecycleOwner(), ok -> {
            if (Boolean.TRUE.equals(ok)) {
                MensajeUtils.mostrar(requireContext(), getString(R.string.msg_orden_guardada));
                limpiarFormulario();
                enlace.getRoot().postDelayed(() -> {
                    if (getActivity() instanceof ActividadPrincipal)
                        ((ActividadPrincipal) getActivity()).navegarA("ordenes");
                }, 1500);
                viewModel.ordenGuardada.setValue(null);
            }
        });

        viewModel.error.observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                MensajeUtils.mostrar(requireContext(), err);
                viewModel.error.setValue(null);
            }
        });
    }

    // ── Lógica de formulario ──────────────────────────────────────────

    private boolean validarFormulario() {
        boolean valido = true;
        if (clienteSeleccionado == null) {
            enlace.layoutCliente.setError(getString(R.string.error_selecciona_cliente));
            valido = false;
        } else {
            enlace.layoutCliente.setError(null);
        }
        if (enlace.campoMarca.getText().toString().trim().isEmpty()) {
            enlace.layoutMarca.setError(getString(R.string.error_ingresa_marca));
            valido = false;
        } else {
            enlace.layoutMarca.setError(null);
        }
        if (enlace.campoDesperfecto.getText().toString().trim().isEmpty()) {
            enlace.layoutDesperfecto.setError(getString(R.string.error_ingresa_desperfecto));
            valido = false;
        } else {
            enlace.layoutDesperfecto.setError(null);
        }
        return valido;
    }

    private void guardarOrden() {
        Map<String, Object> datosOrden = new HashMap<>();
        datosOrden.put("empresa_id", empresaId);
        datosOrden.put("cliente_id", clienteSeleccionado.getId());
        datosOrden.put("tecnico_id", tecnicoId);
        datosOrden.put("estado",     "pendiente");
        datosOrden.put("prioridad",  prioridad);
        datosOrden.put("adelanto",   0.0);
        datosOrden.put("descuento",  0.0);

        String contrasena = enlace.campoContrasena.getText().toString().trim();
        if (!contrasena.isEmpty()) datosOrden.put("contrasena_equipo", contrasena);
        if (!fechaIso.isEmpty())   datosOrden.put("fecha_prometida", fechaIso);

        Map<String, Object> datosEquipo = new HashMap<>();
        datosEquipo.put("tipo",                tipoEquipo);
        datosEquipo.put("marca",               enlace.campoMarca.getText().toString().trim());
        datosEquipo.put("modelo",              enlace.campoModelo.getText().toString().trim());
        datosEquipo.put("numero_serie",        enlace.campoSerie.getText().toString().trim());
        datosEquipo.put("desperfecto",         enlace.campoDesperfecto.getText().toString().trim());
        datosEquipo.put("descripcion_general", enlace.campoDescripcion.getText().toString().trim());
        if (!accesorios.isEmpty())
            datosEquipo.put("accesorios", android.text.TextUtils.join(", ", accesorios));

        viewModel.guardarOrden(empresaId, tecnicoId, datosOrden, datosEquipo,
                new ArrayList<>(serviciosSeleccionados));
    }

    private void mostrarDialogoNuevoCliente() {
        DialogoNuevoCliente dialogo = DialogoNuevoCliente.nuevaInstancia(empresaId);
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
        serviciosSeleccionados.clear();
        idsSeleccionados.clear();
        fechaIso = "";
        actualizarVistaServicios();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        enlace = null;
    }
}
