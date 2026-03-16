package com.k1den.synccart_v20.network; // Оставь свой пакет

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://192.168.1.7:8080/";
    private static Retrofit retrofit;

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {

            // 1. Создаем кастомный клиент и увеличиваем время ожидания до 60 секунд
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS) // Время на подключение
                    .readTimeout(60, TimeUnit.SECONDS)    // Время ожидания ответа
                    .writeTimeout(60, TimeUnit.SECONDS)   // Время на отправку данных
                    .build();

            // 2. Добавляем этот клиент в наш Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient) // <-- Подключили наши настройки тайм-аута
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getRetrofitInstance().create(ApiService.class);
    }
}