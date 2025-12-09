package com.example.inmueblecheck;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatViewModel extends ViewModel {
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>();
    private final MutableLiveData<List<Chat>> myChats = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(); // Nuevo: Para mostrar errores en pantalla

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration messagesListener;
    private ListenerRegistration chatsListener;

    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<List<Chat>> getMyChats() { return myChats; }
    public LiveData<String> getErrorMessage() { return errorMessage; } // Getter del error

    // --- CARGAR LISTA DE CHATS ---
    public void loadMyChats() {
        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) {
            errorMessage.setValue("Error: No hay sesión de usuario.");
            return;
        }

        if (chatsListener != null) chatsListener.remove();

        chatsListener = db.collection("chats")
                .whereArrayContains("users", myId)
                .orderBy("lastTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("ChatViewModel", "Error cargando chats", error);
                        if (error.getMessage().contains("PERMISSION_DENIED")) {
                            errorMessage.setValue("Error de Permisos: Revisa las Reglas de Firestore.");
                        } else if (error.getMessage().contains("FAILED_PRECONDITION")) {
                            errorMessage.setValue("Falta Índice: Revisa el Logcat para el link de creación.");
                        } else {
                            errorMessage.setValue("Error: " + error.getMessage());
                        }
                        return;
                    }

                    if (value != null) {
                        List<Chat> lista = value.toObjects(Chat.class);
                        myChats.setValue(lista);
                        if (lista.isEmpty()) {
                        }
                    }
                });
    }

    // --- INICIAR CHAT ---
    public interface OnChatCreatedListener { void onChatIdReady(String chatId); }

    public void iniciarChat(String ownerId, String nombreInmueble, String inmuebleId, OnChatCreatedListener listener) {
        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) return;

        db.collection("chats")
                .whereArrayContains("users", myId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String existingChatId = null;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        @SuppressWarnings("unchecked")
                        List<String> users = (List<String>) doc.get("users");
                        String docInmuebleId = doc.getString("inmuebleId");

                        if (users != null && users.contains(ownerId) &&
                                docInmuebleId != null && docInmuebleId.equals(inmuebleId)) {
                            existingChatId = doc.getId();
                            break;
                        }
                    }
                    if (existingChatId != null) listener.onChatIdReady(existingChatId);
                    else crearNuevoChat(myId, ownerId, nombreInmueble, inmuebleId, listener);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Error al iniciar: " + e.getMessage());
                    listener.onChatIdReady(null);
                });
    }

    private void crearNuevoChat(String myId, String ownerId, String nombreInmueble, String inmuebleId, OnChatCreatedListener listener) {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("users", Arrays.asList(myId, ownerId));
        chatData.put("lastMessage", "Chat iniciado");
        chatData.put("lastTimestamp", FieldValue.serverTimestamp());
        chatData.put("inmuebleNombre", nombreInmueble);
        chatData.put("inmuebleId", inmuebleId);

        db.collection("chats").add(chatData)
                .addOnSuccessListener(docRef -> listener.onChatIdReady(docRef.getId()))
                .addOnFailureListener(e -> listener.onChatIdReady(null));
    }

    // --- CARGAR MENSAJES ---
    public void loadMessages(String chatId) {
        if (chatId == null) return;

        if (messagesListener != null) messagesListener.remove();

        messagesListener = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("ChatViewModel", "Error cargando mensajes", error);
                        errorMessage.setValue("Error mensajes: " + error.getMessage());
                        return;
                    }
                    if (value != null) {
                        List<Message> lista = value.toObjects(Message.class);
                        messages.setValue(lista);
                        Log.d("ChatViewModel", "Mensajes recibidos: " + lista.size());
                    }
                });
    }

    // --- ENVIAR ---
    public void sendMessage(String chatId, String text) {
        if (chatId == null || text.trim().isEmpty()) {
            errorMessage.setValue("Error: ChatID nulo o mensaje vacío");
            return;
        }

        String myId = FirebaseAuth.getInstance().getUid();
        String myEmail = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : "anónimo";

        if (myId == null) {
            errorMessage.setValue("Error: Sesión caducada");
            return;
        }

        Message msg = new Message(text, myId, myEmail);

        // Guardar mensaje
        db.collection("chats").document(chatId)
                .collection("messages")
                .add(msg)
                .addOnSuccessListener(doc -> Log.d("ChatViewModel", "Mensaje enviado: " + doc.getId()))
                .addOnFailureListener(e -> {
                    Log.e("ChatViewModel", "Fallo al enviar", e);
                    errorMessage.setValue("Error enviando: " + e.getMessage());
                });

        // Actualizar resumen del chat
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", text);
        updates.put("lastTimestamp", FieldValue.serverTimestamp());

        db.collection("chats").document(chatId).update(updates);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (messagesListener != null) messagesListener.remove();
        if (chatsListener != null) chatsListener.remove();
    }
}