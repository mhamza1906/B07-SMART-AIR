package com.example.smart_air;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class ChildSummaryAdapter extends RecyclerView.Adapter<ChildSummaryAdapter.ViewHolder> {
    private final List<ChildSummary> childSummaries;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView child_name_text;
        public View todays_zone_view;
        public TextView dob_value;
        public LineChart trend_graph_view;
        public TextView last_rescue_value;
        public TextView weekly_rescue_value;



        public ViewHolder(View itemView) {
            super(itemView);
            child_name_text = itemView.findViewById(R.id.child_name_text);
            todays_zone_view = itemView.findViewById(R.id.todays_zone_view);
            dob_value = itemView.findViewById(R.id.dob_value);
            trend_graph_view = itemView.findViewById(R.id.trend_graph_view);
            last_rescue_value = itemView.findViewById(R.id.last_rescue_value);
            weekly_rescue_value = itemView.findViewById(R.id.weekly_rescue_value);
        }
    }

    public ChildSummaryAdapter(List<ChildSummary> list) {
        this.childSummaries = list;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View childSummaryView = inflater.inflate(R.layout.parent_child_summary_item, parent, false);

        // Return a new holder instance
        return new ViewHolder(childSummaryView);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ChildSummary currentChild = childSummaries.get(position);

        holder.child_name_text.setText(currentChild.getChildName());
        holder.dob_value.setText(currentChild.getDob());


        try {
            holder.todays_zone_view.setBackgroundColor(Color.parseColor(currentChild.getTodayZoneColor()));
        } catch (IllegalArgumentException e) {
            holder.todays_zone_view.setBackgroundColor(Color.GRAY);
        }

        holder.last_rescue_value.setText(currentChild.getLastRescueTime());
        holder.weekly_rescue_value.setText(String.valueOf(currentChild.getWeeklyRescueCount()));


        setupGraph(holder.trend_graph_view, currentChild.getGraphData());
    }


    private void setupGraph(LineChart chart, List<Float> graphData) {
        if (graphData == null || graphData.isEmpty()) {
            chart.clear();
            chart.invalidate();
            return;
        }

        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < graphData.size(); i++) {
            entries.add(new Entry(i, graphData.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily PEF % Trend");

        // Style the line
        dataSet.setColor(ContextCompat.getColor(chart.getContext(), R.color.chart_line_color));
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(true);
        dataSet.setDrawCircles(true);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(chart.getContext(), R.color.chart_line_color));
        dataSet.setFillAlpha(100);

        // Style the chart grid
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate(); // Refresh the chart
    }



    @Override
    public int getItemCount() {
        return childSummaries.size();
    }
}
