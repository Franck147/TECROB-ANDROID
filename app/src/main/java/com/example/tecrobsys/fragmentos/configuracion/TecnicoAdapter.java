package com.example.tecrobsys.fragmentos.configuracion;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tecrobsys.R;
import com.example.tecrobsys.modelos.Tecnico;
import java.util.ArrayList;
import java.util.List;

public class TecnicoAdapter extends RecyclerView.Adapter<TecnicoAdapter.ViewHolder> {

    public interface OnEliminarListener {
        void onEliminar(Tecnico tecnico);
    }

    private final List<Tecnico> lista = new ArrayList<>();
    private final OnEliminarListener listener;

    public TecnicoAdapter(OnEliminarListener listener) {
        this.listener = listener;
    }

    public void actualizar(List<Tecnico> nuevaLista) {
        lista.clear();
        lista.addAll(nuevaLista);
        notifyDataSetChanged();
    }

    public void agregar(Tecnico tecnico) {
        lista.add(0, tecnico);
        notifyItemInserted(0);
    }

    public void eliminar(int tecnicoId) {
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId() == tecnicoId) {
                lista.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tecnico, parent, false);
        return new ViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(lista.get(position), listener);
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView avatar;
        private final TextView nombre;
        private final TextView email;
        private final TextView rol;
        private final TextView btnEliminar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar      = itemView.findViewById(R.id.text_avatar_tecnico);
            nombre      = itemView.findViewById(R.id.text_nombre_tecnico);
            email       = itemView.findViewById(R.id.text_email_tecnico);
            rol         = itemView.findViewById(R.id.text_rol_tecnico);
            btnEliminar = itemView.findViewById(R.id.btn_eliminar_tecnico);
        }

        void bind(Tecnico tecnico, OnEliminarListener listener) {
            String nombreCompleto = tecnico.getNombreCompleto();
            avatar.setText(nombreCompleto != null && !nombreCompleto.isEmpty()
                    ? nombreCompleto.substring(0, 1).toUpperCase() : "?");
            nombre.setText(nombreCompleto);
            email.setText(tecnico.getEmail());
            rol.setText(tecnico.esAdministrador() ? "ADMINISTRADOR" : "TÉCNICO");
            btnEliminar.setOnClickListener(v -> listener.onEliminar(tecnico));
        }
    }
}
