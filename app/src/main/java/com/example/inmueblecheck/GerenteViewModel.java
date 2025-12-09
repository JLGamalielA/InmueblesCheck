package com.example.inmueblecheck;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class GerenteViewModel extends AndroidViewModel {

    private final InmuebleRepository repository;
    private final InmuebleDao dao;
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();

    public GerenteViewModel(@NonNull Application application) {
        super(application);
        repository = new InmuebleRepository(application);
        dao = AppDatabase.getDatabase(application).inmuebleDao();
    }

    // Obtener activos
    public LiveData<List<Inmueble>> getMisInmueblesActivos() {
        String uid = FirebaseAuth.getInstance().getUid();
        return dao.getMisInmueblesActivos(uid);
    }

    // Obtener historial
    public LiveData<List<Inmueble>> getMisInmueblesHistorial() {
        String uid = FirebaseAuth.getInstance().getUid();
        return dao.getMisInmueblesHistorial(uid);
    }

    public LiveData<Inmueble> getInmuebleById(String id) {
        return repository.getInmuebleById(id);
    }

    // Cambiar estado (Archivar/Desarchivar)
    public void alternarEstadoInmueble(Inmueble inmueble) {
        String nuevoEstado = "disponible".equals(inmueble.getEstado()) ? "no_disponible" : "disponible";

        Executors.newSingleThreadExecutor().execute(() -> {
            dao.actualizarEstado(inmueble.getUid(), nuevoEstado);
        });
    }

    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }
    public void resetSaveSuccess() { saveSuccess.setValue(false); }
    public void clearError() { error.setValue(null); }

    public void guardarInmueble(String idToUpdate, String direccion, double precio, String tipo,
                                String descripcion, String contacto,
                                double lat, double lon, String photoUri) {

        String userId = FirebaseAuth.getInstance().getUid();
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        if (userId == null) {
            error.setValue("Sesión no válida");
            return;
        }

        Inmueble inmueble;

        if (idToUpdate == null) {
            inmueble = new Inmueble(direccion, precio, tipo, userId, userEmail);
        } else {
            inmueble = new Inmueble();
            inmueble.setUid(idToUpdate);
            inmueble.setArrendadorId(userId);
            inmueble.setArrendadorEmail(userEmail);
            inmueble.setDireccion(direccion);
            inmueble.setPrecio(precio);
            inmueble.setTipoTransaccion(tipo);
            inmueble.setEstado("disponible");
            inmueble.setStatusSync("pendiente_sync");
        }

        inmueble.setDescripcion(descripcion);
        inmueble.setDatosContacto(contacto);
        inmueble.setLatitud(lat);
        inmueble.setLongitud(lon);

        if (photoUri != null) inmueble.setFotoPortada(photoUri);

        repository.crearInmueble(inmueble);

        if (photoUri != null && !photoUri.isEmpty()) {
            Media media = new Media();
            media.setMediaId(UUID.randomUUID().toString());
            media.setInspectionId(inmueble.getUid());
            media.setItemName("Foto Principal");
            media.setLocalUri(photoUri);
            media.setType("image");
            media.setSynced(false);
            repository.insertMedia(media);
        }

        saveSuccess.setValue(true);
    }
}