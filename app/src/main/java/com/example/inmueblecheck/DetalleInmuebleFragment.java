package com.example.inmueblecheck;

import android.content.Intent;
import android.net.Uri;
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

    private static final String TAG = "DetalleInmueble";
    private String inmuebleId;
    private DetalleInmuebleViewModel viewModel;
    private Inmueble currentInmueble;

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
    private String tituloInmueble = "";

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
            Toast.makeText(getContext(), "Error: ID no recibido", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }

        initViews(view);

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

        // Listener del botón Contactar
        btnContactar.setOnClickListener(v -> contactarDueno());
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(DetalleInmuebleViewModel.class);
        viewModel.setInmuebleId(inmuebleId);
        progressBar.setVisibility(View.VISIBLE);

        // Observar datos del inmueble
        viewModel.getInmueble().observe(getViewLifecycleOwner(), inmueble -> {
            progressBar.setVisibility(View.GONE);
            if (inmueble != null) {
                this.currentInmueble = inmueble;
                llenarDatos(inmueble);
            } else {
                Log.e(TAG, "Inmueble cargado es nulo");
                Toast.makeText(getContext(), "No se pudo cargar la información", Toast.LENGTH_SHORT).show();
            }
        });

        // Observar fotos
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

        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId != null && currentUserId.equals(inmueble.getArrendadorId())) {
            btnContactar.setVisibility(View.GONE);
        } else {
            btnContactar.setVisibility(View.VISIBLE);
        }

        // Actualizar mapa si ya cargó
        if (mMap != null && lat != 0 && lon != 0) {
            actualizarMapa();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

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
        Log.d(TAG, "Botón Contactar presionado");

        if (currentInmueble == null) {
            Log.e(TAG, "Error: currentInmueble es null al presionar contactar");
            Toast.makeText(getContext(), "Cargando datos, espera un momento...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentInmueble.getArrendadorId() == null) {
            Log.e(TAG, "Error: arrendadorId es null");
            Toast.makeText(getContext(), "Error: No se puede identificar al dueño", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Iniciando chat con: " + currentInmueble.getArrendadorId());

        btnContactar.setEnabled(false);
        btnContactar.setText("Abriendo chat...");

        ChatViewModel chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        chatViewModel.iniciarChat(
                currentInmueble.getArrendadorId(),
                currentInmueble.getDireccion(),
                currentInmueble.getUid(),
                chatId -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            btnContactar.setEnabled(true);
                            btnContactar.setText("Contactar Arrendador");

                            if (chatId != null) {
                                Log.d(TAG, "Chat iniciado/encontrado con ID: " + chatId);
                                Bundle args = new Bundle();
                                args.putString("chatId", chatId);
                                args.putString("titulo", currentInmueble.getDireccion());

                                try {
                                    Navigation.findNavController(requireView()).navigate(R.id.action_global_conversacionFragment, args);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error CRÍTICO navegando al fragmento de conversación", e);
                                    Toast.makeText(getContext(), "Error de navegación al chat", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.e(TAG, "Error: chatId devuelto es null (Fallo en creación)");
                                Toast.makeText(getContext(), "Error al iniciar la conversación", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
        );
    }
}