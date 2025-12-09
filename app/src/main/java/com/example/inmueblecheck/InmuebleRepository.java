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
    private final InmuebleDao inmuebleDao;
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

    // --- MÉTODOS PARA EL ARRENDADOR  ---

    // Obtener SOLO mis propiedades ACTIVAS
    public LiveData<List<Inmueble>> getMisInmuebles() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            refreshMisInmueblesFromFirestore(user.getUid());
            // CORRECCIÓN: Usamos el nuevo nombre del método en el DAO
            return inmuebleDao.getMisInmueblesActivos(user.getUid());
        }
        return null;
    }

    // Obtener historial
    public LiveData<List<Inmueble>> getMisInmueblesHistorial() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return inmuebleDao.getMisInmueblesHistorial(user.getUid());
        }
        return null;
    }

    public void crearInmueble(Inmueble inmueble) {
        executor.execute(() -> {
            inmuebleDao.insertInmueble(inmueble);
            Log.d(TAG, "Inmueble guardado localmente: " + inmueble.getDireccion());
            scheduleSync();
        });
    }

    public void insertMedia(Media media) {
        executor.execute(() -> inmuebleDao.insertMedia(media));
    }

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
                                    i.setUid(doc.getId());
                                    i.setStatusSync("sincronizado");
                                    // Asegurar que tenga estado si viene de versiones viejas
                                    if (i.getEstado() == null) i.setEstado("disponible");
                                    lista.add(i);
                                }
                            }
                            inmuebleDao.insertAll(lista);
                        });
                    }
                });
    }

    // --- MÉTODOS PARA EL ARRENDATARIO ---

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
                                    if (i.getEstado() == null) i.setEstado("disponible");
                                    lista.add(i);
                                }
                            }
                            inmuebleDao.insertAll(lista);
                        });
                    }
                });
    }

    // --- UTILIDADES ---

    public LiveData<Inmueble> getInmuebleById(String id) {
        return inmuebleDao.getInmuebleById(id);
    }

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