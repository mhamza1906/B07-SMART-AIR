package com.example.smart_air;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

// Adapter class example for RecyclerView
public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {
    private List<String> dates;
    private List<String> controlNum;
    private List<String> taken;

    public ScheduleAdapter(List<String> dates,List<String> controlNum,List<String> taken) {
        this.dates = dates;
        this.controlNum = controlNum;
        this.taken = taken;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.schedule_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String date = dates.get(position);
        String numControl = controlNum.get(position);
        String checkTaken = taken.get(position);
        holder.dateView.setText(date);
        holder.controllerNumView.setText(numControl);
        holder.takenView.setText(checkTaken);
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateView;
        TextView controllerNumView;
        TextView takenView;

        public ViewHolder(View itemView) {
            super(itemView);
            dateView = itemView.findViewById(R.id.txtDate);
            controllerNumView = itemView.findViewById(R.id.txtControlNum);
            takenView = itemView.findViewById(R.id.txtTaken);
        }
    }
}