package com.example.smart_air;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriggerSummaryActivity extends AppCompatActivity {

    private BarChart chartTriggers;
    private TextView txtEmpty;
    private View progressBar;

    private FirebaseFirestore db;
    private String childId;
    private CheckBox chkShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trigger_summary);

        db = FirebaseFirestore.getInstance();
        childId = getIntent().getStringExtra("childID");

        if (childId == null || childId.isEmpty()) {
            finish();
            return;
        }

        chartTriggers = findViewById(R.id.chart_triggers);
        txtEmpty = findViewById(R.id.txt_trigger_empty);
        progressBar = findViewById(R.id.trigger_progress_bar);
        chkShare = findViewById(R.id.chk_share_chart);


        loadShareState();

        chkShare.setOnCheckedChangeListener((btn, checked) ->
                updateProviderShareSetting(checked)
        );

        setupChart();
        loadTriggerData();
    }

    private void loadShareState() {
        db.collection("child-provider-share")
                .document(childId)
                .collection("providers")
                .get()
                .addOnSuccessListener(snap -> {

                    boolean shareEnabled = false;

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Boolean val = doc.getBoolean("summary_visibility.triggers");
                        if (val != null && val) {
                            shareEnabled = true;
                            break;
                        }
                    }

                    chkShare.setChecked(shareEnabled);
                });
    }

    private void updateProviderShareSetting(boolean checked) {

        db.collection("child-provider-share")
                .document(childId)
                .collection("providers")
                .get()
                .addOnSuccessListener(snap -> {

                    if (snap.isEmpty()) {
                        Toast.makeText(
                                TriggerSummaryActivity.this,
                                "This toggle will not be saved. Please link to a provider first.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Map<String, Object> update = new HashMap<>();
                        Map<String, Object> visibility = new HashMap<>();

                        visibility.put("triggers", checked);
                        update.put("summary_visibility", visibility);

                        doc.getReference().set(update, SetOptions.merge());
                    }
                });
    }



    private void setupChart() {

        chartTriggers.setNoDataText("No trigger data yet.");
        chartTriggers.getDescription().setEnabled(false);
        chartTriggers.getLegend().setEnabled(false);

        chartTriggers.setXAxisRenderer(
                new MultiLineXAxisRenderer(chartTriggers.getViewPortHandler(),
                        chartTriggers.getXAxis(),
                        chartTriggers));

        XAxis xAxis = chartTriggers.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(14f);
        xAxis.setYOffset(16f);
        xAxis.setTextColor(Color.parseColor("#000000"));

        YAxis leftAxis = chartTriggers.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setTextSize(14f);
        leftAxis.setTextColor(Color.parseColor("#000000"));

        chartTriggers.getAxisRight().setEnabled(false);
        chartTriggers.setExtraOffsets(0f, 32f, 0f, 64f);
    }


    private void loadTriggerData() {
        progressBar.setVisibility(View.VISIBLE);
        txtEmpty.setVisibility(View.GONE);

        db.collection("DailyCheckIns")
                .document(childId)
                .collection("log")
                .get()
                .addOnSuccessListener(this::onTriggerDataLoaded)
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    txtEmpty.setText(getString(R.string.trigger_loading_failed));
                    txtEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void onTriggerDataLoaded(QuerySnapshot snapshot) {
        progressBar.setVisibility(View.GONE);

        if (snapshot == null || snapshot.isEmpty()) {
            txtEmpty.setText(getString(R.string.trigger_na));
            txtEmpty.setVisibility(View.VISIBLE);
            return;
        }

        List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
        docs.sort(Comparator.comparing(DocumentSnapshot::getId));

        Map<String, Integer> triggerCounts = new HashMap<>();

        for (DocumentSnapshot doc : docs) {
            addTriggerCounts(triggerCounts, getStringList(doc, "NWTrigger"));
            addTriggerCounts(triggerCounts, getStringList(doc, "CWTrigger"));
            addTriggerCounts(triggerCounts, getStringList(doc, "ALTrigger"));
        }

        if (triggerCounts.isEmpty()) {
            txtEmpty.setText(getString(R.string.no_triggers));
            txtEmpty.setVisibility(View.VISIBLE);
            return;
        }

        List<String> triggerLabels = new ArrayList<>(triggerCounts.keySet());
        Collections.sort(triggerLabels);

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < triggerLabels.size(); i++) {
            String key = triggerLabels.get(i);
            Integer boxed = triggerCounts.get(key);
            int count = (boxed == null ? 0 : boxed);
            entries.add(new BarEntry(i, (float) count));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Triggers");
        dataSet.setColor(Color.parseColor("#FFB74D"));
        dataSet.setDrawValues(true);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        chartTriggers.setData(barData);

        XAxis xAxis = chartTriggers.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(formatTriggerLabels(triggerLabels)));

        chartTriggers.invalidate();
        txtEmpty.setVisibility(View.GONE);
    }


    private void addTriggerCounts(Map<String, Integer> map, List<String> triggers) {
        if (triggers == null) return;

        for (String t : triggers) {
            if (t == null || t.trim().isEmpty()) continue;

            Integer cur = map.get(t);
            if (cur == null) cur = 0;

            map.put(t, cur + 1);
        }
    }

    private List<String> formatTriggerLabels(List<String> labels) {
        List<String> formatted = new ArrayList<>();
        for (String label : labels) {
            if (label.length() > 10) {
                label = label.replace(" / ", "\n/ ");
                int idx = label.indexOf(" ");
                if (idx != -1) {
                    label = label.substring(0, idx) + "\n" + label.substring(idx + 1);
                }
            }
            formatted.add(label);
        }
        return formatted;
    }

    private List<String> getStringList(DocumentSnapshot doc, String key) {
        Object value = doc.get(key);
        List<String> list = new ArrayList<>();
        if (value instanceof List<?>) {
            for (Object o : (List<?>) value) {
                if (o instanceof String) {
                    list.add((String) o);
                }
            }
        }
        return list;
    }
}
