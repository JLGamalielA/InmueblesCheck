package com.example.inmueblecheck;

import android.os.Bundle;
import android.util.Log;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class AgenteDashboardFragment extends Fragment {

    private AgenteViewModel viewModel;
    private RecyclerView recyclerView;
    private InmuebleAdapter adapter;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvWelcomeTitle, tvWelcomeSubtitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agente_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rvInspecciones);
        progressBar = view.findViewById(R.id.progressBarAgente);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshAgente);
        tvWelcomeTitle = view.findViewById(R.id.tvWelcomeTitle);
        tvWelcomeSubtitle = view.findViewById(R.id.tvWelcomeSubtitle);
        swipeRefreshLayout.setColorSchemeResources(R.color.blue_primary);
        viewModel = new ViewModelProvider(requireActivity()).get(AgenteViewModel.class);

        setupRecyclerView();
        setupObservers();

        // Recargar al deslizar hacia abajo
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (viewModel != null) {
                viewModel.recargarDatos();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new InmuebleAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Clic en la tarjeta para ver detalles
        adapter.setOnInmuebleClickListener(inmueble -> {
            if (!isAdded()) return;

            try {
                if (inmueble != null && inmueble.getUid() != null) {
                    Bundle args = new Bundle();
                    args.putString("inmuebleId", inmueble.getUid());

                    NavController navController = Navigation.findNavController(requireView());
                    navController.navigate(R.id.action_agente_to_detalle, args);
                }
            } catch (Exception e) {
                Log.e("AgenteDashboard", "Error navegando al detalle", e);
                Toast.makeText(getContext(), "No se pudo abrir el detalle", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupObservers() {
        // Observamos la lista filtrada del ViewModel
        viewModel.getCatalogoFiltrado().observe(getViewLifecycleOwner(), inmuebles -> {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);

            if (inmuebles != null) {
                adapter.setInmuebles(inmuebles);

                // Mensaje si no hay resultados
                if (inmuebles.isEmpty()) {
                    tvWelcomeSubtitle.setText("No hay propiedades con estos filtros.");
                } else {
                    tvWelcomeSubtitle.setText("Encuentra tu lugar ideal");
                }
            } else {
                Toast.makeText(getContext(), "Cargando catálogo...", Toast.LENGTH_SHORT).show();
            }
        });
    }
}