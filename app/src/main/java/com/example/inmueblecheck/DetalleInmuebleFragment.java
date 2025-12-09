package com.example.inmueblecheck;

import android.content.Intent;
import android.net.Uri;
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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class DetalleInmuebleFragment extends Fragment implements OnMapReadyCallback {

    private String inmuebleId;
    private DetalleInmuebleViewModel viewModel;

    private TextView tvPrecio, tvDireccion, tvDescripcion, tvSinFotos;
    private Chip chipTipo;
    private MaterialButton btnContactar;
    private RecyclerView rvMedia;
    private ProgressBar progressBar;
    private MaterialToolbar toolbar;
    private MediaAdapter mediaAdapter;
    private GoogleMap mMap;

    private double lat = 0, lon = 0;
    private String contactoStr = "";
    private String tituloInmueble = ""; // Para el marcador

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detalle_inmueble, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            inmuebleId = getArguments().getString("inmuebleId");
        }

        if (inmuebleId == null) {
            Toast.makeText(getContext(), "Error: Inmueble no encontrado", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }

        initViews(view);

        // Inicializar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapLite);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupViewModel();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbarDetalle);
        tvPrecio = view.findViewById(R.id.tvPrecioDetalle);
        tvDireccion = view.findViewById(R.id.tvDireccionDetalle);
        tvDescripcion = view.findViewById(R.id.tvDescripcionDetalle);
        tvSinFotos = view.findViewById(R.id.tvSinFotos);
        chipTipo = view.findViewById(R.id.chipTipo);
        btnContactar = view.findViewById(R.id.btnContactar);
        rvMedia = view.findViewById(R.id.rvMediaDetalle);
        progressBar = view.findViewById(R.id.pbDetalle);

        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        mediaAdapter = new MediaAdapter(getContext());
        rvMedia.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvMedia.setAdapter(mediaAdapter);

        btnContactar.setOnClickListener(v -> contactarDueno());
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(DetalleInmuebleViewModel.class);
        viewModel.setInmuebleId(inmuebleId);
        progressBar.setVisibility(View.VISIBLE);

        viewModel.getInmueble().observe(getViewLifecycleOwner(), inmueble -> {
            progressBar.setVisibility(View.GONE);
            if (inmueble != null) {
                llenarDatos(inmueble);
            }
        });

        viewModel.getMediaList().observe(getViewLifecycleOwner(), mediaList -> {
            if (mediaList != null && !mediaList.isEmpty()) {
                mediaAdapter.setMedia(mediaList);
                tvSinFotos.setVisibility(View.GONE);
                rvMedia.setVisibility(View.VISIBLE);
            } else {
                tvSinFotos.setVisibility(View.VISIBLE);
                rvMedia.setVisibility(View.GONE);
            }
        });
    }

    private void llenarDatos(Inmueble inmueble) {
        lat = inmueble.getLatitud();
        lon = inmueble.getLongitud();
        contactoStr = inmueble.getDatosContacto();
        tituloInmueble = inmueble.getDireccion();

        tvDireccion.setText(inmueble.getDireccion());
        tvDescripcion.setText(inmueble.getDescripcion() != null && !inmueble.getDescripcion().isEmpty()
                ? inmueble.getDescripcion() : "Sin descripción adicional.");

        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
        tvPrecio.setText(format.format(inmueble.getPrecio()));

        String tipo = inmueble.getTipoTransaccion() != null ? inmueble.getTipoTransaccion().toUpperCase() : "RENTA";
        chipTipo.setText(tipo);

        // --- LÓGICA DE VISIBILIDAD PARA EL DUEÑO ---
        String currentUserId = FirebaseAuth.getInstance().getUid();

        if (currentUserId != null && currentUserId.equals(inmueble.getArrendadorId())) {
            // Si soy el dueño, oculto el botón de contactar
            btnContactar.setVisibility(View.GONE);
        } else {
            // Si soy cliente, lo muestro
            btnContactar.setVisibility(View.VISIBLE);
        }

        // Actualizar el mapa si ya está listo
        if (mMap != null && lat != 0 && lon != 0) {
            actualizarMapa();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setScrollGesturesEnabled(false); // Desactivar scroll para no interferir con la pantalla
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        // Si los datos llegaron antes que el mapa
        if (lat != 0 && lon != 0) {
            actualizarMapa();
        }
    }

    private void actualizarMapa() {
        LatLng pos = new LatLng(lat, lon);
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(pos).title(tituloInmueble));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
    }

    private void contactarDueno() {
        if (contactoStr == null || contactoStr.isEmpty()) {
            Toast.makeText(getContext(), "No hay datos de contacto disponibles.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent;
        if (contactoStr.contains("@")) {
            intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{contactoStr});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Interés en Inmueble: " + tituloInmueble);
        } else {
            intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + contactoStr));
        }

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "No se pudo abrir la app de contacto.", Toast.LENGTH_SHORT).show();
        }
    }
}