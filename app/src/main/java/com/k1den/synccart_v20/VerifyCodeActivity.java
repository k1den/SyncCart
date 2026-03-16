package com.k1den.synccart_v20;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.k1den.synccart_v20.models.User;
import com.k1den.synccart_v20.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyCodeActivity extends AppCompatActivity {

    private EditText etCode;
    private Button btnVerifyCode; // Новое имя кнопки
    private String userEmail;
    private String userUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_code);

        // Получаем данные с прошлого экрана
        userEmail = getIntent().getStringExtra("EMAIL");
        userUsername = getIntent().getStringExtra("USERNAME");

        // ИЗМЕНЕНИЕ 1: Обновленный ID текста и оживление кнопки Назад
        TextView tvCodeSubtitle = findViewById(R.id.tvCodeSubtitle);
        if (tvCodeSubtitle != null) {
            tvCodeSubtitle.setText("Код отправлен на:\n" + userEmail);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        etCode = findViewById(R.id.etCode);

        // ИЗМЕНЕНИЕ 2: Новый ID кнопки
        btnVerifyCode = findViewById(R.id.btnVerifyCode);

        btnVerifyCode.setOnClickListener(v -> {
            String code = etCode.getText().toString().trim();
            if (code.length() != 6) {
                Toast.makeText(this, "Введите 6 цифр", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyCodeOnServer(code);
        });
    }

    private void verifyCodeOnServer(String code) {
        btnVerifyCode.setEnabled(false);
        btnVerifyCode.setText("Проверка...");

        RetrofitClient.getApiService().verifyCode(userEmail, userUsername, code).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                btnVerifyCode.setEnabled(true);
                btnVerifyCode.setText("Подтвердить");

                if (response.isSuccessful() && response.body() != null) {
                    User registeredUser = response.body();
                    Toast.makeText(VerifyCodeActivity.this, "Привет, " + registeredUser.getUsername() + "!", Toast.LENGTH_LONG).show();

                    android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    prefs.edit()
                            .putInt("USER_ID", registeredUser.getId())
                            .putString("USERNAME", registeredUser.getUsername())
                            .apply();

                    Intent intent = new Intent(VerifyCodeActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(VerifyCodeActivity.this, "Неверный код", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                btnVerifyCode.setEnabled(true);
                btnVerifyCode.setText("Подтвердить");
                Toast.makeText(VerifyCodeActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                Log.e("API_ERROR", t.getMessage());
            }
        });
    }
}