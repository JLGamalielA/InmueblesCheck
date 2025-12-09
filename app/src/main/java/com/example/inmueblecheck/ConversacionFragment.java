package com.example.inmueblecheck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ConversacionFragment extends Fragment {

    private ChatViewModel viewModel;
    private String chatId;
    private String titulo;
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private EditText etInput;
    private ImageButton btnSend;
    private Toolbar toolbar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Recibir y Validar Argumentos
        if (getArguments() != null) {
            chatId = getArguments().getString("chatId");
            titulo = getArguments().getString("titulo");
        }

        if (chatId == null) {
            Toast.makeText(getContext(), "Error: ID de chat perdido", Toast.LENGTH_LONG).show();
        }

        // Inicializar Vistas
        recyclerView = view.findViewById(R.id.rvChatMessages);
        etInput = view.findViewById(R.id.etMessageInput);
        btnSend = view.findViewById(R.id.btnSend);
        toolbar = view.findViewById(R.id.toolbarChat);

        // Configurar Toolbar
        toolbar.setTitle(titulo != null ? titulo : "Chat");
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        // Configurar RecyclerView
        adapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Configurar ViewModel
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        if (chatId != null) {
            viewModel.loadMessages(chatId);
        }

        // Observar Mensajes
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.setMessages(messages);
            if (!messages.isEmpty()) {
                recyclerView.smoothScrollToPosition(messages.size() - 1);
            }
        });

        //  Observador de errores
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        //  Botón Enviar
        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString();

            if (chatId == null) {
                Toast.makeText(getContext(), "No se puede enviar: ChatID es nulo", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!text.trim().isEmpty()) {
                viewModel.sendMessage(chatId, text);
                etInput.setText("");
            } else {
                Toast.makeText(getContext(), "Escribe un mensaje", Toast.LENGTH_SHORT).show();
            }
        });
    }
}