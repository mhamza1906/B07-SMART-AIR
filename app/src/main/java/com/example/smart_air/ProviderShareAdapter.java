package com.example.smart_air;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProviderShareAdapter extends RecyclerView.Adapter<ProviderShareAdapter.ViewHolder> {

    private final List<ProviderShareItem> providerList;
    private final OnRevokeClickListener revokeListener;

    public interface OnRevokeClickListener {
        void onRevoke(String providerUsername);
    }

    public ProviderShareAdapter(List<ProviderShareItem> list, OnRevokeClickListener listener) {
        this.providerList = list;
        this.revokeListener = listener;
    }

    @NonNull
    @Override
    public ProviderShareAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider_share, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProviderShareItem item = providerList.get(position);

        holder.txtProviderName.setText(item.username);

        holder.btnRevoke.setOnClickListener(v ->
                revokeListener.onRevoke(item.username)
        );
    }


    @Override
    public int getItemCount() {
        return providerList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtProviderName;
        Button btnRevoke;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtProviderName = itemView.findViewById(R.id.txtProviderName);
            btnRevoke = itemView.findViewById(R.id.btnRevoke);
        }
    }


}
