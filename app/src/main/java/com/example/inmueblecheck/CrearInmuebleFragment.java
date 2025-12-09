package com.example.inmueblecheck;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CrearInmuebleFragment extends Fragment {

    private GerenteViewModel viewModel;

    private TextInputEditText etDireccion, etPrecio, etContacto, etDescripcion;
    private TextInputLayout tilDireccion; // Referencia al contenedor para ocultarlo
    private RadioGroup rgTipo;
    private Button btnGuardar;
    private FloatingActionButton btnFoto;
    private MaterialSwitch switchUbicacion; // Nuestro Switch nuevo
    private TextView tvCoordenadas;
    private ImageView ivFoto;
    private ProgressBar progressBar;
    private MaterialToolbar toolbar;

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0.0;
    private double currentLon = 0.0;
    private Uri currentPhotoUri;
    private String currentPhotoPath;
    private String editInmuebleId = null;

    private ActivityResultLauncher<String> requestLocationPermissionLauncher;
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        requestLocationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) obtenerUbicacionGps();
                    else {
                        switchUbicacion.setChecked(false); // Si niega, apaga el switch
                        Toast.makeText(getContext(), "Permiso de ubicación requerido", Toast.LENGTH_SHORT).show();
                    }
                });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) dispatchTakePictureIntent();
                    else Toast.makeText(getContext(), "Permiso de cámara requerido", Toast.LENGTH_SHORT).show();
                });

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        mostrarFotoCapturada();
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_crear_inmueble, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) editInmuebleId = getArguments().getString("editInmuebleId");

        initViews(view);
        viewModel = new ViewModelProvider(requireActivity()).get(GerenteViewModel.class);

        if (editInmuebleId != null) cargarDatosEdicion(editInmuebleId);
        setupObservers();
    }

    private void initViews(View view) {
        etDireccion = view.findViewById(R.id.etDireccion);
        tilDireccion = view.findViewById(R.id.tilDireccion); // Contenedor
        etPrecio = view.findViewById(R.id.etPrecio);
        etContacto = view.findViewById(R.id.etContacto);
        etDescripcion = view.findViewById(R.id.etDescripcion);
        rgTipo = view.findViewById(R.id.rgTipo);
        btnGuardar = view.findViewById(R.id.btnGuardarInmueble);
        btnFoto = view.findViewById(R.id.btnTomarFoto);
        tvCoordenadas = view.findViewById(R.id.tvCoordenadas);
        ivFoto = view.findViewById(R.id.ivFotoPrincipal);
        switchUbicacion = view.findViewById(R.id.switchUbicacion);
        progressBar = view.findViewById(R.id.pbCrear);
        toolbar = view.findViewById(R.id.toolbarCrear);

        if (editInmuebleId != null) {
            toolbar.setTitle("Editar Propiedad");
            btnGuardar.setText("Actualizar Propiedad");
        }

        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        // LÓGICA DEL SWITCH
        switchUbicacion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Activar GPS -> Ocultar entrada manual
                tilDireccion.setVisibility(View.GONE);
                checkGpsPermission(); // Pedir permiso y obtener coordenadas
            } else {
                // Manual -> Mostrar entrada
                tilDireccion.setVisibility(View.VISIBLE);
                tvCoordenadas.setText("GPS Desactivado - Ingrese dirección manual");
            }
        });

        btnFoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnGuardar.setOnClickListener(v -> guardarInmueble());
    }

    private void checkGpsPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionGps();
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void obtenerUbicacionGps() {
        try {
            tvCoordenadas.setText("Obteniendo ubicación...");
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLon = location.getLongitude();
                    tvCoordenadas.setText(String.format(Locale.getDefault(), "Ubicación Exacta: %.5f, %.5f", currentLat, currentLon));
                    tvCoordenadas.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_primary));

                    // Opcional: Llenar internamente la dirección para mostrar algo en la lista
                    buscarDireccionDesdeCoordenadas(currentLat, currentLon);
                } else {
                    tvCoordenadas.setText("No se pudo obtener ubicación. Intente de nuevo.");
                    switchUbicacion.setChecked(false); // Regresar a manual si falla
                }
            });
        } catch (SecurityException e) { e.printStackTrace(); }
    }

    // --- GUARDADO INTELIGENTE ---
    private void guardarInmueble() {
        String direccionFinal = "";

        // 1. Si es manual (Switch OFF) -> Leer caja de texto y resolver
        if (!switchUbicacion.isChecked()) {
            String input = etDireccion.getText().toString().trim();
            if (TextUtils.isEmpty(input)) {
                etDireccion.setError("Requerido");
                return;
            }
            direccionFinal = input;

            // Intentar resolver coordenadas si el usuario puso una dirección
            resolverCoordenadasDesdeTexto(input);
        } else {
            // 2. Si es GPS (Switch ON) -> Usar lat/lon obtenidos
            if (currentLat == 0 && currentLon == 0) {
                Toast.makeText(getContext(), "Esperando señal GPS...", Toast.LENGTH_SHORT).show();
                return;
            }
            // Usamos la dirección geocodificada o un texto genérico si falló
            direccionFinal = etDireccion.getText().toString().trim();
            if (direccionFinal.isEmpty()) direccionFinal = "Ubicación por GPS";
        }

        String precioStr = etPrecio.getText().toString().trim();
        if (TextUtils.isEmpty(precioStr)) {
            etPrecio.setError("Requerido");
            return;
        }

        double precio = Double.parseDouble(precioStr);
        String contacto = etContacto.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String tipo = (rgTipo.getCheckedRadioButtonId() == R.id.rbVenta) ? "Venta" : "Renta";

        progressBar.setVisibility(View.VISIBLE);
        btnGuardar.setEnabled(false);

        viewModel.guardarInmueble(
                editInmuebleId,
                direccionFinal,
                precio,
                tipo,
                descripcion,
                contacto,
                currentLat,
                currentLon,
                currentPhotoPath
        );
    }

    // --- Helpers de Geocoding ---

    private void buscarDireccionDesdeCoordenadas(double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String dir = addresses.get(0).getAddressLine(0);
                etDireccion.setText(dir); // Llenamos el campo aunque esté oculto para guardarlo
            }
        } catch (IOException e) { }
    }

    private void resolverCoordenadasDesdeTexto(String input) {
        // Si el usuario ingresó coordenadas manuales "Lat, Lon"
        if (input.matches("^-?\\d+(\\.\\d+)?\\s*,\\s*-?\\d+(\\.\\d+)?$")) {
            try {
                String[] parts = input.split(",");
                currentLat = Double.parseDouble(parts[0].trim());
                currentLon = Double.parseDouble(parts[1].trim());
            } catch (Exception e) {}
        } else {
            // Si ingresó dirección, intentar obtener lat/lon
            try {
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(input, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    currentLat = addresses.get(0).getLatitude();
                    currentLon = addresses.get(0).getLongitude();
                }
            } catch (IOException e) {}
        }
    }

    private void cargarDatosEdicion(String id) {
        progressBar.setVisibility(View.VISIBLE);
        viewModel.getInmuebleById(id).observe(getViewLifecycleOwner(), inmueble -> {
            progressBar.setVisibility(View.GONE);
            if (inmueble != null) {
                etDireccion.setText(inmueble.getDireccion());
                etPrecio.setText(String.valueOf(inmueble.getPrecio()));
                etContacto.setText(inmueble.getDatosContacto());
                etDescripcion.setText(inmueble.getDescripcion());
                if ("Venta".equalsIgnoreCase(inmueble.getTipoTransaccion())) rgTipo.check(R.id.rbVenta);
                else rgTipo.check(R.id.rbRenta);

                currentLat = inmueble.getLatitud();
                currentLon = inmueble.getLongitud();
                currentPhotoPath = inmueble.getFotoPortada();

                if (currentPhotoPath != null) {
                    ivFoto.setPadding(0, 0, 0, 0);
                    Glide.with(this).load(currentPhotoPath).centerCrop().into(ivFoto);
                }

                // Si ya tenía coordenadas, podemos asumir que se usó GPS o manual verificado?
                // Por defecto dejamos el switch apagado para permitir edición manual, o podrías encenderlo si prefieres.
                if (currentLat != 0) {
                    tvCoordenadas.setText(String.format(Locale.getDefault(), "Guardada: %.5f, %.5f", currentLat, currentLon));
                }
            }
        });
    }

    // --- CÁMARA (Sin cambios) ---
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try { photoFile = createImageFile(); } catch (IOException ex) { return; }
            if (photoFile != null) {
                try {
                    currentPhotoUri = FileProvider.getUriForFile(requireContext(), "com.example.inmueblecheck.fileprovider", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                    takePictureLauncher.launch(takePictureIntent);
                } catch (IllegalArgumentException e) { }
            }
        }
    }
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
    }
    private void mostrarFotoCapturada() {
        if (currentPhotoUri != null) {
            currentPhotoPath = currentPhotoUri.toString();
            ivFoto.setPadding(0, 0, 0, 0);
            Glide.with(this).load(currentPhotoUri).centerCrop().into(ivFoto);
        }
    }
    private void setupObservers() {
        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            progressBar.setVisibility(View.GONE);
            if (success) {
                Toast.makeText(getContext(), "Guardado correctamente", Toast.LENGTH_SHORT).show();
                viewModel.resetSaveSuccess();
                Navigation.findNavController(getView()).popBackStack();
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                progressBar.setVisibility(View.GONE);
                btnGuardar.setEnabled(true);
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}