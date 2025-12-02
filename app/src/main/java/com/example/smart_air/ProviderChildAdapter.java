package com.example.smart_air;

import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.*;

public class ProviderChildAdapter extends RecyclerView.Adapter<ProviderChildAdapter.Holder> {

    private final List<ProviderChildItem> list;
    private final OnChildClickListener listener;

    public interface OnChildClickListener {
        void onChildClicked(ProviderChildItem item);
    }

    public ProviderChildAdapter(List<ProviderChildItem> list, OnChildClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.provider_child_item, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        ProviderChildItem item = list.get(pos);
        h.txtName.setText(item.childName);
        h.itemView.setOnClickListener(v -> listener.onChildClicked(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView txtName;
        final TextView txtArrow;

        Holder(View v) {
            super(v);
            txtName = v.findViewById(R.id.txtChildName);
            txtArrow = v.findViewById(R.id.txtArrow);
        }
    }
}
