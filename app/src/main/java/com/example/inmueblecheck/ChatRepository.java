package com.example.inmueblecheck;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId;

    public ChatRepository() {
        currentUserId = FirebaseAuth.getInstance().getUid();
    }

    public LiveData<List<Chat>> getMisChats() {
        MutableLiveData<List<Chat>> chats = new MutableLiveData<>();
        if (currentUserId == null) return chats;

        db.collection("chats")
                .whereArrayContains("participantes", currentUserId)
                .orderBy("ultimaActualizacion", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("ChatRepo", "Error al obtener chats", error);
                        return;
                    }
                    if (value == null) return;

                    List<Chat> lista = new ArrayList<>();
                    for (var doc : value) {
                        Chat chat = doc.toObject(Chat.class);
                        chat.setId(doc.getId());
                        lista.add(chat);
                    }
                    chats.setValue(lista);
                });
        return chats;
    }

    public LiveData<List<Mensaje>> getMensajes(String chatId) {
        MutableLiveData<List<Mensaje>> mensajes = new MutableLiveData<>();

        db.collection("chats").document(chatId).collection("mensajes")
                .orderBy("fechaEnvio", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value == null) return;

                    List<Mensaje> lista = new ArrayList<>();
                    for (var doc : value) {
                        lista.add(doc.toObject(Mensaje.class));
                    }
                    mensajes.setValue(lista);
                });
        return mensajes;
    }

    public void enviarMensaje(String chatId, String texto) {
        if (currentUserId == null || texto.isEmpty()) return;

        Mensaje mensaje = new Mensaje(texto, currentUserId);

        db.collection("chats").document(chatId).collection("mensajes").add(mensaje);
        db.collection("chats").document(chatId).update(
                "ultimoMensaje", texto,
                "ultimaActualizacion", new java.util.Date()
        );
    }

    public void iniciarChat(String otroUsuarioId, String nombreInmueble, String inmuebleId, OnChatCreadoListener listener) {
        if (currentUserId == null) {
            Log.e("ChatRepo", "Usuario no autenticado");
            listener.onChatCreado(null);
            return;
        }

        // IMPORTANTE: Esta consulta suele requerir un índice compuesto en Firestore.
        // Si no existe, fallará y verás un link en el Logcat para crearlo.
        db.collection("chats")
                .whereEqualTo("inmuebleId", inmuebleId)
                .whereArrayContains("participantes", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Chat existente encontrado
                        String existingId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        Log.d("ChatRepo", "Chat existente encontrado: " + existingId);
                        listener.onChatCreado(existingId);
                    } else {
                        // Crear nuevo chat
                        Log.d("ChatRepo", "Creando nuevo chat...");
                        Chat nuevoChat = new Chat(Arrays.asList(currentUserId, otroUsuarioId), nombreInmueble, inmuebleId);
                        db.collection("chats").add(nuevoChat)
                                .addOnSuccessListener(docRef -> {
                                    Log.d("ChatRepo", "Chat creado con éxito: " + docRef.getId());
                                    listener.onChatCreado(docRef.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("ChatRepo", "Error al crear nuevo chat", e);
                                    listener.onChatCreado(null);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // Aquí es donde cae si falta el índice o hay error de permisos
                    Log.e("ChatRepo", "Error al buscar chat existente. REVISA EL LOGCAT POR SI FALTA UN ÍNDICE.", e);
                    listener.onChatCreado(null);
                });
    }

    public interface OnChatCreadoListener {
        void onChatCreado(String chatId);
    }
}