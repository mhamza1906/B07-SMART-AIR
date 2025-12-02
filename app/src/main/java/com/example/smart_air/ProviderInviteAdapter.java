package com.example.smart_air;


import android.graphics.Color;
import android.graphics.Paint;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.*;

public class ProviderInviteAdapter extends RecyclerView.Adapter<ProviderInviteAdapter.Holder> {

    public interface OnLinkClick {
        void onLinkClicked(String linkValue, String childId);
    }

    private final List<ProviderInviteItem> list;
    private final OnLinkClick listener;

    public ProviderInviteAdapter(List<ProviderInviteItem> list, OnLinkClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.provider_invite_item, parent, false);
        return new Holder(v);
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int pos) {
        ProviderInviteItem item = list.get(pos);

        if (item.link != null) {
            h.txtType.setText(R.string.link);
            h.txtValue.setText(item.link);
            h.txtValue.setTextColor(Color.BLUE);
            h.txtValue.setPaintFlags(h.txtValue.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

            h.txtValue.setOnClickListener(v ->
                    listener.onLinkClicked(item.link, item.childId)
            );

        } else {
            h.txtType.setText(R.string.invite_type);
            h.txtValue.setText(item.code);
            h.txtValue.setTextColor(Color.BLACK);
            h.txtValue.setPaintFlags(0);
            h.txtValue.setOnClickListener(null);
        }
    }

    @Override public int getItemCount() { return list.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView txtType, txtValue;

        Holder(View v) {
            super(v);
            txtType = v.findViewById(R.id.txtInviteType);
            txtValue = v.findViewById(R.id.txtInviteValue);
        }
    }
}
