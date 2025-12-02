package com.example.smart_air;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DailyCheckinHistoryAdapter extends RecyclerView.Adapter<DailyCheckinHistoryAdapter.ViewHolder> {

    private final List<DailyCheckinHistoryItem> historyItems;

    public DailyCheckinHistoryAdapter(List<DailyCheckinHistoryItem> historyItems) {
        this.historyItems = historyItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.daily_checkin_history_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyCheckinHistoryItem item = historyItems.get(position);

        holder.date.setText(item.getDate());
        holder.author.setText("Author: " + item.getAuthor());

        // Handle Symptoms
        if (item.getSymptoms() != null && !item.getSymptoms().isEmpty()) {
            String symptomsText = "Symptoms: " + String.join(", ", item.getSymptoms());
            holder.symptoms.setText(symptomsText);
            holder.symptoms.setVisibility(View.VISIBLE);
        } else {
            holder.symptoms.setText("Symptoms: None");
        }

        // Handle Triggers
        if (item.getTriggers() != null && !item.getTriggers().isEmpty()) {
            String triggersText = "Triggers: " + String.join(", ", item.getTriggers());
            holder.triggers.setText(triggersText);
            holder.triggers.setVisibility(View.VISIBLE);
        } else {
            holder.triggers.setText("Triggers: None");
        }
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView date;
        TextView author;
        TextView symptoms;
        TextView triggers;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.checkin_history_date);
            author = itemView.findViewById(R.id.checkin_history_author);
            symptoms = itemView.findViewById(R.id.checkin_history_symptoms);
            triggers = itemView.findViewById(R.id.checkin_history_triggers);
        }
    }
}
