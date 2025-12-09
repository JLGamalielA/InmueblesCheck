package com.example.inmueblecheck;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private final InmuebleDao dao;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        AppDatabase appDb = AppDatabase.getDatabase(context);
        dao = appDb.inmuebleDao();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Iniciando sincronización...");

        try {
            // 1. Buscar inmuebles pendientes
            List<Inmueble> inmueblesPendientes = dao.getInmueblesPendientesDeSubida();
            if (inmueblesPendientes.isEmpty()) {
                return Result.success();
            }

            for (Inmueble inmueble : inmueblesPendientes) {
                // 2. Primero subimos las fotos (para tener las URLs listas)
                uploadMedia(inmueble.getUid());

                // 3. Buscamos si hay una "Foto Principal" ya subida con URL remota
                Media fotoPrincipal = dao.getFotoPrincipal(inmueble.getUid());
                if (fotoPrincipal != null && fotoPrincipal.getRemoteUri() != null) {
                    // CRÍTICO: Reemplazamos la ruta local con la URL de internet antes de subir a Firestore
                    inmueble.setFotoPortada(fotoPrincipal.getRemoteUri());
                    Log.d(TAG, "URL de portada actualizada para la nube: " + fotoPrincipal.getRemoteUri());
                }

                // 4. Subimos el inmueble con la URL correcta
                uploadInmuebleData(inmueble);
            }

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error en sincronización", e);
            return Result.retry();
        }
    }

    private void uploadMedia(String inmuebleId) throws ExecutionException, InterruptedException {
        List<Media> mediaToSync = dao.getMediaToSync(inmuebleId);

        for (Media media : mediaToSync) {
            try {
                Uri fileUri = Uri.parse(media.getLocalUri());
                StorageReference storageRef = storage.getReference()
                        .child("inmuebles_fotos")
                        .child(inmuebleId)
                        .child(new File(fileUri.getPath()).getName());

                UploadTask uploadTask = storageRef.putFile(fileUri);

                // Obtener URL de descarga
                Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return storageRef.getDownloadUrl();
                });

                Uri downloadUri = Tasks.await(urlTask);

                // Actualizar DB Local con la URL remota
                media.setRemoteUri(downloadUri.toString());
                media.setSynced(true);
                dao.updateMedia(media);

            } catch (Exception e) {
                Log.e(TAG, "Fallo al subir archivo: " + media.getLocalUri(), e);
                // No lanzamos excepción para intentar con los siguientes archivos
            }
        }
    }

    private void uploadInmuebleData(Inmueble inmueble) throws ExecutionException, InterruptedException {
        // Marcamos como sincronizado antes de subir para evitar bucles si la UI se actualiza
        inmueble.setStatusSync("sincronizado");

        Task<Void> task = db.collection("inmuebles")
                .document(inmueble.getUid())
                .set(inmueble);

        Tasks.await(task);

        if (task.isSuccessful()) {
            dao.markAsSynced(inmueble.getUid());
            Log.d(TAG, "Inmueble " + inmueble.getUid() + " sincronizado.");
        } else {
            throw new ExecutionException(task.getException());
        }
    }
}