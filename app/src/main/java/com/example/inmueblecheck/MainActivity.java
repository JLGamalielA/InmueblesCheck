package com.example.inmueblecheck;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast; // Importante para feedback visual
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private NavController navController;
    private AgenteViewModel agenteViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // ViewModel compartido (scoped to Activity) para que el filtro persista
        agenteViewModel = new ViewModelProvider(this).get(AgenteViewModel.class);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            if (id == R.id.loginFragment || id == R.id.registerFragment) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                getSupportActionBar().hide();
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                getSupportActionBar().show();
                configurarMenuPorRol();
            }
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            // --- SECCIÓN MÁS ---
            if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                navController.navigate(R.id.loginFragment);
            } else if (id == R.id.nav_about) {
                navController.navigate(R.id.aboutFragment);
            }

            // --- ACCIONES ARRENDADOR ---
            else if (id == R.id.nav_crear_inmueble) {
                navController.navigate(R.id.crearInmuebleFragment);
            } else if (id == R.id.nav_mis_propiedades) {
                navController.popBackStack(R.id.gerenteDashboardFragment, false);
                navController.navigate(R.id.gerenteDashboardFragment);
            }

            // --- ACCIONES ARRENDATARIO ---
            else if (id == R.id.nav_explorar) {
                agenteViewModel.aplicarFiltro(null); // Resetear filtro al ir a explorar
                navController.popBackStack(R.id.agenteDashboardFragment, false);
                navController.navigate(R.id.agenteDashboardFragment);
            } else if (id == R.id.nav_mapa) {
                navController.navigate(R.id.mapaFragment);
            }

            // --- LÓGICA DE FILTROS ---
            else if (id == R.id.nav_filtro_venta) {
                aplicarFiltroYMostrarLista("Venta");
            } else if (id == R.id.nav_filtro_renta) {
                aplicarFiltroYMostrarLista("Renta");
            } else if (id == R.id.nav_filtro_todos) {
                aplicarFiltroYMostrarLista(null);
            }

            // --- ACCIONES ARRENDADOR ---
            else if (id == R.id.nav_crear_inmueble) {
                navController.navigate(R.id.crearInmuebleFragment);
            } else if (id == R.id.nav_mis_propiedades) {
                navController.popBackStack(R.id.gerenteDashboardFragment, false);
                navController.navigate(R.id.gerenteDashboardFragment);
            } else if (id == R.id.nav_historial) {
                // Nuevo: Navegar al historial
                navController.navigate(R.id.historialFragment);
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    // Helper para aplicar filtro y navegar
    private void aplicarFiltroYMostrarLista(String tipo) {
        agenteViewModel.aplicarFiltro(tipo);

        String mensaje = (tipo == null) ? "Mostrando todos" : "Mostrando solo: " + tipo;
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();

        // Si no estamos en la lista, ir a ella
        if (navController.getCurrentDestination().getId() != R.id.agenteDashboardFragment) {
            navController.navigate(R.id.agenteDashboardFragment);
        }
    }

    private void configurarMenuPorRol() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");

                        View headerView = navigationView.getHeaderView(0);
                        TextView tvTitle = headerView.findViewById(R.id.tvHeaderTitle);
                        TextView tvSubtitle = headerView.findViewById(R.id.tvHeaderSubtitle);

                        tvTitle.setText("InmuebleCheck");
                        tvSubtitle.setText(user.getEmail());

                        navigationView.getMenu().clear();
                        if ("arrendador".equals(role) || "gerente".equals(role)) {
                            navigationView.inflateMenu(R.menu.activity_main_drawer_arrendador);
                        } else {
                            navigationView.inflateMenu(R.menu.activity_main_drawer_arrendatario);
                        }
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, drawerLayout) || super.onSupportNavigateUp();
    }
}