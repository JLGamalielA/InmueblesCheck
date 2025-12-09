package com.example.inmueblecheck;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // Importante
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InmuebleAdapter extends RecyclerView.Adapter<InmuebleAdapter.InmuebleViewHolder> {

    private List<Inmueble> listaInmuebles = new ArrayList<>();
    private OnInmuebleClickListener clickListener;
    private OnEditarClickListener editarListener; // Nuevo listener

    public interface OnInmuebleClickListener {
        void onInmuebleClick(Inmueble inmueble);
    }

    public interface OnEditarClickListener { // Nueva interfaz
        void onEditarClick(Inmueble inmueble);
    }

    public void setOnInmuebleClickListener(OnInmuebleClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnEditarClickListener(OnEditarClickListener listener) { // Setter para editar
        this.editarListener = listener;
    }

    public void setInmuebles(List<Inmueble> inmuebles) {
        this.listaInmuebles = inmuebles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InmuebleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inmueble, parent, false);
        return new InmuebleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InmuebleViewHolder holder, int position) {
        holder.bind(listaInmuebles.get(position), clickListener, editarListener);
    }

    @Override
    public int getItemCount() {
        return listaInmuebles != null ? listaInmuebles.size() : 0;
    }

    static class InmuebleViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDireccion, tvPrecio, tvTipo;
        private ImageView ivFoto;
        private ImageButton btnEditar; // Referencia al botón

        public InmuebleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDireccion = itemView.findViewById(R.id.tvDireccion);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvTipo = itemView.findViewById(R.id.tvStatus);
            ivFoto = itemView.findViewById(R.id.ivInmuebleFoto);
            btnEditar = itemView.findViewById(R.id.btnEditar);
        }

        public void bind(Inmueble inmueble, OnInmuebleClickListener listener, OnEditarClickListener editListener) {
            Context context = itemView.getContext();

            tvDireccion.setText(inmueble.getDireccion());

            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
            String precioStr = format.format(inmueble.getPrecio());
            tvPrecio.setText(precioStr);

            String tipo = inmueble.getTipoTransaccion() != null ? inmueble.getTipoTransaccion() : "N/A";
            tvTipo.setText(tipo.toUpperCase());

            if ("Venta".equalsIgnoreCase(tipo)) {
                tvTipo.setTextColor(Color.parseColor("#D32F2F"));
                tvTipo.setBackgroundColor(Color.parseColor("#FFEBEE"));
            } else {
                tvTipo.setTextColor(Color.parseColor("#1976D2"));
                tvTipo.setBackgroundColor(Color.parseColor("#E3F2FD"));
            }

            if (inmueble.getFotoPortada() != null && !inmueble.getFotoPortada().isEmpty()) {
                Glide.with(context)
                        .load(inmueble.getFotoPortada())
                        .placeholder(R.drawable.ic_home_black)
                        .error(R.drawable.ic_home_black)
                        .centerCrop()
                        .into(ivFoto);
            } else {
                ivFoto.setImageResource(R.drawable.ic_home_black);
            }

            // Lógica del botón Editar
            if (editListener != null) {
                btnEditar.setVisibility(View.VISIBLE);
                btnEditar.setOnClickListener(v -> editListener.onEditarClick(inmueble));
            } else {
                btnEditar.setVisibility(View.GONE); // Ocultar si no hay listener (para el cliente)
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onInmuebleClick(inmueble);
            });
        }
    }
}