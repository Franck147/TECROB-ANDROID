package com.example.tecrobsys.adaptadores;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tecrobsys.databinding.ItemOrdenBinding;
import com.example.tecrobsys.modelos.Orden;
import com.example.tecrobsys.utils.UtilEstado;
import java.util.List;

/**
 * AdaptadorOrden — Adaptador RecyclerView para mostrar tarjetas de órdenes.
 *
 * Cada tarjeta muestra:
 *   - Número de orden (ORD-0041)
 *   - Badge de estado con color dinámico según el estado
 *   - Badge de prioridad (solo si es ALTA o URGENTE)
 *   - Nombre del cliente
 *   - Equipo + desperfecto resumido
 *   - Iniciales del técnico + nombre abreviado + precio total
 *
 * Usa ViewBinding (ItemOrdenBinding) para acceso seguro a las vistas.
 * El patrón ViewHolder evita llamadas repetidas a findViewById.
 */
public class AdaptadorOrden extends RecyclerView.Adapter<AdaptadorOrden.VistaOrden> {

    // Lista de órdenes que se muestra (referencia a la lista del fragmento)
    private final List<Orden> listaOrdenes;

    // Interfaz para manejar el clic en una tarjeta de orden
    private final OnOrdenClickListener listener;

    /**
     * Interfaz funcional para el callback de clic.
     * Se implementa como lambda en el fragmento.
     */
    public interface OnOrdenClickListener {
        void alHacerClic(Orden orden);
    }

    public AdaptadorOrden(List<Orden> listaOrdenes, OnOrdenClickListener listener) {
        this.listaOrdenes = listaOrdenes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VistaOrden onCreateViewHolder(@NonNull ViewGroup padre, int tipoVista) {
        // Inflar el layout del ítem usando ViewBinding
        ItemOrdenBinding enlace = ItemOrdenBinding.inflate(
                LayoutInflater.from(padre.getContext()), padre, false);
        return new VistaOrden(enlace);
    }

    @Override
    public void onBindViewHolder(@NonNull VistaOrden holder, int posicion) {
        holder.vincular(listaOrdenes.get(posicion), listener);
    }

    @Override
    public int getItemCount() {
        return listaOrdenes.size();
    }

    // ────────────────────────────────────────────────────────────────
    //  ViewHolder — contiene y gestiona las vistas de una tarjeta
    // ────────────────────────────────────────────────────────────────
    static class VistaOrden extends RecyclerView.ViewHolder {

        private final ItemOrdenBinding enlace;

        VistaOrden(ItemOrdenBinding enlace) {
            super(enlace.getRoot());
            this.enlace = enlace;
        }

        /**
         * Vincula los datos de una Orden a las vistas de la tarjeta.
         * Se llama automáticamente para cada elemento visible.
         */
        void vincular(Orden orden, OnOrdenClickListener listener) {

            // ── Número de orden ───────────────────────────────────
            enlace.textNumeroOrden.setText(orden.getNumeroOrden());

            // ── Estado: texto + color de fondo del chip ───────────
            String estado = orden.getEstado();
            enlace.chipEstado.setText(UtilEstado.obtenerTexto(estado));
            enlace.chipEstado.setChipBackgroundColor(
                    ColorStateList.valueOf(
                            ContextCompat.getColor(
                                    enlace.getRoot().getContext(),
                                    UtilEstado.obtenerColorFondo(estado))));
            enlace.chipEstado.setTextColor(
                    ContextCompat.getColor(
                            enlace.getRoot().getContext(),
                            UtilEstado.obtenerColorTexto(estado)));

            // ── Badge de prioridad ────────────────────────────────
            String prioridad = orden.getPrioridad();
            if ("urgente".equals(prioridad)) {
                enlace.badgePrioridad.setVisibility(View.VISIBLE);
                enlace.badgePrioridad.setText("URGENTE");
            } else if ("alta".equals(prioridad)) {
                enlace.badgePrioridad.setVisibility(View.VISIBLE);
                enlace.badgePrioridad.setText("ALTA");
            } else {
                enlace.badgePrioridad.setVisibility(View.GONE);
            }

            // ── Nombre del cliente ────────────────────────────────
            if (orden.getCliente() != null) {
                enlace.textNombreCliente.setText(
                        orden.getCliente().getNombreCompleto());
            } else {
                enlace.textNombreCliente.setText("Cliente #" + orden.getClienteId());
            }

            // ── Equipo + desperfecto ──────────────────────────────
            if (orden.getEquipo() != null) {
                String equipo = orden.getEquipo().getNombreCompleto();
                String desperfecto = orden.getEquipo().getDesperfecto();
                if (desperfecto != null && !desperfecto.isEmpty()) {
                    // Truncar si es muy largo
                    if (desperfecto.length() > 50) {
                        desperfecto = desperfecto.substring(0, 47) + "...";
                    }
                    equipo += " · " + desperfecto;
                }
                enlace.textDescripcionEquipo.setText(equipo);
            } else {
                enlace.textDescripcionEquipo.setText("");
            }

            // ── Técnico: iniciales + nombre abreviado ─────────────
            if (orden.getTecnico() != null) {
                enlace.textInicialesTecnico.setText(
                        orden.getTecnico().getIniciales());
                String nombre = orden.getTecnico().getNombre();
                String apellido = orden.getTecnico().getApellido();
                String nombreAbrev = nombre != null ? nombre : "";
                if (apellido != null && !apellido.isEmpty()) {
                    nombreAbrev += " " + apellido.charAt(0) + ".";
                }
                enlace.textNombreTecnico.setText(nombreAbrev);
            } else {
                enlace.textInicialesTecnico.setText("--");
                enlace.textNombreTecnico.setText("");
            }

            // ── Precio total formateado ───────────────────────────
            enlace.textPrecioTotal.setText(
                    String.format("S/ %.2f", orden.getTotal()));

            // ── Clic en la tarjeta completa ───────────────────────
            enlace.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.alHacerClic(orden);
            });
        }
    }
}