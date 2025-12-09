package com.example.inmueblecheck;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class MapaFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private AgenteViewModel viewModel;
    private ChipGroup cgTipo;
    private Chip chipPrecio;
    private boolean esArrendador = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mapa, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cgTipo = view.findViewById(R.id.cgTipo);
        chipPrecio = view.findViewById(R.id.chipPrecio);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        viewModel = new ViewModelProvider(requireActivity()).get(AgenteViewModel.class);

        checkUserRole();
        setupFilters();
    }

    private void checkUserRole() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String role = doc.getString("role");
                            esArrendador = "arrendador".equals(role) || "gerente".equals(role);
                            observarDatos();
                        }
                    });
        }
    }

    private void setupFilters() {
        // Filtro Tipo (Venta/Renta)
        cgTipo.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipVenta) {
                viewModel.setFiltroTipo("Venta");
            } else if (checkedId == R.id.chipRenta) {
                viewModel.setFiltroTipo("Renta");
            } else {
                viewModel.setFiltroTipo(null);
            }
        });

        // Filtro Precio (Diálogo Personalizado)
        chipPrecio.setOnClickListener(v -> mostrarDialogoRangoPrecio());
    }

    private void mostrarDialogoRangoPrecio() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_filtro_precio, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etMin = dialogView.findViewById(R.id.etMinimo);
        EditText etMax = dialogView.findViewById(R.id.etMaximo);
        Button btnAplicar = dialogView.findViewById(R.id.btnAplicar);
        Button btnLimpiar = dialogView.findViewById(R.id.btnLimpiar);

        btnAplicar.setOnClickListener(v -> {
            String minStr = etMin.getText().toString().trim();
            String maxStr = etMax.getText().toString().trim();

            double min = TextUtils.isEmpty(minStr) ? 0 : Double.parseDouble(minStr);
            double max = TextUtils.isEmpty(maxStr) ? -1 : Double.parseDouble(maxStr);

            viewModel.setRangoPrecio(min, max);

            // Actualizar etiqueta del botón
            if (max == -1) chipPrecio.setText("Más de $" + (int)min);
            else chipPrecio.setText("$" + (int)min + " - $" + (int)max);

            dialog.dismiss();
        });

        btnLimpiar.setOnClickListener(v -> {
            viewModel.limpiarFiltroPrecio();
            chipPrecio.setText("Precio");
            dialog.dismiss();
        });

        dialog.show();
    }

    private void observarDatos() {
        if (esArrendador) {
            viewModel.getMisPropiedadesFiltradas().observe(getViewLifecycleOwner(), this::actualizarMapa);
        } else {
            viewModel.getCatalogoFiltrado().observe(getViewLifecycleOwner(), this::actualizarMapa);
        }
    }

    private void actualizarMapa(List<Inmueble> inmuebles) {
        if (mMap == null) return;
        mMap.clear();

        if (inmuebles != null && !inmuebles.isEmpty()) {
            LatLng firstPos = null;
            for (Inmueble i : inmuebles) {
                if (i.getLatitud() != 0) {
                    LatLng pos = new LatLng(i.getLatitud(), i.getLongitud());
                    if (firstPos == null) firstPos = pos;

                    float color = "Venta".equalsIgnoreCase(i.getTipoTransaccion())
                            ? BitmapDescriptorFactory.HUE_RED
                            : BitmapDescriptorFactory.HUE_AZURE;

                    mMap.addMarker(new MarkerOptions()
                                    .position(pos)
                                    .title("$" + i.getPrecio())
                                    .snippet(i.getDireccion())
                                    .icon(BitmapDescriptorFactory.defaultMarker(color)))
                            .setTag(i.getUid());
                }
            }
            if (firstPos != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstPos, 12));
            }
        } else {
            Toast.makeText(getContext(), "No hay resultados para este filtro", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        observarDatos();

        mMap.setOnInfoWindowClickListener(marker -> {
            String uid = (String) marker.getTag();
            if (uid != null) {
                Bundle args = new Bundle();
                args.putString("inmuebleId", uid);
                Navigation.findNavController(requireView()).navigate(R.id.detalleInmuebleFragment, args);
            }
        });
    }
}