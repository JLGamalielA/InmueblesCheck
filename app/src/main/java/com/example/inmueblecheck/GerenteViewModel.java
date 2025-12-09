package com.example.inmueblecheck;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;
import java.util.UUID;

public class GerenteViewModel extends AndroidViewModel {

    private final InmuebleRepository repository;
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();

    public GerenteViewModel(@NonNull Application application) {
        super(application);
        repository = new InmuebleRepository(application);
    }

    public LiveData<List<Inmueble>> getMisInmuebles() {
        return repository.getMisInmuebles();
    }

    // Método para obtener un solo inmueble (para editar)
    public LiveData<Inmueble> getInmuebleById(String id) {
        return repository.getInmuebleById(id);
    }

    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }
    public void resetSaveSuccess() { saveSuccess.setValue(false); }
    public void clearError() { error.setValue(null); }

    /**
     * Guarda (Crea o Actualiza) un inmueble.
     * @param idToUpdate Si es null, crea uno nuevo. Si tiene valor, actualiza ese ID.
     */
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
            // MODO CREAR: Nuevo objeto con ID nuevo
            inmueble = new Inmueble(direccion, precio, tipo, userId, userEmail);
        } else {
            // MODO EDITAR: Reconstruimos el objeto conservando el ID original
            // Nota: Al usar Room @Insert(REPLACE), esto sobrescribirá el registro existente
            inmueble = new Inmueble();
            inmueble.setUid(idToUpdate); // IMPORTANTE: Usar el mismo ID
            inmueble.setArrendadorId(userId);
            inmueble.setArrendadorEmail(userEmail);
            inmueble.setDireccion(direccion);
            inmueble.setPrecio(precio);
            inmueble.setTipoTransaccion(tipo);
            inmueble.setStatusSync("pendiente_sync"); // Marcar para resubir cambios
            // La fecha de creación idealmente se conserva, pero Firestore server timestamp la manejará
        }

        // Setear campos comunes
        inmueble.setDescripcion(descripcion);
        inmueble.setDatosContacto(contacto);
        inmueble.setLatitud(lat);
        inmueble.setLongitud(lon);

        if (photoUri != null) {
            inmueble.setFotoPortada(photoUri);
        }

        // 1. Guardar/Actualizar en DB Local
        repository.crearInmueble(inmueble); // "crearInmueble" usa INSERT OR REPLACE, sirve para ambos

        // 2. Gestionar Foto en tabla Media
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