package com.k1den.synccart_v20.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.k1den.synccart_v20.R;
import com.k1den.synccart_v20.models.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages = new ArrayList<>();

    // --- ДЛЯ ИИ: Добавляем слушатель кликов ---
    public interface OnMessageAiClickListener {
        void onMessageClick(Message message, int position);
    }

    private OnMessageAiClickListener aiListener;

    public void setOnMessageAiClickListener(OnMessageAiClickListener listener) {
        this.aiListener = listener;
    }
    // ----------------------------------------

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message msg = messages.get(position);
        holder.tvContent.setText(msg.getContent());
        holder.tvAuthor.setText("UserID: " + msg.getUserId());

        // --- ДЛЯ ИИ: Логика клика по сообщению ---
        holder.itemView.setOnClickListener(v -> {
            if (aiListener != null) {
                // Если слушатель установлен (т.е. мы в режиме выбора), отправляем событие в Активити
                aiListener.onMessageClick(msg, position);
            }
        });

        // Меняем фон сообщения, чтобы показать, что оно кликабельно (по желанию)
        if (aiListener != null) {
            holder.itemView.setBackgroundColor(0xFFE0E0E0); // Серый цвет выбора
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
        // -----------------------------------------
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvAuthor;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvMessageContent);
            tvAuthor = itemView.findViewById(R.id.tvAuthorId);
        }
    }
}