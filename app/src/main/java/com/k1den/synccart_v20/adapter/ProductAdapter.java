package com.k1den.synccart_v20.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.k1den.synccart_v20.R;
import com.k1den.synccart_v20.models.ListItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<ListItem> items = new ArrayList<>();
    private OnItemToggleListener listener;

    public interface OnItemToggleListener {
        void onToggle(ListItem item, int position);
    }

    public void setOnItemToggleListener(OnItemToggleListener listener) {
        this.listener = listener;
    }

    public interface OnItemDeleteListener {
        void onDelete(ListItem item, int position);
    }

    // В начало класса добавь:
    public interface OnAssignClickListener {
        void onAssignClick(ListItem item, int position);
    }

    private OnAssignClickListener assignListener;

    public void setOnAssignClickListener(OnAssignClickListener listener) {
        this.assignListener = listener;
    }

    private OnItemDeleteListener deleteListener;

    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.deleteListener = listener;
    }

    // --- МАГИЯ СОРТИРОВКИ ЗДЕСЬ ---
    private void sortItemsByCategory() {
        Collections.sort(this.items, (a, b) -> {
            String catA = a.getCategory() != null ? a.getCategory() : "Разное";
            String catB = b.getCategory() != null ? b.getCategory() : "Разное";
            return catA.compareTo(catB);
        });
    }

    public void setItems(List<ListItem> items) {
        this.items = items;
        sortItemsByCategory(); // Сортируем при загрузке
        notifyDataSetChanged();
    }

    public void addItem(ListItem item) {
        this.items.add(item);
        sortItemsByCategory(); // Сортируем при добавлении
        notifyDataSetChanged();
    }
    // -----------------------------

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ListItem item = items.get(position);

        // 1. ЛОГИКА ОТОБРАЖЕНИЯ ЗАГОЛОВКА КАТЕГОРИИ
        String currentCat = item.getCategory() != null ? item.getCategory() : "Разное";
        boolean showHeader = false;

        if (position == 0) {
            showHeader = true; // Для первого элемента всегда показываем
        } else {
            String prevCat = items.get(position - 1).getCategory() != null ? items.get(position - 1).getCategory() : "Разное";
            if (!currentCat.equals(prevCat)) {
                showHeader = true; // Показываем, если категория изменилась
            }
        }

        if (showHeader) {
            holder.tvCategoryHeader.setVisibility(View.VISIBLE);
            holder.tvCategoryHeader.setText(currentCat);
        } else {
            holder.tvCategoryHeader.setVisibility(View.GONE);
        }

        // 2. ЛОГИКА ПРОДУКТА
        holder.cbIsBought.setOnCheckedChangeListener(null);
        holder.tvProductName.setText(item.getName());
        holder.cbIsBought.setChecked(item.isBought());

        if (item.isBought()) {
            holder.tvProductName.setPaintFlags(holder.tvProductName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvProductName.setTextColor(0xFF888888);
        } else {
            holder.tvProductName.setPaintFlags(holder.tvProductName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvProductName.setTextColor(0xFF000000);
        }

        holder.cbIsBought.setOnCheckedChangeListener((btn, isChecked) -> {
            if (listener != null) listener.onToggle(item, position);
        });

        holder.btnDeleteProduct.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(item, position);
            }
        });

        // Отображение ответственного
        if (item.getAssigneeName() != null && !item.getAssigneeName().isEmpty()) {
            holder.tvAssignedUser.setText("Купит: " + item.getAssigneeName());
            holder.tvAssignedUser.setTextColor(0xFF6A9A6E); // Зеленый цвет
        } else {
            holder.tvAssignedUser.setText("Всем");
            holder.tvAssignedUser.setTextColor(0xFF888888); // Серый цвет
        }

        // Клик по тексту "Всем" / "Купит: Имя"
        holder.tvAssignedUser.setOnClickListener(v -> {
            if (assignListener != null) assignListener.onAssignClick(item, position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryHeader; // <-- ДОБАВИЛИ ЗАГОЛОВОК
        CheckBox cbIsBought;
        TextView tvProductName, tvAssignedUser;

        android.widget.ImageButton btnDeleteProduct;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryHeader = itemView.findViewById(R.id.tvCategoryHeader);
            cbIsBought = itemView.findViewById(R.id.cbIsBought);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvAssignedUser = itemView.findViewById(R.id.tvAssignedUser);
            btnDeleteProduct = itemView.findViewById(R.id.btnDeleteProduct);
        }
    }
}