package com.example.tecrobsys.adaptadores;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tecrobsys.databinding.ItemServicioBinding;
import com.example.tecrobsys.modelos.ServicioCatalogo;
import java.util.List;

/**
 * AdaptadorServicio — Adaptador RecyclerView para el catálogo de servicios.
 *
 * Gestos:
 *   - Toque en botón "+"     → agregar a orden (OnServicioClickListener)
 *   - Toque largo en tarjeta → menú editar/eliminar (OnServicioLongClickListener)
 */
public class AdaptadorServicio extends
        RecyclerView.Adapter<AdaptadorServicio.VistaServicio> {

    private final List<ServicioCatalogo> listaServicios;
    private final OnServicioClickListener listenerClick;
    private final OnServicioLongClickListener listenerLong;

    /** Clic en botón "+" — agregar servicio a una orden */
    public interface OnServicioClickListener {
        void alAgregarServicio(ServicioCatalogo servicio);
    }

    /** Toque largo en la tarjeta — abrir menú editar/eliminar */
    public interface OnServicioLongClickListener {
        void alTocarLargo(ServicioCatalogo servicio);
    }

    public AdaptadorServicio(List<ServicioCatalogo> lista,
                             OnServicioClickListener listenerClick,
                             OnServicioLongClickListener listenerLong) {
        this.listaServicios = lista;
        this.listenerClick  = listenerClick;
        this.listenerLong   = listenerLong;
    }

    @NonNull
    @Override
    public VistaServicio onCreateViewHolder(@NonNull ViewGroup padre,
                                            int tipoVista) {
        ItemServicioBinding enlace = ItemServicioBinding.inflate(
                LayoutInflater.from(padre.getContext()), padre, false);
        return new VistaServicio(enlace);
    }

    @Override
    public void onBindViewHolder(@NonNull VistaServicio holder, int posicion) {
        holder.vincular(listaServicios.get(posicion),
                listenerClick, listenerLong);
    }

    @Override
    public int getItemCount() {
        return listaServicios.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────
    static class VistaServicio extends RecyclerView.ViewHolder {

        private final ItemServicioBinding enlace;

        VistaServicio(ItemServicioBinding enlace) {
            super(enlace.getRoot());
            this.enlace = enlace;
        }

        void vincular(ServicioCatalogo servicio,
                      OnServicioClickListener listenerClick,
                      OnServicioLongClickListener listenerLong) {

            enlace.textNombreServicio.setText(servicio.getNombre());
            enlace.textCategoriaServicio.setText(
                    servicio.getCategoriaFormateada());
            enlace.textPrecioServicio.setText(
                    servicio.getPrecioFormateado());

            // Toque en "+" → agregar a orden
            enlace.botonAgregarServicio.setOnClickListener(v -> {
                if (listenerClick != null)
                    listenerClick.alAgregarServicio(servicio);
            });

            // Toque largo en la tarjeta → editar/eliminar
            enlace.getRoot().setOnLongClickListener(v -> {
                if (listenerLong != null) {
                    listenerLong.alTocarLargo(servicio);
                    return true; // consumir el evento
                }
                return false;
            });
        }
    }
}