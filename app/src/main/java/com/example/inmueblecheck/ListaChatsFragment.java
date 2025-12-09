package com.example.inmueblecheck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ListaChatsFragment extends Fragment {

    private ChatViewModel viewModel;
    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_chats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rvListaChats);
        progressBar = view.findViewById(R.id.pbListaChats);
        tvEmpty = view.findViewById(R.id.tvEmptyChats);

        adapter = new ChatListAdapter(chat -> {
            Bundle args = new Bundle();
            args.putString("chatId", chat.getChatId());
            args.putString("titulo", chat.getInmuebleNombre());
            Navigation.findNavController(view).navigate(R.id.action_global_conversacionFragment, args);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // Cargar
        progressBar.setVisibility(View.VISIBLE);
        viewModel.loadMyChats();

        viewModel.getMyChats().observe(getViewLifecycleOwner(), chats -> {
            progressBar.setVisibility(View.GONE);
            if (chats != null && !chats.isEmpty()) {
                adapter.setChats(chats);
                tvEmpty.setVisibility(View.GONE);
            } else {
                adapter.setChats(new ArrayList<>());
                tvEmpty.setText("No tienes chats aún.");
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && errorMsg.contains("Error")) {
                progressBar.setVisibility(View.GONE);
                if (errorMsg.contains("index")) {
                    tvEmpty.setText("Falta Índice en Firebase.");
                } else {
                    tvEmpty.setText(errorMsg);
                }
                tvEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }
}