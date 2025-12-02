package com.example.smart_air;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PefHistoryAdapter extends RecyclerView.Adapter<PefHistoryAdapter.ViewHolder> {

    private final List<PefHistoryItem> historyItems;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    public PefHistoryAdapter(List<PefHistoryItem> historyItems) {
        this.historyItems = historyItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pef_zone_history_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PefHistoryItem item = historyItems.get(position);

        holder.timestamp.setText(dateFormat.format(item.getTimestamp()));
        holder.percent.setText(String.format("%d%%", item.getPercent()));

        String zone = item.getZone().toLowerCase();
        holder.zoneName.setText(item.getZone());

        switch (zone) {
            case "green":
                holder.zoneColor.setBackgroundColor(Color.parseColor("#4CAF50"));
                break;
            case "yellow":
                holder.zoneColor.setBackgroundColor(Color.parseColor("#FFEB3B"));
                break;
            case "red":
                holder.zoneColor.setBackgroundColor(Color.parseColor("#F44336"));
                break;
            default:
                holder.zoneColor.setBackgroundColor(Color.GRAY);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timestamp;
        TextView percent;
        View zoneColor;
        TextView zoneName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timestamp = itemView.findViewById(R.id.pef_history_timestamp);
            percent = itemView.findViewById(R.id.pef_history_percent);
            zoneColor = itemView.findViewById(R.id.pef_history_zone_color);
            zoneName = itemView.findViewById(R.id.pef_history_zone_name);
        }
    }
}
