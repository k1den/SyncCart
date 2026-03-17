package com.k1den.synccart_v20.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.k1den.synccart_v20.ChatRoomActivity;
import com.k1den.synccart_v20.InvitationsActivity;
import com.k1den.synccart_v20.R;
import com.k1den.synccart_v20.adapter.ChatAdapter;
import com.k1den.synccart_v20.models.Chat;
import com.k1den.synccart_v20.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatsFragment extends Fragment {

    private RecyclerView rvChats;
    private FloatingActionButton fabAddChat;
    private ChatAdapter chatAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        rvChats = view.findViewById(R.id.rvChats);
        fabAddChat = view.findViewById(R.id.fabAddChat);

        rvChats.setLayoutManager(new LinearLayoutManager(getContext()));
        chatAdapter = new ChatAdapter();
        rvChats.setAdapter(chatAdapter);

        chatAdapter.setOnChatClickListener(chat -> {
            android.content.Intent intent = new android.content.Intent(getContext(), ChatRoomActivity.class);
            intent.putExtra("CHAT_ID", chat.getId());
            intent.putExtra("CHAT_TITLE", chat.getTitle());
            startActivity(intent);
        });

        chatAdapter.setOnChatLongClickListener(chat -> {
            new android.app.AlertDialog.Builder(getContext())
                    .setTitle("Удалить чат?")
                    .setMessage("Чат будет удален для всех участников. Это действие нельзя отменить.")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        RetrofitClient.getApiService().deleteChat(chat.getId()).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                loadChatsFromServer(); // Обновляем список
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                            }
                        });
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        fabAddChat.setOnClickListener(v -> showAddChatDialog());

        loadChatsFromServer();

        view.findViewById(R.id.btnInvites).setOnClickListener(v -> {
            startActivity(new android.content.Intent(getContext(), InvitationsActivity.class));
        });

        return view;
    }

    private void loadChatsFromServer() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("USER_ID", -1);
        if (currentUserId == -1) return;

        RetrofitClient.getApiService().getMyChats(currentUserId)
                .enqueue(new Callback<List<Chat>>() {
                    @Override
                    public void onResponse(Call<List<Chat>> call, Response<List<Chat>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Chat> chats = response.body();
                            chatAdapter.setChats(chats);

                            View emptyState = getView().findViewById(R.id.emptyStateLayout);
                            View rvChats = getView().findViewById(R.id.rvChats);

                            if (chats.isEmpty()) {
                                emptyState.setVisibility(View.VISIBLE);
                                rvChats.setVisibility(View.GONE);
                            } else {
                                emptyState.setVisibility(View.GONE);
                                rvChats.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Chat>> call, Throwable t) {
                        Toast.makeText(getContext(), "Ошибка загрузки чатов", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAddChatDialog() {
        final EditText input = new EditText(getContext());
        input.setHint("Введите название чата");

        new AlertDialog.Builder(getContext())
                .setTitle("Новый чат")
                .setView(input)
                .setPositiveButton("Создать", (dialog, which) -> {
                    String chatName = input.getText().toString().trim();
                    if (!chatName.isEmpty()) {
                        createNewChatOnServer(chatName);
                    } else {
                        Toast.makeText(getContext(), "Название не может быть пустым", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void createNewChatOnServer(String chatName) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("USER_ID", -1);

        if (currentUserId == -1) return;

        RetrofitClient.getApiService().createChat(chatName, currentUserId)
                .enqueue(new Callback<Chat>() {
                    @Override
                    public void onResponse(Call<Chat> call, Response<Chat> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(getContext(), "Чат создан!", Toast.LENGTH_SHORT).show();
                            // ВАЖНО: После успешного создания заново скачиваем чаты, чтобы список обновился
                            loadChatsFromServer();
                        } else {
                            Toast.makeText(getContext(), "Ошибка создания чата", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Chat> call, Throwable t) {
                        Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}