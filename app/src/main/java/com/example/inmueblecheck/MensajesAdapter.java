package com.example.inmueblecheck;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MensajesAdapter extends RecyclerView.Adapter<MensajesAdapter.MensajeViewHolder> {
    private List<Mensaje> mensajes = new ArrayList<>();
    private final String currentUserId;

    public MensajesAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMensajes(List<Mensaje> mensajes) {
        this.mensajes = mensajes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MensajeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Usamos un layout simple integrado aquí para no crear otro XML
        // Pero idealmente se usa item_mensaje.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mensaje_simple, parent, false);
        return new MensajeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MensajeViewHolder holder, int position) {
        Mensaje msg = mensajes.get(position);
        holder.tvMensaje.setText(msg.getTexto());

        // Alinear a derecha si es mío, izquierda si es otro
        if (msg.getRemitenteId() != null && msg.getRemitenteId().equals(currentUserId)) {
            holder.container.setGravity(Gravity.END);
            holder.tvMensaje.setBackgroundResource(R.drawable.bg_mensaje_mio); // Necesitas crear este drawable
        } else {
            holder.container.setGravity(Gravity.START);
            holder.tvMensaje.setBackgroundResource(R.drawable.bg_mensaje_otro); // Y este
        }
    }

    @Override
    public int getItemCount() { return mensajes.size(); }

    static class MensajeViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        TextView tvMensaje;
        public MensajeViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.containerMensaje);
            tvMensaje = itemView.findViewById(R.id.tvTextoMensaje);
        }
    }
}