package com.example.smart_air;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ROW = 1;

    private final List<String> dates;
    private final List<String> controllers;
    private final List<String> takenList;

    public ScheduleAdapter(List<String> dates, List<String> controllers, List<String> takenList) {
        this.dates = dates;
        this.controllers = controllers;
        this.takenList = takenList;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_ROW;
    }

    @Override
    public int getItemCount() {
        return dates.size() + 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_schedule_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_schedule_row, parent, false);
            return new RowViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof RowViewHolder) {

            int index = position - 1;

            RowViewHolder rowHolder = (RowViewHolder) holder;

            rowHolder.date.setText(dates.get(index));
            rowHolder.controllers.setText(controllers.get(index));
            rowHolder.taken.setText(takenList.get(index));
        }

    }


    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView headerDate, headerControllers, headerTaken;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);

            headerDate = itemView.findViewById(R.id.headerDate);
            headerControllers = itemView.findViewById(R.id.headerControllers);
            headerTaken = itemView.findViewById(R.id.headerTaken);
        }
    }

    static class RowViewHolder extends RecyclerView.ViewHolder {

        TextView date, controllers, taken;

        public RowViewHolder(@NonNull View itemView) {
            super(itemView);

            date = itemView.findViewById(R.id.rowDate);
            controllers = itemView.findViewById(R.id.rowControllers);
            taken = itemView.findViewById(R.id.rowTaken);
        }
    }
}
