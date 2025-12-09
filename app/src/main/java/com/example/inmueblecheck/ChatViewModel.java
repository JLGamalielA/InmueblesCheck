package com.example.inmueblecheck;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;

public class ChatViewModel extends ViewModel {
    private final ChatRepository repository = new ChatRepository();

    public LiveData<List<Chat>> getMisChats() {
        return repository.getMisChats();
    }

    public LiveData<List<Mensaje>> getMensajes(String chatId) {
        return repository.getMensajes(chatId);
    }

    public void enviarMensaje(String chatId, String texto) {
        repository.enviarMensaje(chatId, texto);
    }

    public void iniciarChat(String otroUsuarioId, String nombreInmueble, String inmuebleId, ChatRepository.OnChatCreadoListener listener) {
        repository.iniciarChat(otroUsuarioId, nombreInmueble, inmuebleId, listener);
    }
}