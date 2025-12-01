package com.example.smart_air;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;

public class PEFSummaryActivity extends AppCompatActivity {

    private String childId;
    private FirebaseFirestore db;

    private View zoneColorView;
    private LineChart chartPEF;
    private TextView txtRangeLabel;
    private CheckBox chkShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pef_summary);

        childId = getIntent().getStringExtra("childID");
        if (childId == null || childId.isEmpty()) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        zoneColorView = findViewById(R.id.pef_zone_view);
        chartPEF = findViewById(R.id.chart_pef);
        txtRangeLabel = findViewById(R.id.txt_range_label);
        chkShare = findViewById(R.id.chk_share_pef);

        setupChart();
        loadSharePreference();
        loadPEFData();
    }

    private void loadSharePreference() {
        db.collection("summaryCharts").document(childId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean saved = doc.getBoolean("pefSummary");
                        if (saved != null) chkShare.setChecked(saved);
                    }
                });

        chkShare.setOnCheckedChangeListener((btn, isChecked) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("pefSummary", isChecked);
            db.collection("summaryCharts")
                    .document(childId)
                    .set(map, com.google.firebase.firestore.SetOptions.merge());
        });
    }

    private void setupChart() {
        chartPEF.setNoDataText("No PEF data yet.");
        chartPEF.getAxisRight().setEnabled(false);

        Description desc = new Description();
        desc.setText("");
        chartPEF.setDescription(desc);

        chartPEF.getLegend().setEnabled(false);
        chartPEF.getXAxis().setDrawGridLines(false);
        chartPEF.getAxisLeft().setTextColor(Color.BLACK);
        chartPEF.getXAxis().setTextColor(Color.BLACK);
    }

    private List<String> getLastNDays(int n) {
        List<String> dates = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < n; i++) {
            dates.add(sdf.format(cal.getTime()));
            cal.add(Calendar.DATE, -1);
        }
        return dates;
    }

    private void loadPEFData() {

        db.collection("PEF").document(childId)
                .get()
                .addOnSuccessListener(prefDoc -> {

                    int nDays = 7; // default
                    if (prefDoc.exists()) {
                        Long v = prefDoc.getLong("graph_day_range");
                        if (v != null) nDays = v.intValue();
                    }

                    txtRangeLabel.setText(getString(R.string.chart_default_range, nDays));

                    List<String> dates = getLastNDays(nDays);

                    db.collection("PEF")
                            .document(childId)
                            .collection("log")
                            .whereIn(FieldPath.documentId(), dates)
                            .get()
                            .addOnSuccessListener(snap -> {

                                Map<String, Float> dailyData = new HashMap<>();

                                for (DocumentSnapshot doc : snap) {
                                    Double percent = doc.getDouble("percent");
                                    if (percent != null)
                                        dailyData.put(doc.getId(), percent.floatValue());
                                }

                                List<Float> values = new ArrayList<>();
                                List<String> labels = new ArrayList<>();

                                for (String d : dates) {
                                    values.add(dailyData.getOrDefault(d, 0f));
                                    labels.add(d.substring(5)); // MM-dd
                                }

                                drawPEFChart(values, labels);
                                loadZoneColor();
                            });
                });
    }

    private void drawPEFChart(List<Float> values, List<String> labels) {

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            entries.add(new Entry(i, values.get(i)));
        }

        LineDataSet ds = new LineDataSet(entries, "");
        ds.setColor(Color.parseColor("#1976D2"));
        ds.setCircleColor(Color.parseColor("#1976D2"));
        ds.setLineWidth(2f);
        ds.setCircleRadius(4f);
        ds.setDrawValues(false);

        LineData data = new LineData(ds);
        chartPEF.setData(data);
        chartPEF.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        chartPEF.invalidate();
    }

    private void loadZoneColor() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("PEF")
                .document(childId)
                .collection("log")
                .document(today)
                .get()
                .addOnSuccessListener(doc -> {

                    String zone = doc.getString("zone");
                    if (zone == null) {
                        zoneColorView.setBackgroundColor(Color.GRAY);
                        return;
                    }

                    switch (zone.toLowerCase()) {
                        case "green": zoneColorView.setBackgroundColor(Color.parseColor("#4CAF50")); break;
                        case "yellow": zoneColorView.setBackgroundColor(Color.parseColor("#FFEB3B")); break;
                        case "red": zoneColorView.setBackgroundColor(Color.parseColor("#F44336")); break;
                        default: zoneColorView.setBackgroundColor(Color.GRAY);
                    }
                });
    }
}
