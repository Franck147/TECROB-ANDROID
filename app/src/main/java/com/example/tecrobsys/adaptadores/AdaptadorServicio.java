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
 * Cada tarjeta muestra:
 *   - Nombre del servicio
 *   - Categoría formateada
 *   - Precio base: "S/ 35.00"
 *   - Botón "+" para agregar a una orden
 */
public class AdaptadorServicio extends
        RecyclerView.Adapter<AdaptadorServicio.VistaServicio> {

    private final List<ServicioCatalogo> listaServicios;
    private final OnServicioClickListener listener;

    /** Callback para el clic en el botón "+" de cada servicio. */
    public interface OnServicioClickListener {
        void alAgregarServicio(ServicioCatalogo servicio);
    }

    public AdaptadorServicio(List<ServicioCatalogo> lista,
                             OnServicioClickListener listener) {
        this.listaServicios = lista;
        this.listener = listener;
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
        holder.vincular(listaServicios.get(posicion), listener);
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
                      OnServicioClickListener listener) {

            enlace.textNombreServicio.setText(servicio.getNombre());
            enlace.textCategoriaServicio.setText(
                    servicio.getCategoriaFormateada());
            enlace.textPrecioServicio.setText(
                    servicio.getPrecioFormateado());

            // Botón "+" para agregar el servicio a una orden
            enlace.botonAgregarServicio.setOnClickListener(v -> {
                if (listener != null) listener.alAgregarServicio(servicio);
            });
        }
    }
}