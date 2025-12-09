package com.example.inmueblecheck;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface InmuebleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertInmueble(Inmueble inmueble);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Inmueble> inmuebles);
    @Query("SELECT * FROM inmuebles_table WHERE estado = 'disponible' ORDER BY fechaCreacion DESC")
    LiveData<List<Inmueble>> getAllInmuebles();
    @Query("SELECT * FROM inmuebles_table WHERE arrendadorId = :userId AND estado = 'disponible' ORDER BY fechaCreacion DESC")
    LiveData<List<Inmueble>> getMisInmueblesActivos(String userId);
    @Query("SELECT * FROM inmuebles_table WHERE arrendadorId = :userId AND estado <> 'disponible' ORDER BY fechaCreacion DESC")
    LiveData<List<Inmueble>> getMisInmueblesHistorial(String userId);
    @Query("SELECT * FROM inmuebles_table WHERE uid = :inmuebleId")
    LiveData<Inmueble> getInmuebleById(String inmuebleId);
    @Query("UPDATE inmuebles_table SET statusSync = 'sincronizado' WHERE uid = :uid")
    void markAsSynced(String uid);
    @Query("UPDATE inmuebles_table SET estado = :nuevoEstado, statusSync = 'pendiente_sync' WHERE uid = :uid")
    void actualizarEstado(String uid, String nuevoEstado);

    // --- SYNC ---
    @Query("SELECT * FROM inmuebles_table WHERE statusSync = 'pendiente_sync'")
    List<Inmueble> getInmueblesPendientesDeSubida();

    // --- MEDIA ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMedia(Media media);
    @Query("SELECT * FROM media_table WHERE inspectionId = :inmuebleId")
    LiveData<List<Media>> getMediaForInmueble(String inmuebleId);
    @Query("SELECT * FROM media_table WHERE inspectionId = :inmuebleId AND isSynced = 0")
    List<Media> getMediaToSync(String inmuebleId);
    @Query("SELECT * FROM media_table WHERE inspectionId = :inmuebleId AND itemName = 'Foto Principal' LIMIT 1")
    Media getFotoPrincipal(String inmuebleId);
    @Update
    void updateMedia(Media media);
}