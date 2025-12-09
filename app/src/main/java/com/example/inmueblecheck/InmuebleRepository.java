package com.example.inmueblecheck;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InmuebleRepository {

    private static final String TAG = "InmuebleRepo";
    private final InmuebleDao inmuebleDao; // Usamos el nuevo DAO
    private final FirebaseFirestore db;
    private final WorkManager workManager;
    private final ExecutorService executor;

    public InmuebleRepository(Application application) {
        AppDatabase database = AppDatabase.getDatabase(application);
        inmuebleDao = database.inmuebleDao();
        db = FirebaseFirestore.getInstance();
        workManager = WorkManager.getInstance(application);
        executor = Executors.newSingleThreadExecutor();
    }

    // --- MÉTODOS PARA EL ARRENDADOR (DUEÑO) ---

    // Obtener SOLO mis propiedades
    public LiveData<List<Inmueble>> getMisInmuebles() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            refreshMisInmueblesFromFirestore(user.getUid());
            return inmuebleDao.getMyInmuebles(user.getUid());
        }
        return null; // O retornar LiveData vacío
    }

    // Guardar un nuevo inmueble (Primero local, luego intenta sync)
    public void crearInmueble(Inmueble inmueble) {
        executor.execute(() -> {
            inmuebleDao.insertInmueble(inmueble);
            Log.d(TAG, "Inmueble guardado localmente: " + inmueble.getDireccion());
            scheduleSync(); // Intenta subirlo si hay red
        });
    }
    public void insertMedia(Media media) {
        executor.execute(() -> inmuebleDao.insertMedia(media));
    }

    // Sincronizar mis datos desde la nube
    private void refreshMisInmueblesFromFirestore(String userId) {
        db.collection("inmuebles")
                .whereEqualTo("arrendadorId", userId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Error escuchando inmuebles remotos", e);
                        return;
                    }
                    if (snapshots != null) {
                        executor.execute(() -> {
                            List<Inmueble> lista = new ArrayList<>();
                            for (DocumentSnapshot doc : snapshots) {
                                Inmueble i = doc.toObject(Inmueble.class);
                                if (i != null) {
                                    i.setUid(doc.getId()); // Asegurar ID correcto
                                    i.setStatusSync("sincronizado"); // Viene de la nube
                                    lista.add(i);
                                }
                            }
                            inmuebleDao.insertAll(lista);
                        });
                    }
                });
    }

    // --- MÉTODOS PARA EL ARRENDATARIO (CLIENTE) ---

    // Ver TODOS los inmuebles disponibles
    public LiveData<List<Inmueble>> getCatalogoInmuebles() {
        refreshCatalogoFromFirestore();
        return inmuebleDao.getAllInmuebles();
    }

    private void refreshCatalogoFromFirestore() {
        db.collection("inmuebles")
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        executor.execute(() -> {
                            List<Inmueble> lista = new ArrayList<>();
                            for (DocumentSnapshot doc : snapshots) {
                                Inmueble i = doc.toObject(Inmueble.class);
                                if (i != null) {
                                    i.setUid(doc.getId());
                                    i.setStatusSync("sincronizado");
                                    lista.add(i);
                                }
                            }
                            inmuebleDao.insertAll(lista); // Cache local
                        });
                    }
                });
    }

    // --- UTILIDADES ---

    public LiveData<Inmueble> getInmuebleById(String id) {
        return inmuebleDao.getInmuebleById(id);
    }

    // Programa el Worker para subir datos pendientes
    private void scheduleSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();

        workManager.enqueue(syncRequest);
    }
}