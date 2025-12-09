package com.example.inmueblecheck;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;

public class ListaChatsFragment extends Fragment {

    private ChatViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView tvNoChats;
    private ChatsAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_chats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rvChats);
        tvNoChats = view.findViewById(R.id.tvNoChats);
        MaterialToolbar toolbar = view.findViewById(R.id.toolbarChats);

        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        setupRecyclerView();
        setupObservers();
    }

    private void setupRecyclerView() {
        adapter = new ChatsAdapter(chat -> {
            Bundle args = new Bundle();
            args.putString("chatId", chat.getId());
            args.putString("titulo", chat.getNombreInmueble());
            Navigation.findNavController(requireView()).navigate(R.id.action_global_conversacionFragment, args);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getMisChats().observe(getViewLifecycleOwner(), chats -> {
            if (chats == null || chats.isEmpty()) {
                tvNoChats.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                tvNoChats.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.setChats(chats);
            }
        });
    }
}