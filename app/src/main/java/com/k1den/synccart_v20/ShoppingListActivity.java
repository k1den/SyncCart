package com.k1den.synccart_v20;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.k1den.synccart_v20.adapter.ProductAdapter;
import com.k1den.synccart_v20.models.ListItem;
import com.k1den.synccart_v20.network.RetrofitClient;

import org.json.JSONArray;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShoppingListActivity extends AppCompatActivity {

    private int listId;
    private RecyclerView rvProducts;
    private ProductAdapter adapter;
    private AutoCompleteTextView etProductName;
    private java.util.Map<String, String> categoryMap = new java.util.HashMap<>();
    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> voiceRecognitionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list);

        listId = getIntent().getIntExtra("LIST_ID", -1);
        if (listId == -1) {
            Toast.makeText(this, "Ошибка: Список не найден!", Toast.LENGTH_SHORT).show();
            finish(); // Закрываем экран, если произошла ошибка
            return;
        }

        rvProducts = findViewById(R.id.rvProducts);
        etProductName = findViewById(R.id.etProductName);
        android.widget.ImageButton btnAddProduct = findViewById(R.id.btnAddProduct);
        android.widget.ImageButton btnMic = findViewById(R.id.btnMic);

        voiceRecognitionLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        java.util.ArrayList<String> matches = result.getData().getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String spokenText = matches.get(0);
                            processVoiceInput(spokenText);
                        }
                    }
                }
        );

        btnMic.setOnClickListener(v -> startVoiceRecognition());

        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter();
        rvProducts.setAdapter(adapter);

        adapter.setOnItemToggleListener((item, position) -> toggleProductOnServer(item));
        adapter.setOnItemDeleteListener((item, position) -> deleteProductFromServer(item.getId()));
        adapter.setOnAssignClickListener((item, position) -> showAssignDialog(item));

        setupAutoComplete();

        loadProductsFromServer();

        btnAddProduct.setOnClickListener(v -> {
            String name = etProductName.getText().toString().trim();
            if (!name.isEmpty()) {
                String category = categoryMap.containsKey(name) ? categoryMap.get(name) : "Другое";
                addProductToServer(name, category);
            }
        });
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMap).setOnClickListener(v -> {
            Intent intent = new Intent(ShoppingListActivity.this, StoreMapActivity.class);
            startActivity(intent);
        });
    }

    private void setupAutoComplete() {
        List<String> suggestions = new ArrayList<>();
        try {
            InputStream is = getAssets().open("products.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String jsonStr = new String(buffer, "UTF-8");

            org.json.JSONObject rootObject = new org.json.JSONObject(jsonStr);

            org.json.JSONArray jsonArray = rootObject.getJSONArray("products");
            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONObject productObj = jsonArray.getJSONObject(i);
                String pName = productObj.getString("name");
                String pCategory = productObj.optString("category", "Разное");

                suggestions.add(pName);
                categoryMap.put(pName, pCategory);
            }

            ArrayAdapter<String> autoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, suggestions);
            etProductName.setAdapter(autoAdapter);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка чтения products.json: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadProductsFromServer() {
        RetrofitClient.getApiService().getItems(listId).enqueue(new Callback<List<ListItem>>() {
            @Override
            public void onResponse(Call<List<ListItem>> call, Response<List<ListItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setItems(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<ListItem>> call, Throwable t) {
            }
        });
    }

    private void addProductToServer(String name, String category) {
        RetrofitClient.getApiService().addItem(listId, name, category).enqueue(new Callback<ListItem>() {
            @Override
            public void onResponse(Call<ListItem> call, Response<ListItem> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.addItem(response.body());
                    etProductName.setText("");

                    Toast.makeText(ShoppingListActivity.this, "Добавлено!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ShoppingListActivity.this, "Ошибка: Список с ID=" + listId + " не существует?", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ListItem> call, Throwable t) {
                Toast.makeText(ShoppingListActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleProductOnServer(ListItem item) {
        RetrofitClient.getApiService().toggleItemStatus(item.getId()).enqueue(new Callback<ListItem>() {
            @Override
            public void onResponse(Call<ListItem> call, Response<ListItem> response) {
                if (response.isSuccessful()) {
                    loadProductsFromServer();
                }
            }

            @Override
            public void onFailure(Call<ListItem> call, Throwable t) {
            }
        });
    }

    private void deleteProductFromServer(int itemId) {
        RetrofitClient.getApiService().deleteItem(itemId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ShoppingListActivity.this, "Удалено", Toast.LENGTH_SHORT).show();
                    loadProductsFromServer();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ShoppingListActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startVoiceRecognition() {
        android.content.Intent intent = new android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Перечислите продукты (например: молоко, хлеб и сыр)");

        try {
            voiceRecognitionLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Голосовой ввод не поддерживается", Toast.LENGTH_SHORT).show();
        }
    }

    private void processVoiceInput(String text) {
        String[] items = text.split("(?i),|\\s+и\\s+|\\s+а также\\s+");

        int addedCount = 0;

        for (String item : items) {
            String cleanName = item.trim();
            if (!cleanName.isEmpty()) {
                cleanName = cleanName.substring(0, 1).toUpperCase() + cleanName.substring(1).toLowerCase();

                String category = categoryMap.containsKey(cleanName) ? categoryMap.get(cleanName) : "Другое";

                addProductToServer(cleanName, category);
                addedCount++;
            }
        }

        Toast.makeText(this, "Распознано товаров: " + addedCount, Toast.LENGTH_SHORT).show();
    }

    private void showAssignDialog(ListItem item) {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Кто купит? (оставьте пустым для сброса)");
        if (item.getAssigneeName() != null) {
            input.setText(item.getAssigneeName());
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("Назначить ответственного")
                .setView(input)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    assignProductOnServer(item.getId(), name.isEmpty() ? null : name);
                })
                .setNeutralButton("Я куплю", (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                    String myName = prefs.getString("USERNAME", "Я");
                    assignProductOnServer(item.getId(), myName);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void assignProductOnServer(int itemId, String assigneeName) {
        RetrofitClient.getApiService().assignItem(itemId, assigneeName).enqueue(new Callback<ListItem>() {
            @Override
            public void onResponse(Call<ListItem> call, Response<ListItem> response) {
                if (response.isSuccessful()) loadProductsFromServer();
            }

            @Override
            public void onFailure(Call<ListItem> call, Throwable t) {
            }
        });
    }
}