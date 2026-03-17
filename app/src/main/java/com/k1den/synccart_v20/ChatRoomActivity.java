package com.k1den.synccart_v20;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.k1den.synccart_v20.adapter.MessageAdapter;
import com.k1den.synccart_v20.models.Message;
import com.k1den.synccart_v20.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRoomActivity extends AppCompatActivity {

    private int chatId;
    private int currentUserId;
    private RecyclerView rvMessages;
    private MessageAdapter messageAdapter;
    private EditText etMessage;
    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> voiceRecognitionLauncher;

    private View bottomInputContainer;
    private ImageButton btnAiAssist;
    private TextView tvRoomTitle;
    private String originalChatTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        chatId = getIntent().getIntExtra("CHAT_ID", -1);
        originalChatTitle = getIntent().getStringExtra("CHAT_TITLE");

        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getInt("USER_ID", -1);

        tvRoomTitle = findViewById(R.id.tvRoomTitle);
        if (originalChatTitle != null) tvRoomTitle.setText(originalChatTitle);

        etMessage = findViewById(R.id.etMessage);
        ImageButton btnSend = findViewById(R.id.btnSend);

        bottomInputContainer = findViewById(R.id.bottomInputContainer);
        btnAiAssist = findViewById(R.id.btnAiAssist);

        if (btnAiAssist != null) {
            btnAiAssist.setOnClickListener(v -> enterAiSelectionMode());
        }

        voiceRecognitionLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        java.util.ArrayList<String> matches = result.getData().getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String spokenText = matches.get(0);

                            String currentText = etMessage.getText().toString();
                            if (currentText.isEmpty()) {
                                etMessage.setText(spokenText);
                            } else {
                                etMessage.setText(currentText + " " + spokenText);
                            }
                            etMessage.setSelection(etMessage.getText().length());
                        }
                    }
                }
        );

        rvMessages = findViewById(R.id.rvMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);

        messageAdapter = new MessageAdapter(currentUserId);
        rvMessages.setAdapter(messageAdapter);

        loadMessages();

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessageToServer(text);
            }
        });

        ImageButton btnOpenList = findViewById(R.id.btnOpenList);
        btnOpenList.setOnClickListener(v -> openOrCreateShoppingList());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ImageButton btnAddUser = findViewById(R.id.btnAddUser);
        btnAddUser.setOnClickListener(v -> showInviteDialog());

        ImageButton btnMic = findViewById(R.id.btnMic);
        btnMic.setOnClickListener(v -> startVoiceRecognition());
    }

    private void loadMessages() {
        RetrofitClient.getApiService().getMessages(chatId).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messageAdapter.setMessages(response.body());
                    if (messageAdapter.getItemCount() > 0) {
                        rvMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Toast.makeText(ChatRoomActivity.this, "Ошибка загрузки сообщений", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessageToServer(String text) {
        RetrofitClient.getApiService().sendMessage(chatId, currentUserId, text).enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                if (response.isSuccessful() && response.body() != null) {
                    etMessage.setText("");
                    messageAdapter.addMessage(response.body());
                    rvMessages.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                }
            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                Toast.makeText(ChatRoomActivity.this, "Ошибка отправки", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openOrCreateShoppingList() {
        RetrofitClient.getApiService().getListsByChat(chatId).enqueue(new Callback<List<com.k1den.synccart_v20.models.ShoppingList>>() {
            @Override
            public void onResponse(Call<List<com.k1den.synccart_v20.models.ShoppingList>> call, Response<List<com.k1den.synccart_v20.models.ShoppingList>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    int existingListId = response.body().get(0).getId();
                    goToShoppingList(existingListId);
                } else {
                    createNewListForChat();
                }
            }

            @Override
            public void onFailure(Call<List<com.k1den.synccart_v20.models.ShoppingList>> call, Throwable t) {
                Toast.makeText(ChatRoomActivity.this, "Ошибка сети при поиске списка", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNewListForChat() {
        RetrofitClient.getApiService().createList(chatId).enqueue(new Callback<com.k1den.synccart_v20.models.ShoppingList>() {
            @Override
            public void onResponse(Call<com.k1den.synccart_v20.models.ShoppingList> call, Response<com.k1den.synccart_v20.models.ShoppingList> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int newListId = response.body().getId();
                    goToShoppingList(newListId);
                } else {
                    Toast.makeText(ChatRoomActivity.this, "Не удалось создать список", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.k1den.synccart_v20.models.ShoppingList> call, Throwable t) {
                Toast.makeText(ChatRoomActivity.this, "Ошибка сети при создании списка", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToShoppingList(int listId) {
        android.content.Intent intent = new android.content.Intent(ChatRoomActivity.this, ShoppingListActivity.class);
        intent.putExtra("LIST_ID", listId);
        startActivity(intent);
    }

    private void showInviteDialog() {
        final EditText input = new EditText(this);
        input.setHint("Введите никнейм друга");

        new android.app.AlertDialog.Builder(this)
                .setTitle("Пригласить в чат")
                .setView(input)
                .setPositiveButton("Отправить", (dialog, which) -> {
                    String friendUsername = input.getText().toString().trim();
                    if (!friendUsername.isEmpty()) {
                        sendInviteToServer(friendUsername);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void sendInviteToServer(String friendUsername) {
        String title = ((TextView) findViewById(R.id.tvRoomTitle)).getText().toString();

        RetrofitClient.getApiService().inviteUser(chatId, friendUsername, title)
                .enqueue(new Callback<okhttp3.ResponseBody>() {
                    @Override
                    public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(ChatRoomActivity.this, "Приглашение отправлено!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ChatRoomActivity.this, "Пользователь с таким ником не найден", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                        Toast.makeText(ChatRoomActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startVoiceRecognition() {
        android.content.Intent intent = new android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Говорите сообщение...");

        try {
            voiceRecognitionLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Ваше устройство не поддерживает голосовой ввод", Toast.LENGTH_SHORT).show();
        }
    }

    private void enterAiSelectionMode() {
        tvRoomTitle.setText("Выберите сообщение");

        if (bottomInputContainer != null) bottomInputContainer.setVisibility(View.GONE);
        if (btnAiAssist != null) btnAiAssist.setVisibility(View.GONE);

        // Включаем слушатель в адаптере
        messageAdapter.setOnMessageAiClickListener((message, position) -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Отправить ИИ?")
                    .setMessage("ИИ найдет продукты в этом сообщении и добавит их в список.")
                    .setPositiveButton("Отправить", (dialog, which) -> {
                        sendAiRequestToServer(message.getContent());
                        exitAiSelectionMode();
                    })
                    .setNegativeButton("Отмена", (dialog, which) -> exitAiSelectionMode())
                    .setCancelable(false)
                    .show();
        });

        messageAdapter.notifyDataSetChanged();
    }

    private void exitAiSelectionMode() {
        if (originalChatTitle != null) tvRoomTitle.setText(originalChatTitle);

        if (bottomInputContainer != null) bottomInputContainer.setVisibility(View.VISIBLE);
        if (btnAiAssist != null) btnAiAssist.setVisibility(View.VISIBLE);

        messageAdapter.setOnMessageAiClickListener(null);
        messageAdapter.notifyDataSetChanged();
    }

    private void sendAiRequestToServer(String messageText) {
        Toast.makeText(this, "Анализируем текст...", Toast.LENGTH_SHORT).show();

        RetrofitClient.getApiService().processAiMessage(chatId, messageText).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ChatRoomActivity.this, "Продукты добавлены в список!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ChatRoomActivity.this, "Ошибка ИИ на сервере", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                Toast.makeText(ChatRoomActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }
}