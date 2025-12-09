package com.example.inmueblecheck;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

public class GerenteDashboardFragment extends Fragment {

    private GerenteViewModel viewModel;
    private RecyclerView recyclerView;
    private InmuebleAdapter adapter;
    private ProgressBar progressBar;
    private MaterialToolbar toolbar;
    // Eliminada la variable 'fab' porque ya no existe en el XML

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gerente_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewInspecciones);
        progressBar = view.findViewById(R.id.progressBar);
        // Eliminada la línea: fab = view.findViewById(R.id.fabCrearInspeccion);

        toolbar = view.findViewById(R.id.toolbarGerente);

        viewModel = new ViewModelProvider(requireActivity()).get(GerenteViewModel.class);
        progressBar.setVisibility(View.VISIBLE);

        setupRecyclerView();
        // setupClickListeners ya no es necesario para el FAB, y el menú se maneja en MainActivity
        setupObservers();
    }

    private void setupRecyclerView() {
        adapter = new InmuebleAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Eliminado el listener de scroll que encogía el FAB

        // Navegación al Detalle
        adapter.setOnInmuebleClickListener(inmueble -> {
            if (inmueble != null && inmueble.getUid() != null) {
                Bundle args = new Bundle();
                args.putString("inmuebleId", inmueble.getUid());
                try {
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_gerente_to_detalle, args);
                } catch (Exception e) {
                    Log.e("NavError", "Error al navegar", e);
                }
            }
        });

        // Navegación a Editar
        adapter.setOnEditarClickListener(inmueble -> {
            if (inmueble != null && inmueble.getUid() != null) {
                Bundle args = new Bundle();
                args.putString("editInmuebleId", inmueble.getUid());
                try {
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_gerenteDashboardFragment_to_crearInmuebleFragment, args);
                } catch (Exception e) {
                    Log.e("NavError", "Error al navegar a editar", e);
                }
            }
        });
    }

    private void setupObservers() {
        viewModel.getMisInmuebles().observe(getViewLifecycleOwner(), inmuebles -> {
            adapter.setInmuebles(inmuebles);
            progressBar.setVisibility(View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });
    }
}