package com.example.smart_air;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;
import android.view.View;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SymptomSummaryActivity extends AppCompatActivity {

    private LineChart chartNightWaking;
    private LineChart chartCoughWheeze;
    private LineChart chartActivityLimit;

    private TextView txtEmpty;
    private View progressBar;

    private FirebaseFirestore db;
    private String childId;
    private CheckBox chkShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_summary);

        db = FirebaseFirestore.getInstance();
        childId = getIntent().getStringExtra("childID");

        if (childId == null || childId.isEmpty()) {
            finish();
            return;
        }

        chartNightWaking = findViewById(R.id.chart_night_waking);
        chartCoughWheeze = findViewById(R.id.chart_cough_wheeze);
        chartActivityLimit = findViewById(R.id.chart_activity_limit);

        txtEmpty = findViewById(R.id.txt_symptom_empty);
        progressBar = findViewById(R.id.symptom_progress_bar);

        chkShare = findViewById(R.id.chk_share_chart);

        loadShareState();

        chkShare.setOnCheckedChangeListener((btn, checked) ->
                updateProviderShareSetting(checked)
        );

        setupChart(chartNightWaking);
        setupChart(chartCoughWheeze);
        setupChart(chartActivityLimit);

        loadSymptomData();
    }

    private void loadShareState() {
        db.collection("child-provider-share")
                .document(childId)
                .collection("providers")
                .get()
                .addOnSuccessListener(snap -> {

                    boolean shareEnabled = false;

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Boolean v = doc.getBoolean("summary_visibility.symptoms");
                        if (v != null && v) {
                            shareEnabled = true;
                            break;
                        }
                    }

                    chkShare.setChecked(shareEnabled);
                });
    }

    private void updateProviderShareSetting(boolean value) {

        db.collection("child-provider-share")
                .document(childId)
                .collection("providers")
                .get()
                .addOnSuccessListener(snap -> {

                    for (DocumentSnapshot doc : snap.getDocuments()) {

                        Map<String, Object> update = new HashMap<>();
                        Map<String, Object> vis = new HashMap<>();
                        vis.put("symptoms", value);
                        update.put("summary_visibility", vis);

                        doc.getReference().set(update, SetOptions.merge());
                    }
                });
    }

    private void setupChart(LineChart chart) {

        chart.setNoDataText("No symptom data yet.");
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(14f);
        xAxis.setTextColor(Color.parseColor("#000000"));

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum((float) 1.0);
        leftAxis.setGranularity(1f);
        leftAxis.setTextSize(14f);
        leftAxis.setTextColor(Color.parseColor("#000000"));

        chart.getAxisRight().setEnabled(false);

        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);

        chart.setExtraOffsets(0, 32, 0, 16);
    }

    private void loadSymptomData() {
        progressBar.setVisibility(View.VISIBLE);
        txtEmpty.setVisibility(View.GONE);

        db.collection("DailyCheckIns")
                .document(childId)
                .collection("log")
                .get()
                .addOnSuccessListener(this::onSymptomDataLoaded)
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    txtEmpty.setText(getString(R.string.symptom_loading_failed));
                    txtEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void onSymptomDataLoaded(QuerySnapshot snapshot) {
        progressBar.setVisibility(View.GONE);

        if (snapshot == null || snapshot.isEmpty()) {
            txtEmpty.setText(getString(R.string.symptom_na));
            txtEmpty.setVisibility(View.VISIBLE);
            return;
        }

        List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
        docs.sort(Comparator.comparing(DocumentSnapshot::getId));

        List<String> dateLabels = new ArrayList<>();
        List<Entry> nightEntries = new ArrayList<>();
        List<Entry> coughEntries = new ArrayList<>();
        List<Entry> activityEntries = new ArrayList<>();

        int index = 0;
        for (DocumentSnapshot doc : docs) {
            String dateId = doc.getId();
            dateLabels.add(formatDateLabel(dateId));

            float nightVal = mapNightWakingValue(doc.getString("NightWaking"));
            float coughVal = mapCoughWheezeValue(doc.getString("CoughWheeze"));
            float activityVal = mapActivityLimitValue(doc.getString("ActivityLimit"));

            nightEntries.add(new Entry(index, nightVal));
            coughEntries.add(new Entry(index, coughVal));
            activityEntries.add(new Entry(index, activityVal));

            index++;
        }

        if (index == 0) {
            txtEmpty.setText(getString(R.string.no_symptoms));
            txtEmpty.setVisibility(View.VISIBLE);
            return;
        }

        applyLineData(chartNightWaking, nightEntries, getString(R.string.night_waking_label), dateLabels);
        applyLineData(chartCoughWheeze, coughEntries, getString(R.string.cough_wheeze_label), dateLabels);
        applyLineData(chartActivityLimit, activityEntries, getString(R.string.activity_limit_label), dateLabels);

        txtEmpty.setVisibility(View.GONE);
    }


    private void applyLineData(LineChart chart,
                               List<Entry> entries,
                               String label,
                               List<String> xLabels) {

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setColor(Color.parseColor("#FF6978"));
        dataSet.setCircleColor(Color.parseColor("#FF6978"));

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));

        chart.invalidate();
    }


    private String formatDateLabel(String raw) {
        try {
            SimpleDateFormat src = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat dst = new SimpleDateFormat("MM-dd", Locale.getDefault());
            Date parsed = src.parse(raw);
            if (parsed != null) return dst.format(parsed);
        } catch (Exception ignore) {}
        return raw;
    }

    private float mapNightWakingValue(String s) {
        if (s == null) return 0f;
        if (s.equals(getString(R.string.yes))) return 1f;
        return 0f;
    }

    private float mapCoughWheezeValue(String s) {
        if (s == null) return 0f;
        if (s.equals(getString(R.string.yes))) return 1f;
        return 0f;
    }

    private float mapActivityLimitValue(String s) {
        if (s == null) return 0f;
        if (s.equals(getString(R.string.yes))) return 1f;
        return 0f;
    }
}
