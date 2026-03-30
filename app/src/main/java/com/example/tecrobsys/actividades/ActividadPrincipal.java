package com.example.tecrobsys.actividades;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.tecrobsys.R;
import com.example.tecrobsys.databinding.ActivityMainBinding;
import com.example.tecrobsys.fragmentos.catalogo.FragmentoCatalogo;
import com.example.tecrobsys.fragmentos.dashboard.FragmentoDashboard;
import com.example.tecrobsys.fragmentos.nueva_orden.FragmentoNuevaOrden;
import com.example.tecrobsys.fragmentos.ordenes.FragmentoOrdenes;
import com.example.tecrobsys.utils.SesionManager;

/**
 * ActividadPrincipal — Contenedor de todos los fragmentos de la app.
 *
 * Patrón: Single Activity + múltiples Fragmentos.
 * La navegación se maneja con BottomNavigationView de Material 3.
 *
 * Fragmentos que maneja:
 *   Dashboard   → métricas del día + órdenes recientes
 *   Órdenes     → lista completa con filtros
 *   Nueva orden → formulario de creación (FAB central)
 *   Catálogo    → servicios y repuestos disponibles
 *
 * Estrategia de navegación: show/hide en lugar de replace.
 * Esto preserva el estado de cada fragmento (scroll, filtros, etc.)
 * cuando el usuario navega entre pantallas.
 */
public class ActividadPrincipal extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener {

    // ViewBinding — acceso seguro a las vistas
    private ActivityMainBinding enlace;

    // Los 4 fragmentos principales — se crean una sola vez
    private FragmentoDashboard  fragmentoDashboard;
    private FragmentoOrdenes    fragmentoOrdenes;
    private FragmentoNuevaOrden fragmentoNuevaOrden;
    private FragmentoCatalogo   fragmentoCatalogo;

    // Fragmento actualmente visible en pantalla
    private Fragment fragmentoActivo;

    @Override
    protected void onCreate(Bundle estadoGuardado) {
        super.onCreate(estadoGuardado);

        // Inflar el layout usando ViewBinding
        enlace = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(enlace.getRoot());

        // Restaurar el token JWT en el cliente HTTP
        // (por si la app fue cerrada y se reabre con sesión activa)
        SesionManager.obtenerInstancia(this).restaurarToken();

        // Inicializar fragmentos y configurar la navegación
        inicializarFragmentos();
        configurarNavegacion();
    }

    /**
     * Crea los 4 fragmentos y los agrega al FragmentManager.
     * Solo el Dashboard se muestra inicialmente, los demás se ocultan.
     *
     * Agregar todos de una vez evita recrearlos al navegar,
     * lo que preserva su estado (listas cargadas, filtros, etc.)
     */
    private void inicializarFragmentos() {
        fragmentoDashboard  = new FragmentoDashboard();
        fragmentoOrdenes    = new FragmentoOrdenes();
        fragmentoNuevaOrden = new FragmentoNuevaOrden();
        fragmentoCatalogo   = new FragmentoCatalogo();

        // Agregar todos al contenedor pero ocultar todos menos el Dashboard
        getSupportFragmentManager().beginTransaction()
                .add(R.id.contenedor_fragmento, fragmentoDashboard,  "dashboard")
                .add(R.id.contenedor_fragmento, fragmentoOrdenes,    "ordenes")
                .add(R.id.contenedor_fragmento, fragmentoNuevaOrden, "nueva_orden")
                .add(R.id.contenedor_fragmento, fragmentoCatalogo,   "catalogo")
                .hide(fragmentoOrdenes)
                .hide(fragmentoNuevaOrden)
                .hide(fragmentoCatalogo)
                .commit();

        fragmentoActivo = fragmentoDashboard;
    }

    /**
     * Configura el BottomNavigationView y el FAB central.
     */
    private void configurarNavegacion() {
        // Listener del Bottom Navigation
        enlace.navegacionInferior.setOnNavigationItemSelectedListener(this);

        // Seleccionar Dashboard como ítem inicial
        enlace.navegacionInferior.setSelectedItemId(R.id.nav_dashboard);

        // FAB central: abre directamente el formulario de nueva orden
        enlace.fabNuevaOrden.setOnClickListener(vista -> {
            mostrarFragmento(fragmentoNuevaOrden);
            // Marcar el ítem "Nueva" como activo en el nav
            enlace.navegacionInferior.getMenu()
                    .findItem(R.id.nav_nueva)
                    .setChecked(true);
        });
    }

    /**
     * Maneja los clics en el Bottom Navigation.
     * Cada ítem muestra su fragmento correspondiente.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            mostrarFragmento(fragmentoDashboard);
            return true;
        } else if (id == R.id.nav_ordenes) {
            mostrarFragmento(fragmentoOrdenes);
            return true;
        } else if (id == R.id.nav_nueva) {
            mostrarFragmento(fragmentoNuevaOrden);
            return true;
        } else if (id == R.id.nav_catalogo) {
            mostrarFragmento(fragmentoCatalogo);
            return true;
        } else if (id == R.id.nav_config) {
            // TODO: Agregar FragmentoConfiguracion en el futuro
            return true;
        }

        return false;
    }

    /**
     * Muestra el fragmento solicitado y oculta el anterior.
     * Usar show/hide conserva el estado de cada fragmento.
     *
     * @param nuevoFragmento El fragmento a mostrar
     */
    private void mostrarFragmento(Fragment nuevoFragmento) {
        // Si ya está visible no hacer nada
        if (nuevoFragmento == fragmentoActivo) return;

        getSupportFragmentManager().beginTransaction()
                .hide(fragmentoActivo)
                .show(nuevoFragmento)
                .commit();

        fragmentoActivo = nuevoFragmento;
    }

    /**
     * Método público para que los fragmentos puedan navegar
     * a otras pantallas desde su propio código.
     *
     * Ejemplo desde FragmentoNuevaOrden después de guardar:
     *   ((ActividadPrincipal) requireActivity()).navegarA("ordenes");
     *
     * @param destino "dashboard" | "ordenes" | "nueva_orden" | "catalogo"
     */
    public void navegarA(String destino) {
        switch (destino) {
            case "dashboard":
                mostrarFragmento(fragmentoDashboard);
                enlace.navegacionInferior.setSelectedItemId(R.id.nav_dashboard);
                break;
            case "ordenes":
                mostrarFragmento(fragmentoOrdenes);
                enlace.navegacionInferior.setSelectedItemId(R.id.nav_ordenes);
                break;
            case "nueva_orden":
                mostrarFragmento(fragmentoNuevaOrden);
                enlace.navegacionInferior.setSelectedItemId(R.id.nav_nueva);
                break;
            case "catalogo":
                mostrarFragmento(fragmentoCatalogo);
                enlace.navegacionInferior.setSelectedItemId(R.id.nav_catalogo);
                break;
        }
    }

    /**
     * Abre el detalle de una orden desde cualquier fragmento.
     * Agrega el detalle al BackStack para que el usuario
     * pueda volver con el botón atrás.
     *
     * @param ordenId ID de la orden a mostrar
     */
    public void abrirDetalleOrden(int ordenId) {
        com.example.tecrobsys.fragmentos.ordenes.FragmentoDetalleOrden detalle =
                com.example.tecrobsys.fragmentos.ordenes.FragmentoDetalleOrden
                        .nuevaInstancia(ordenId);

        getSupportFragmentManager().beginTransaction()
                .hide(fragmentoActivo)
                .add(R.id.contenedor_fragmento, detalle, "detalle_orden")
                .addToBackStack("detalle_orden")
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        enlace = null; // Evitar memory leaks
    }
}