package com.example.inmueblecheck;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private List<Chat> chatList = new ArrayList<>();
    private final OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    public ChatListAdapter(OnChatClickListener listener) {
        this.listener = listener;
    }

    public void setChats(List<Chat> chats) {
        this.chatList = chats;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_list, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.bind(chatList.get(position), listener);
    }

    @Override
    public int getItemCount() { return chatList.size(); }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvLastMessage, tvDate;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvChatTitle);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvDate = itemView.findViewById(R.id.tvChatDate);
        }

        public void bind(Chat chat, OnChatClickListener listener) {
            tvTitle.setText(chat.getInmuebleNombre() != null ? chat.getInmuebleNombre() : "Chat sin título");
            tvLastMessage.setText(chat.getLastMessage());

            if (chat.getLastTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
                tvDate.setText(sdf.format(chat.getLastTimestamp()));
            } else {
                tvDate.setText("");
            }

            itemView.setOnClickListener(v -> listener.onChatClick(chat));
        }
    }
}