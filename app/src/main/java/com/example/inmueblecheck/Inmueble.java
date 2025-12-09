package com.example.inmueblecheck;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

@Entity(tableName = "inmuebles_table")
public class Inmueble {

    @PrimaryKey
    @NonNull
    @Exclude
    private String uid = "";
    private String arrendadorId;
    private String arrendadorEmail;
    private String datosContacto;
    private String direccion;
    private String descripcion;
    private double precio;
    private String tipoTransaccion;
    private String fotoPortada;
    private String estado;
    private String statusSync;
    private double latitud;
    private double longitud;

    @ServerTimestamp
    private Date fechaCreacion;

    public Inmueble() {

    }

    public Inmueble(String direccion, double precio, String tipoTransaccion, String arrendadorId, String arrendadorEmail) {
        this.uid = java.util.UUID.randomUUID().toString();
        this.direccion = direccion;
        this.precio = precio;
        this.tipoTransaccion = tipoTransaccion;
        this.arrendadorId = arrendadorId;
        this.arrendadorEmail = arrendadorEmail;
        this.estado = "disponible";
        this.statusSync = "pendiente_sync";
        this.fechaCreacion = new Date();
    }

    // --- Getters y Setters ---
    @Exclude public String getUid() { return uid; }
    public void setUid(@NonNull String uid) { this.uid = uid; }

    public String getArrendadorId() { return arrendadorId; }
    public void setArrendadorId(String arrendadorId) { this.arrendadorId = arrendadorId; }

    public String getArrendadorEmail() { return arrendadorEmail; }
    public void setArrendadorEmail(String arrendadorEmail) { this.arrendadorEmail = arrendadorEmail; }

    public String getDatosContacto() { return datosContacto; }
    public void setDatosContacto(String datosContacto) { this.datosContacto = datosContacto; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public String getTipoTransaccion() { return tipoTransaccion; }
    public void setTipoTransaccion(String tipoTransaccion) { this.tipoTransaccion = tipoTransaccion; }

    public String getFotoPortada() { return fotoPortada; }
    public void setFotoPortada(String fotoPortada) { this.fotoPortada = fotoPortada; }

    // Getter/Setter del nuevo campo
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getStatusSync() { return statusSync; }
    public void setStatusSync(String statusSync) { this.statusSync = statusSync; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    public Date getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Date fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}