package com.example.tecrobsys.fragmentos.nueva_orden;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.tecrobsys.utils.MensajeUtils;
import com.example.tecrobsys.BuildConfig;
import com.example.tecrobsys.R;
import com.example.tecrobsys.actividades.ActividadPrincipal;
import com.example.tecrobsys.databinding.FragmentoNuevaOrdenBinding;
import com.example.tecrobsys.modelos.Cliente;
import com.example.tecrobsys.modelos.DNIRespuesta;
import com.example.tecrobsys.modelos.ServicioCatalogo;
import com.example.tecrobsys.red.ApiDNICliente;
import com.example.tecrobsys.utils.SesionManager;
import com.example.tecrobsys.viewmodels.NuevaOrdenViewModel;
import java.util.ArrayList;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class FragmentoNuevaOrden extends Fragment {

    private FragmentoNuevaOrdenBinding enlace;
    private NuevaOrdenViewModel viewModel;

    private Cliente clienteSeleccionado = null;
    private Call<DNIRespuesta> llamadaDNI;
    private final List<ServicioCatalogo> serviciosSeleccionados = new ArrayList<>();
    private final Set<Integer>           idsSeleccionados     = new HashSet<>();
    private final Set<Integer>           seleccionAnterior    = new HashSet<>();

    private static final String[] CATEGORIAS_VALOR    = {
            "mantenimiento", "reparacion", "software", "repuesto", "diagnostico", "otro"
    };
    private static final String[] CATEGORIAS_ETIQUETA = {
            "Mantenimiento", "Reparación", "Software", "Repuesto", "Diagnóstico", "Otro"
    };

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
        configurarBusquedaDni();
        configurarChipsEquipo();
        configurarChipsPrioridad();
        configurarChipsAccesorios();
        configurarFechaPicker();
        configurarServicios();
        configurarBotones();
        observarViewModel();

        viewModel.cargarCatalogo(empresaId);
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

    private void configurarBusquedaDni() {
        enlace.campoDniBusqueda.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable e) {}
            @Override
            public void onTextChanged(CharSequence texto, int start, int before, int count) {
                String dni = texto.toString().trim();
                if (dni.length() == 8) {
                    viewModel.buscarClientePorDni(empresaId, dni);
                } else {
                    ocultarPanelesResultado();
                }
            }
        });

        enlace.botonSeleccionarCliente.setOnClickListener(v -> {
            Cliente c = viewModel.clienteEncontrado.getValue();
            if (c != null) seleccionarCliente(c);
        });

        enlace.botonRegistrarCliente.setOnClickListener(v -> {
            if (validarCamposNuevoCliente()) registrarNuevoCliente();
        });

        enlace.botonEditarTelefono.setOnClickListener(v -> mostrarDialogoEditarTelefono());

        enlace.botonCambiarCliente.setOnClickListener(v -> {
            if (llamadaDNI != null) { llamadaDNI.cancel(); llamadaDNI = null; }
            clienteSeleccionado = null;
            viewModel.clienteEncontrado.setValue(null);
            viewModel.clienteNoEncontrado.setValue(false);
            enlace.contenedorClienteSeleccionado.setVisibility(View.GONE);
            enlace.layoutDniBusqueda.setVisibility(View.VISIBLE);
            enlace.campoDniBusqueda.setText("");
            ocultarPanelesResultado();
        });
    }

    private void consultarDniApi(String dni) {
        if (llamadaDNI != null) llamadaDNI.cancel();
        enlace.progressBuscandoCliente.setVisibility(View.VISIBLE);
        enlace.campoNombreNuevo.setText("");
        enlace.campoApellidoNuevo.setText("");

        llamadaDNI = ApiDNICliente.obtenerServicio().consultar(dni, BuildConfig.DNI_API_TOKEN);
        llamadaDNI.enqueue(new Callback<DNIRespuesta>() {
            @Override
            public void onResponse(@NonNull Call<DNIRespuesta> call,
                                   @NonNull Response<DNIRespuesta> respuesta) {
                if (enlace == null || !isAdded()) return;
                enlace.progressBuscandoCliente.setVisibility(View.GONE);
                if (respuesta.isSuccessful() && respuesta.body() != null) {
                    DNIRespuesta datos = respuesta.body();
                    if (datos.getNombres() != null)
                        enlace.campoNombreNuevo.setText(datos.getNombres());
                    String apellido = "";
                    if (datos.getApellidoPaterno() != null)
                        apellido = datos.getApellidoPaterno();
                    if (datos.getApellidoMaterno() != null && !datos.getApellidoMaterno().isEmpty())
                        apellido += (apellido.isEmpty() ? "" : " ") + datos.getApellidoMaterno();
                    if (!apellido.isEmpty())
                        enlace.campoApellidoNuevo.setText(apellido);
                    enlace.campoTelefonoNuevo.requestFocus();
                }
            }
            @Override
            public void onFailure(@NonNull Call<DNIRespuesta> call, @NonNull Throwable t) {
                if (enlace == null || !isAdded() || call.isCanceled()) return;
                enlace.progressBuscandoCliente.setVisibility(View.GONE);
            }
        });
    }

    private void ocultarPanelesResultado() {
        enlace.progressBuscandoCliente.setVisibility(View.GONE);
        enlace.cardClienteEncontrado.setVisibility(View.GONE);
        enlace.contenedorRegistroCliente.setVisibility(View.GONE);
    }

    private void seleccionarCliente(Cliente cliente) {
        clienteSeleccionado = cliente;
        enlace.textNombreClienteSel.setText(cliente.getNombreCompleto());
        enlace.textTelefonoClienteSel.setText(
                cliente.getTelefono() != null ? cliente.getTelefono() : "Sin teléfono");
        enlace.layoutDniBusqueda.setVisibility(View.GONE);
        enlace.contenedorClienteSeleccionado.setVisibility(View.VISIBLE);
        ocultarPanelesResultado();
        enlace.layoutDniBusqueda.setError(null);
    }

    private void mostrarDialogoEditarTelefono() {
        if (clienteSeleccionado == null) return;

        View vista = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialogo_editar_telefono, null);
        com.google.android.material.textfield.TextInputEditText inputTel =
                vista.findViewById(R.id.campo_nuevo_telefono);

        String telActual = clienteSeleccionado.getTelefono();
        if (telActual != null) {
            inputTel.setText(telActual);
            inputTel.setSelection(telActual.length());
        }

        androidx.appcompat.app.AlertDialog dialogo = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.titulo_editar_telefono))
                .setView(vista)
                .setPositiveButton(getString(R.string.btn_aplicar), (dialog, which) -> {
                    String nuevoTel = inputTel.getText().toString().trim();
                    if (!nuevoTel.isEmpty()) {
                        clienteSeleccionado.setTelefono(nuevoTel);
                        enlace.textTelefonoClienteSel.setText(nuevoTel);
                        if (clienteSeleccionado.getId() > 0)
                            viewModel.actualizarTelefonoCliente(clienteSeleccionado.getId(), nuevoTel);
                    }
                })
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .create();

        dialogo.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.fondo_tarjeta)));
        dialogo.show();
    }

    private boolean validarCamposNuevoCliente() {
        boolean valido = true;
        if (enlace.campoNombreNuevo.getText().toString().trim().isEmpty()) {
            enlace.layoutNombreNuevo.setError(getString(R.string.error_nombre_vacio));
            valido = false;
        } else {
            enlace.layoutNombreNuevo.setError(null);
        }
        if (enlace.campoTelefonoNuevo.getText().toString().trim().isEmpty()) {
            enlace.layoutTelefonoNuevo.setError(getString(R.string.error_telefono_vacio));
            valido = false;
        } else {
            enlace.layoutTelefonoNuevo.setError(null);
        }
        return valido;
    }

    private void registrarNuevoCliente() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("empresa_id", empresaId);
        datos.put("nombre",    enlace.campoNombreNuevo.getText().toString().trim());
        datos.put("apellido",  enlace.campoApellidoNuevo.getText().toString().trim());
        datos.put("telefono",  enlace.campoTelefonoNuevo.getText().toString().trim());
        String dni = enlace.campoDniBusqueda.getText().toString().trim();
        if (!dni.isEmpty()) datos.put("dni", dni);
        String email = enlace.campoEmailNuevo.getText().toString().trim();
        if (!email.isEmpty()) datos.put("email", email);
        String dir = enlace.campoDireccionNuevo.getText().toString().trim();
        if (!dir.isEmpty()) datos.put("direccion", dir);
        viewModel.registrarCliente(datos);
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
        mostrarDialogoServiciosConSeleccion(idsSeleccionados);
    }

    private void mostrarDialogoServiciosConSeleccion(Set<Integer> previos) {
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
            marcados[i]  = previos.contains(s.getId());
        }

        Set<Integer> seleccionTemp = new HashSet<>(previos);
        int dp = (int) getResources().getDisplayMetrics().density;

        // Lista con checkboxes
        ListView lista = new ListView(requireContext());
        lista.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_multiple_choice, etiquetas));
        lista.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        for (int i = 0; i < marcados.length; i++) lista.setItemChecked(i, marcados[i]);
        lista.setOnItemClickListener((parent, v, pos, id) -> {
            if (lista.isItemChecked(pos)) seleccionTemp.add(catalogo.get(pos).getId());
            else                          seleccionTemp.remove(catalogo.get(pos).getId());
        });
        LinearLayout.LayoutParams listaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        lista.setLayoutParams(listaParams);

        // Fila de botones horizontal
        LinearLayout filaBotones = new LinearLayout(requireContext());
        filaBotones.setOrientation(LinearLayout.HORIZONTAL);
        filaBotones.setPadding(8 * dp, 8 * dp, 8 * dp, 8 * dp);

        MaterialButton btnCancelar = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnCancelar.setText(getString(R.string.btn_cancelar));
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p1.setMargins(0, 0, 4 * dp, 0);
        btnCancelar.setLayoutParams(p1);

        MaterialButton btnNuevo = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnNuevo.setText(getString(R.string.btn_nuevo_servicio));
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p2.setMargins(4 * dp, 0, 4 * dp, 0);
        btnNuevo.setLayoutParams(p2);

        MaterialButton btnAceptar = new MaterialButton(requireContext());
        btnAceptar.setText(getString(R.string.btn_aceptar));
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p3.setMargins(4 * dp, 0, 0, 0);
        btnAceptar.setLayoutParams(p3);

        filaBotones.addView(btnCancelar);
        filaBotones.addView(btnNuevo);
        filaBotones.addView(btnAceptar);

        LinearLayout contenedor = new LinearLayout(requireContext());
        contenedor.setOrientation(LinearLayout.VERTICAL);
        contenedor.addView(lista);
        contenedor.addView(filaBotones);

        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.titulo_seleccionar_servicios))
                .setView(contenedor)
                .create();

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnNuevo.setOnClickListener(v -> {
            dialog.dismiss();
            seleccionAnterior.clear();
            seleccionAnterior.addAll(seleccionTemp);
            mostrarDialogoCrearServicio();
        });

        btnAceptar.setOnClickListener(v -> {
            dialog.dismiss();
            idsSeleccionados.clear();
            idsSeleccionados.addAll(seleccionTemp);
            serviciosSeleccionados.clear();
            for (ServicioCatalogo s : catalogo) {
                if (idsSeleccionados.contains(s.getId()))
                    serviciosSeleccionados.add(s);
            }
            actualizarVistaServicios();
        });

        dialog.show();
    }

    private void mostrarDialogoCrearServicio() {
        int empresaId = SesionManager.obtenerInstancia(requireContext()).obtenerEmpresaId();

        android.content.Context ctxClaro = new android.view.ContextThemeWrapper(
                requireContext(),
                com.google.android.material.R.style.Theme_Material3_Light_NoActionBar);

        LinearLayout contenedor = new LinearLayout(ctxClaro);
        contenedor.setOrientation(LinearLayout.VERTICAL);
        int dp = (int) getResources().getDisplayMetrics().density;
        contenedor.setPadding(16 * dp, 16 * dp, 16 * dp, 16 * dp);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 8 * dp;

        TextInputLayout layoutNombre = new TextInputLayout(ctxClaro, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        layoutNombre.setHint("Nombre del servicio *");
        TextInputEditText campoNombre = new TextInputEditText(ctxClaro);
        campoNombre.setTextColor(Color.BLACK);
        layoutNombre.addView(campoNombre);
        contenedor.addView(layoutNombre);

        TextInputLayout layoutPrecio = new TextInputLayout(ctxClaro, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        layoutPrecio.setHint("Precio base (S/) *");
        TextInputEditText campoPrecio = new TextInputEditText(ctxClaro);
        campoPrecio.setTextColor(Color.BLACK);
        campoPrecio.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layoutPrecio.addView(campoPrecio);
        layoutPrecio.setLayoutParams(params);
        contenedor.addView(layoutPrecio);

        TextInputLayout layoutCategoria = new TextInputLayout(ctxClaro, null,
                com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle);
        layoutCategoria.setHint("Categoría *");
        AutoCompleteTextView campoCategoria = new AutoCompleteTextView(ctxClaro);
        campoCategoria.setTextColor(Color.BLACK);
        campoCategoria.setFocusable(false);
        campoCategoria.setAdapter(new ArrayAdapter<>(ctxClaro,
                android.R.layout.simple_dropdown_item_1line, CATEGORIAS_ETIQUETA));
        layoutCategoria.addView(campoCategoria);
        layoutCategoria.setLayoutParams(params);
        contenedor.addView(layoutCategoria);

        TextInputLayout layoutDesc = new TextInputLayout(ctxClaro, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        layoutDesc.setHint("Descripción (opcional)");
        TextInputEditText campoDesc = new TextInputEditText(ctxClaro);
        campoDesc.setTextColor(Color.BLACK);
        layoutDesc.addView(campoDesc);
        layoutDesc.setLayoutParams(params);
        contenedor.addView(layoutDesc);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.titulo_nuevo_servicio))
                .setView(contenedor)
                .setPositiveButton(getString(R.string.btn_guardar_orden), (d, w) -> {
                    String nombre    = campoNombre.getText().toString().trim();
                    String precioStr = campoPrecio.getText().toString().trim();
                    String catTexto  = campoCategoria.getText().toString().trim();
                    String desc      = campoDesc.getText().toString().trim();

                    if (nombre.isEmpty() || precioStr.isEmpty() || catTexto.isEmpty()) {
                        MensajeUtils.mostrar(requireContext(), "Completa los campos obligatorios");
                        return;
                    }

                    String catValor = "otro";
                    for (int i = 0; i < CATEGORIAS_ETIQUETA.length; i++) {
                        if (CATEGORIAS_ETIQUETA[i].equals(catTexto)) {
                            catValor = CATEGORIAS_VALOR[i];
                            break;
                        }
                    }

                    double precio;
                    try { precio = Double.parseDouble(precioStr); }
                    catch (NumberFormatException e) { precio = 0.0; }

                    Map<String, Object> datos = new HashMap<>();
                    datos.put("nombre",      nombre);
                    datos.put("precio_base", precio);
                    datos.put("categoria",   catValor);
                    datos.put("descripcion", desc);
                    datos.put("empresa_id",  empresaId);
                    datos.put("activo",      true);

                    viewModel.crearServicio(empresaId, datos);
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
        viewModel.buscandoDni.observe(getViewLifecycleOwner(), buscando -> {
            boolean cargando = Boolean.TRUE.equals(buscando);
            enlace.progressBuscandoCliente.setVisibility(cargando ? View.VISIBLE : View.GONE);
        });

        viewModel.clienteEncontrado.observe(getViewLifecycleOwner(), cliente -> {
            if (cliente != null) {
                enlace.textNombreClienteEncontrado.setText(cliente.getNombreCompleto());
                String tel = cliente.getTelefono() != null ? cliente.getTelefono() : "Sin teléfono";
                enlace.textTelefonoClienteEncontrado.setText(tel);
                enlace.cardClienteEncontrado.setVisibility(View.VISIBLE);
                enlace.contenedorRegistroCliente.setVisibility(View.GONE);
            }
        });

        viewModel.clienteNoEncontrado.observe(getViewLifecycleOwner(), noEncontrado -> {
            if (Boolean.TRUE.equals(noEncontrado)) {
                enlace.cardClienteEncontrado.setVisibility(View.GONE);
                enlace.contenedorRegistroCliente.setVisibility(View.VISIBLE);
                String dni = enlace.campoDniBusqueda.getText().toString().trim();
                if (dni.length() == 8) consultarDniApi(dni);
            }
        });

        viewModel.registrandoCliente.observe(getViewLifecycleOwner(), registrando -> {
            boolean cargando = Boolean.TRUE.equals(registrando);
            enlace.progressBuscandoCliente.setVisibility(cargando ? View.VISIBLE : View.GONE);
            enlace.botonRegistrarCliente.setEnabled(!cargando);
        });

        viewModel.clienteCreadoRegistrado.observe(getViewLifecycleOwner(), cliente -> {
            if (cliente != null) {
                seleccionarCliente(cliente);
                MensajeUtils.mostrar(requireContext(), getString(R.string.msg_cliente_guardado));
                viewModel.clienteCreadoRegistrado.setValue(null);
            }
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

        viewModel.servicioCreado.observe(getViewLifecycleOwner(), servicio -> {
            if (servicio == null) return;
            seleccionAnterior.add(servicio.getId());
            MensajeUtils.mostrar(requireContext(), getString(R.string.msg_servicio_creado));
            viewModel.servicioCreado.setValue(null);
            mostrarDialogoServiciosConSeleccion(seleccionAnterior);
        });
    }

    // ── Lógica de formulario ──────────────────────────────────────────

    private boolean validarFormulario() {
        boolean valido = true;
        if (clienteSeleccionado == null) {
            enlace.layoutDniBusqueda.setError(getString(R.string.error_selecciona_cliente));
            valido = false;
        } else {
            enlace.layoutDniBusqueda.setError(null);
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

    private void limpiarFormulario() {
        clienteSeleccionado = null;
        enlace.campoDniBusqueda.setText("");
        enlace.campoNombreNuevo.setText("");
        enlace.campoApellidoNuevo.setText("");
        enlace.campoTelefonoNuevo.setText("");
        enlace.campoEmailNuevo.setText("");
        enlace.campoDireccionNuevo.setText("");
        enlace.layoutDniBusqueda.setVisibility(View.VISIBLE);
        enlace.contenedorClienteSeleccionado.setVisibility(View.GONE);
        ocultarPanelesResultado();
        viewModel.clienteEncontrado.setValue(null);
        viewModel.clienteNoEncontrado.setValue(false);
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
        accesorios.clear();
        serviciosSeleccionados.clear();
        idsSeleccionados.clear();
        fechaIso = "";
        actualizarVistaServicios();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (llamadaDNI != null) { llamadaDNI.cancel(); llamadaDNI = null; }
        enlace = null;
    }
}
