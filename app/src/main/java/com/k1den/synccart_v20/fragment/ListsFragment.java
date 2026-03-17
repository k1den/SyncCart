package com.k1den.synccart_v20.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.k1den.synccart_v20.R;
import com.k1den.synccart_v20.ShoppingListActivity;
import com.k1den.synccart_v20.adapter.ChatAdapter;
import com.k1den.synccart_v20.models.ShoppingList;
import com.k1den.synccart_v20.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListsFragment extends Fragment {

    private RecyclerView rvLists;
    private FloatingActionButton fabAddList;
    private ChatAdapter listAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        android.widget.TextView tvTitle = view.findViewById(R.id.tvChatsTitle);
        tvTitle.setText("Мои личные списки");

        rvLists = view.findViewById(R.id.rvChats);
        fabAddList = view.findViewById(R.id.fabAddChat);

        rvLists.setLayoutManager(new LinearLayoutManager(getContext()));
        listAdapter = new ChatAdapter();
        rvLists.setAdapter(listAdapter);

        listAdapter.setOnChatClickListener(chat -> {
            Intent intent = new Intent(getContext(), ShoppingListActivity.class);
            intent.putExtra("LIST_ID", chat.getId());
            startActivity(intent);
        });

        listAdapter.setOnChatLongClickListener(chat -> {
            new android.app.AlertDialog.Builder(getContext())
                    .setTitle("Удалить список?")
                    .setMessage("Этот список будет удален навсегда.")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        RetrofitClient.getApiService().deleteList(chat.getId()).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                loadStandaloneLists(); // Обновляем список
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                            }
                        });
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        fabAddList.setOnClickListener(v -> createStandaloneList());

        loadStandaloneLists();
        view.findViewById(R.id.btnInvites).setVisibility(View.GONE);

        return view;
    }

    private void loadStandaloneLists() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("USER_ID", -1);
        if (userId == -1) return;

        RetrofitClient.getApiService().getStandaloneLists(userId).enqueue(new Callback<List<ShoppingList>>() {
            @Override
            public void onResponse(Call<List<ShoppingList>> call, Response<List<ShoppingList>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<com.k1den.synccart_v20.models.Chat> mockChats = new java.util.ArrayList<>();
                    for (ShoppingList sl : response.body()) {
                        com.k1den.synccart_v20.models.Chat c = new com.k1den.synccart_v20.models.Chat();
                        c.setId(sl.getId());
                        c.setTitle("Личный список #" + sl.getId());
                        mockChats.add(c);
                    }
                    listAdapter.setChats(mockChats);
                }
            }

            @Override
            public void onFailure(Call<List<ShoppingList>> call, Throwable t) {
            }
        });
    }

    private void createStandaloneList() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("USER_ID", -1);
        if (userId == -1) return;

        RetrofitClient.getApiService().createStandaloneList(userId).enqueue(new Callback<ShoppingList>() {
            @Override
            public void onResponse(Call<ShoppingList> call, Response<ShoppingList> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Личный список создан!", Toast.LENGTH_SHORT).show();
                    loadStandaloneLists();
                }
            }

            @Override
            public void onFailure(Call<ShoppingList> call, Throwable t) {
            }
        });
    }
}