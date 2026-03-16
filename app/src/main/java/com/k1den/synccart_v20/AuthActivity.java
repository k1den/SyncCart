package com.k1den.synccart_v20;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// Импортируем наш клиент (убедитесь, что пути совпадают)
import com.k1den.synccart_v20.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthActivity extends AppCompatActivity {

    private EditText etEmail, etUsername;
    private Button btnVerifyEmail; // ИЗМЕНИЛИ ИМЯ ПЕРЕМЕННОЙ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth); // Проверь, чтобы имя файла совпадало с твоим XML

        // --- ДОБАВЛЯЕМ ПРОВЕРКУ АВТОВХОДА ---
        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        if (prefs.contains("USER_ID")) {
            // Пользователь уже авторизован! Идем на главный экран
            startActivity(new Intent(this, MainActivity.class));
            finish(); // Закрываем экран авторизации
            return;   // Останавливаем выполнение остального кода в onCreate
        }
        // ------------------------------------

        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);

        // ИЗМЕНИЛИ ID КНОПКИ НА НОВЫЙ ИЗ ДИЗАЙНА
        btnVerifyEmail = findViewById(R.id.btnVerifyEmail);

        btnVerifyEmail.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String username = etUsername.getText().toString().trim();

            if (email.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            requestVerificationCode(email, username);
        });
    }

    private void requestVerificationCode(String email, String username) {
        btnVerifyEmail.setEnabled(false);
        btnVerifyEmail.setText("Отправка...");

        RetrofitClient.getApiService().requestVerificationCode(email, username)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        btnVerifyEmail.setEnabled(true);
                        btnVerifyEmail.setText("Продолжить"); // Возвращаем текст из дизайна

                        if (response.isSuccessful()) {
                            // ВОТ ОН - ПЕРЕХОД НА СЛЕДУЮЩИЙ ЭКРАН
                            Intent intent = new Intent(AuthActivity.this, VerifyCodeActivity.class);
                            intent.putExtra("EMAIL", email);
                            intent.putExtra("USERNAME", username); // Передаем никнейм тоже, он нужен для регистрации
                            startActivity(intent);
                        } else {
                            Toast.makeText(AuthActivity.this, "Ошибка сервера", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        btnVerifyEmail.setEnabled(true);
                        btnVerifyEmail.setText("Продолжить");
                        Toast.makeText(AuthActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}