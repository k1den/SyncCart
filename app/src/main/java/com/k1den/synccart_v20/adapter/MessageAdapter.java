package com.k1den.synccart_v20.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.k1den.synccart_v20.R;
import com.k1den.synccart_v20.models.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages = new ArrayList<>();
    private int currentUserId;

    public interface OnMessageAiClickListener {
        void onMessageClick(Message message, int position);
    }

    private OnMessageAiClickListener aiListener;

    public void setOnMessageAiClickListener(OnMessageAiClickListener listener) {
        this.aiListener = listener;
    }

    public MessageAdapter(int currentUserId) {
        this.currentUserId = currentUserId;
    }

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

        LinearLayout mainContainer = holder.itemView.findViewById(R.id.msgMainContainer);
        LinearLayout bubble = holder.itemView.findViewById(R.id.msgBubble);

        if (msg.getUserId() == currentUserId) {
            mainContainer.setGravity(Gravity.END);
            bubble.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DCF8C6")));
            holder.tvAuthor.setVisibility(View.GONE);
        } else {
            mainContainer.setGravity(Gravity.START);
            bubble.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            holder.tvAuthor.setVisibility(View.VISIBLE);

            String name = msg.getSenderName() != null ? msg.getSenderName() : "Пользователь";
            holder.tvAuthor.setText(name);

            int userColor = generateUserColor(msg.getUserId());
            holder.tvAuthor.setTextColor(userColor);
        }

        holder.itemView.setOnClickListener(v -> {
            if (aiListener != null) aiListener.onMessageClick(msg, position);
        });

        if (aiListener != null) {
            bubble.setAlpha(0.6f);
        } else {
            bubble.setAlpha(1.0f);
        }
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
            tvAuthor = itemView.findViewById(R.id.tvAuthorName);
        }
    }

    private static int generateUserColor(int userId) {
        int hue = (userId * 1103515245 + 12345) % 360;
        return Color.HSVToColor(new float[]{hue, 0.7f, 0.85f});
    }
}