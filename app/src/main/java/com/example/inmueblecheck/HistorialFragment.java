package com.example.inmueblecheck;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;

public class HistorialFragment extends Fragment {

    private GerenteViewModel viewModel;
    private RecyclerView recyclerView;
    private InmuebleAdapter adapter;
    private ProgressBar progressBar;
    private MaterialToolbar toolbar;
    private TextView tvTitulo, tvSubtitulo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gerente_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewInspecciones);
        progressBar = view.findViewById(R.id.progressBar);
        toolbar = view.findViewById(R.id.toolbarGerente);
        tvTitulo = view.findViewById(R.id.tvTituloDashboard);
        tvSubtitulo = view.findViewById(R.id.tvSubtitulo);

        // Personalizamos textos
        tvTitulo.setText("Historial");
        tvSubtitulo.setText("Propiedades rentadas o vendidas");

        viewModel = new ViewModelProvider(requireActivity()).get(GerenteViewModel.class);
        progressBar.setVisibility(View.VISIBLE);

        setupRecyclerView();
        setupObservers();
    }

    private void setupRecyclerView() {
        adapter = new InmuebleAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Restaurar a Activos
        adapter.setOnEstadoClickListener(inmueble -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("¿Reactivar propiedad?")
                    .setMessage("Este inmueble volverá a estar disponible para todos.")
                    .setPositiveButton("Sí", (d, w) -> {
                        viewModel.alternarEstadoInmueble(inmueble);
                        Toast.makeText(getContext(), "Inmueble reactivado", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

    }

    private void setupObservers() {
        // Observamos HISTORIAL
        viewModel.getMisInmueblesHistorial().observe(getViewLifecycleOwner(), inmuebles -> {
            adapter.setInmuebles(inmuebles);
            progressBar.setVisibility(View.GONE);
        });
    }
}