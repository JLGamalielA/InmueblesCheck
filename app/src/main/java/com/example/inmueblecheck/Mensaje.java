package com.example.inmueblecheck;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Mensaje {
    private String texto;
    private String remitenteId;
    @ServerTimestamp
    private Date fechaEnvio;

    public Mensaje() {}

    public Mensaje(String texto, String remitenteId) {
        this.texto = texto;
        this.remitenteId = remitenteId;
        this.fechaEnvio = new Date();
    }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getRemitenteId() { return remitenteId; }
    public void setRemitenteId(String remitenteId) { this.remitenteId = remitenteId; }
    public Date getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(Date fechaEnvio) { this.fechaEnvio = fechaEnvio; }
}