package com.example.inmueblecheck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

public class ConversacionFragment extends Fragment {

    private ChatViewModel viewModel;
    private String chatId;
    private String titulo;
    private EditText etMensaje;
    private MensajesAdapter adapter;
    private RecyclerView rvMensajes;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_conversacion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            chatId = getArguments().getString("chatId");
            titulo = getArguments().getString("titulo");
        }

        MaterialToolbar toolbar = view.findViewById(R.id.toolbarConversacion);
        toolbar.setTitle(titulo != null ? titulo : "Chat");
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        etMensaje = view.findViewById(R.id.etMensaje);
        ImageButton btnEnviar = view.findViewById(R.id.btnEnviar);
        rvMensajes = view.findViewById(R.id.rvMensajes);

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        String currentUserId = FirebaseAuth.getInstance().getUid();

        adapter = new MensajesAdapter(currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // Mensajes empiezan desde abajo
        rvMensajes.setLayoutManager(layoutManager);
        rvMensajes.setAdapter(adapter);

        setupObservers();

        btnEnviar.setOnClickListener(v -> {
            String texto = etMensaje.getText().toString().trim();
            if (!texto.isEmpty()) {
                viewModel.enviarMensaje(chatId, texto);
                etMensaje.setText("");
            }
        });
    }

    private void setupObservers() {
        viewModel.getMensajes(chatId).observe(getViewLifecycleOwner(), mensajes -> {
            if (mensajes != null) {
                adapter.setMensajes(mensajes);
                rvMensajes.scrollToPosition(mensajes.size() - 1);
            }
        });
    }
}