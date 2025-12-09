package com.example.inmueblecheck;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import java.util.ArrayList;
import java.util.List;

public class DetalleInmuebleViewModel extends AndroidViewModel {

    private final InmuebleRepository repository;
    private final MutableLiveData<String> inmuebleIdInput = new MutableLiveData<>();

    private final LiveData<Inmueble> inmueble;
    private final LiveData<List<Media>> mediaList;

    public DetalleInmuebleViewModel(@NonNull Application application) {
        super(application);
        repository = new InmuebleRepository(application);

        inmueble = Transformations.switchMap(inmuebleIdInput, id ->
                repository.getInmuebleById(id));

        mediaList = Transformations.switchMap(inmuebleIdInput, id -> {
            return AppDatabase.getDatabase(application).inmuebleDao().getMediaForInmueble(id);
        });
    }

    public void setInmuebleId(String id) {
        if (id != null) {
            inmuebleIdInput.setValue(id);
        }
    }

    public LiveData<Inmueble> getInmueble() { return inmueble; }
    public LiveData<List<Media>> getMediaList() { return mediaList; }
}