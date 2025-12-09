package com.example.inmueblecheck;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

public class Chat {
    private String id;
    private List<String> participantes; // UIDs de los usuarios
    private String ultimoMensaje;
    private String nombreInmueble; // Contexto de la conversación
    private String inmuebleId;
    @ServerTimestamp
    private Date ultimaActualizacion;

    public Chat() {}

    public Chat(List<String> participantes, String nombreInmueble, String inmuebleId) {
        this.participantes = participantes;
        this.nombreInmueble = nombreInmueble;
        this.inmuebleId = inmuebleId;
        this.ultimaActualizacion = new Date();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public List<String> getParticipantes() { return participantes; }
    public void setParticipantes(List<String> participantes) { this.participantes = participantes; }
    public String getUltimoMensaje() { return ultimoMensaje; }
    public void setUltimoMensaje(String ultimoMensaje) { this.ultimoMensaje = ultimoMensaje; }
    public String getNombreInmueble() { return nombreInmueble; }
    public void setNombreInmueble(String nombreInmueble) { this.nombreInmueble = nombreInmueble; }
    public String getInmuebleId() { return inmuebleId; }
    public void setInmuebleId(String inmuebleId) { this.inmuebleId = inmuebleId; }
    public Date getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(Date ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
}