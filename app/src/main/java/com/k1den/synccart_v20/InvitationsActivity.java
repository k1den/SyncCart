package com.k1den.synccart_v20;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.k1den.synccart_v20.models.ChatInvitation;
import com.k1den.synccart_v20.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvitationsActivity extends AppCompatActivity {

    private RecyclerView rvInvitations;
    private InviteAdapter adapter;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getInt("USER_ID", -1);

        rvInvitations = findViewById(R.id.rvInvitations);
        rvInvitations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InviteAdapter();
        rvInvitations.setAdapter(adapter);

        loadInvitations();
    }

    private void loadInvitations() {
        RetrofitClient.getApiService().getInvites(currentUserId).enqueue(new Callback<List<ChatInvitation>>() {
            @Override
            public void onResponse(Call<List<ChatInvitation>> call, Response<List<ChatInvitation>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setInvites(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<ChatInvitation>> call, Throwable t) {
            }
        });
    }

    private class InviteAdapter extends RecyclerView.Adapter<InviteAdapter.InviteViewHolder> {
        private List<ChatInvitation> invites = new ArrayList<>();

        public void setInvites(List<ChatInvitation> invites) {
            this.invites = invites;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invitation, parent, false);
            return new InviteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull InviteViewHolder holder, int position) {
            ChatInvitation invite = invites.get(position);
            holder.tvText.setText("Приглашение в чат: " + invite.getChatTitle());

            // Кнопка ПРИНЯТЬ
            holder.btnAccept.setOnClickListener(v -> {
                RetrofitClient.getApiService().acceptInvite(invite.getId()).enqueue(new Callback<okhttp3.ResponseBody>() {
                    @Override
                    public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                        Toast.makeText(InvitationsActivity.this, "Добро пожаловать в чат!", Toast.LENGTH_SHORT).show();
                        loadInvitations();
                    }

                    @Override
                    public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                    }
                });
            });

            holder.btnDecline.setOnClickListener(v -> {
                RetrofitClient.getApiService().declineInvite(invite.getId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        loadInvitations();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                    }
                });
            });
        }

        @Override
        public int getItemCount() {
            return invites.size();
        }

        class InviteViewHolder extends RecyclerView.ViewHolder {
            TextView tvText;
            ImageButton btnAccept, btnDecline;

            public InviteViewHolder(@NonNull View itemView) {
                super(itemView);
                tvText = itemView.findViewById(R.id.tvInviteText);
                btnAccept = itemView.findViewById(R.id.btnAccept);
                btnDecline = itemView.findViewById(R.id.btnDecline);
            }
        }
    }
}