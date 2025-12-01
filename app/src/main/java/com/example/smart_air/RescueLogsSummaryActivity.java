package com.example.smart_air;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.*;

public class RescueLogsSummaryActivity extends AppCompatActivity {

    private LineChart chart7, chart30;
    private TextView txtLastRescue;
    private CheckBox chkShare;

    private FirebaseFirestore db;
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescue_logs_summary);

        db = FirebaseFirestore.getInstance();
        childId = getIntent().getStringExtra("childID");

        if (childId == null || childId.isEmpty()) {
            finish();
            return;
        }

        txtLastRescue = findViewById(R.id.txt_last_rescue);
        chkShare = findViewById(R.id.chk_share_rescue);
        chart7 = findViewById(R.id.chart_rescue_7);
        chart30 = findViewById(R.id.chart_rescue_30);

        setupChart(chart7);
        setupChart(chart30);

        loadSharePreference();
        loadMedlogData();
        loadRescueLogs();
    }

    private void loadSharePreference() {
        db.collection("summaryCharts").document(childId)
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean saved = doc.getBoolean("rescueSummary");
                        if (saved != null)
                            chkShare.setChecked(saved);
                    }
                });

        chkShare.setOnCheckedChangeListener((btn, isChecked) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("rescueSummary", isChecked);
            db.collection("summaryCharts").document(childId)
                    .set(map, SetOptions.merge());
        });
    }

    private void setupChart(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("No rescue log data yet.");

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        xAxis.setDrawGridLines(false);

        YAxis yLeft = chart.getAxisLeft();
        yLeft.setAxisMinimum(0f);
        yLeft.setTextSize(12f);

        chart.getAxisRight().setEnabled(false);
        chart.setExtraBottomOffset(32f);
    }

    private void loadMedlogData() {
        db.collection("medlog").document(childId)
                .get().addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        String lastUse = snap.getString("last_rescue_use");
                        String display = (lastUse == null ? "-" : lastUse);
                        txtLastRescue.setText(getString(R.string.last_rescue_use_format, display));
                    }
                });
    }

    private void loadRescueLogs() {
        db.collection("medlog").document(childId)
                .collection("log")
                .get()
                .addOnSuccessListener(this::onRescueLogLoaded);
    }

    private void onRescueLogLoaded(QuerySnapshot snap) {

        if (snap == null || snap.isEmpty()) return;

        Map<String, Integer> dailyCount = new HashMap<>();

        for (DocumentSnapshot doc : snap.getDocuments()) {

            Object raw = doc.get("rescue");
            Map<String, Object> rescueMap = new HashMap<>();

            if (raw instanceof Map<?, ?>) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) raw).entrySet()) {
                    if (e.getKey() instanceof String) {
                        rescueMap.put((String) e.getKey(), e.getValue());
                    }
                }
            }

            int size = rescueMap.size();
            dailyCount.put(doc.getId(), size);
        }

        draw7DayChart(dailyCount);
        draw30DayChart(dailyCount);
    }


    private void draw7DayChart(Map<String, Integer> map) {
        List<String> last7 = getPastDates(7);
        drawLineChart(chart7, last7, map, true);
    }

    private void draw30DayChart(Map<String, Integer> map) {
        List<String> last30 = getPastDates(30);
        drawLineChart(chart30, last30, map, false);
    }

    private void drawLineChart(LineChart chart,
                               List<String> dateList,
                               Map<String, Integer> valueMap,
                               boolean is7days) {

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < dateList.size(); i++) {
            String day = dateList.get(i);
            Integer boxed = valueMap.get(day);
            int count = (boxed == null ? 0 : boxed);


            entries.add(new Entry(i, count));

            if (is7days) {
                labels.add((i % 2 == 0) ? day.substring(5) : "");
            } else {
                if (i == 0 || i == 9 || i == 19 || i == 29)
                    labels.add(day.substring(5));
                else
                    labels.add("");
            }
        }

        LineDataSet ds = new LineDataSet(entries, "");
        ds.setColor(Color.parseColor("#FF7043"));
        ds.setCircleColor(Color.parseColor("#FF7043"));
        ds.setCircleRadius(4f);
        ds.setLineWidth(2f);
        ds.setDrawValues(false);

        LineData data = new LineData(ds);
        chart.setData(data);

        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.invalidate();
    }

    private List<String> getPastDates(int days) {
        List<String> list = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        for (int i = days - 1; i >= 0; i--) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.DAY_OF_YEAR, -i);
            list.add(sdf.format(c.getTime()));
        }
        return list;
    }
}
