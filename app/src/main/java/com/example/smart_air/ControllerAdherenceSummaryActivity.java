package com.example.smart_air;

import android.graphics.Color;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.CollectionReference;
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

public class ControllerAdherenceSummaryActivity extends AppCompatActivity {

    private LineChart chart;
    private ProgressBar progress;
    private TextView txtEmpty;
    private CheckBox chkShare;

    private ImageView imgStreak;
    private TextView txtStreak;
    private Animation flameAnim;

    private FirebaseFirestore db;
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller_adherence_summary);

        childId = getIntent().getStringExtra("childID");
        if (childId == null) { finish(); return; }

        db = FirebaseFirestore.getInstance();

        chart = findViewById(R.id.chart_adherence);
        progress = findViewById(R.id.progress_adherence);
        txtEmpty = findViewById(R.id.txt_empty_adherence);
        chkShare = findViewById(R.id.chk_share_adherence);

        imgStreak = findViewById(R.id.imgControllerStreak);
        txtStreak = findViewById(R.id.txtControllerStreak);

        flameAnim = AnimationUtils.loadAnimation(this, R.anim.flame_pulse);

        loadShareSetting();
        setupShareToggle();

        setupChartUI();
        loadControllerStreak();
        loadAdherenceData();
    }

    private void loadControllerStreak() {
        db.collection("controllerStreak").document(childId)
                .get()
                .addOnSuccessListener(snap -> {

                    long days = 0;
                    if (snap.exists()) {
                        Long v = snap.getLong("consecutive_days");
                        if (v != null) days = v;
                    }

                    if (days <= 0) {
                        imgStreak.clearAnimation();
                        imgStreak.setImageResource(R.drawable.empty_streak);
                        txtStreak.setText(getString(R.string.controller_streak_interrupted_parent));
                    } else {
                        imgStreak.setImageResource(R.drawable.controller_streak);
                        imgStreak.startAnimation(flameAnim);
                        txtStreak.setText(getString(R.string.controller_streak_success_parent, days));
                    }

                })
                .addOnFailureListener(e -> {
                    imgStreak.clearAnimation();
                    imgStreak.setImageResource(R.drawable.empty_streak);
                    txtStreak.setText(getString(R.string.controller_streak_unable_to_load));
                });
    }

    private void loadShareSetting() {

        db.collection("child-provider-share")
                .document(childId)
                .collection("providers")
                .get()
                .addOnSuccessListener(snap -> {

                    boolean enabled = false;

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Boolean v = doc.getBoolean("summary_visibility.controller");
                        if (v != null && v) {
                            enabled = true;
                            break;
                        }
                    }

                    chkShare.setChecked(enabled);
                });
    }

    private void setupShareToggle() {
        chkShare.setOnCheckedChangeListener((btn, checked) ->
                updateProviderShareSetting(checked)
        );
    }

    private void updateProviderShareSetting(boolean value) {

        db.collection("child-provider-share")
                .document(childId)
                .collection("providers")
                .get()
                .addOnSuccessListener(snap -> {

                    if (snap.isEmpty()) {
                        Toast.makeText(
                                ControllerAdherenceSummaryActivity.this,
                                "This toggle will not be saved. Please link to a provider first.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    for (DocumentSnapshot doc : snap.getDocuments()) {

                        Map<String, Object> update = new HashMap<>();
                        Map<String, Object> visField = new HashMap<>();
                        visField.put("controller", value);

                        update.put("summary_visibility", visField);

                        doc.getReference().set(update, SetOptions.merge());
                    }
                });
    }

    private void loadAdherenceData() {
        progress.setVisibility(View.VISIBLE);

        CollectionReference col =
                db.collection("planned-schedule").document(childId).collection("schedules");

        col.get().addOnSuccessListener(this::onAdherenceLoaded)
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    txtEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void onAdherenceLoaded(QuerySnapshot snap) {
        progress.setVisibility(View.GONE);

        if (snap == null || snap.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            return;
        }

        List<DocumentSnapshot> docs = new ArrayList<>(snap.getDocuments());
        docs.sort(Comparator.comparing(DocumentSnapshot::getId));

        List<String> dates = new ArrayList<>();
        List<Float> percents = new ArrayList<>();

        for (DocumentSnapshot d : docs) {
            String date = d.getId();
            Long goal = d.getLong("controllerNumber");
            Long done = d.getLong("completedNumber");

            if (goal == null || goal == 0) continue;

            float p = (done == null ? 0 : (done * 100f) / goal);

            dates.add(date);
            percents.add(p);
        }

        if (dates.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            return;
        }

        drawChart(dates, percents);
    }

    private void setupChartUI() {
        chart.setNoDataText("No adherence data.");
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);
        x.setDrawGridLines(false);

        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setAxisMaximum(100f);
        chart.getAxisRight().setEnabled(false);
    }

    private void drawChart(List<String> dates, List<Float> percents) {

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < percents.size(); i++) {
            entries.add(new Entry(i, percents.get(i)));
        }

        LineDataSet ds = new LineDataSet(entries, "");
        ds.setColor(Color.parseColor("#42A5F5"));
        ds.setCircleColor(Color.parseColor("#1E88E5"));
        ds.setCircleRadius(4f);
        ds.setLineWidth(2f);
        ds.setDrawValues(false);

        chart.setData(new LineData(ds));

        /* reduced x-axis ticks */
        List<String> labels = new ArrayList<>(Collections.nCopies(dates.size(), ""));
        int n = dates.size();

        int idxMin = 0;
        int idx25 = n / 4;
        int idx50 = n / 2;
        int idx75 = (3 * n) / 4;
        int idxMax = n - 1;

        labels.set(idxMin, dates.get(idxMin).substring(5));
        labels.set(idx25, dates.get(idx25).substring(5));
        labels.set(idx50, dates.get(idx50).substring(5));
        labels.set(idx75, dates.get(idx75).substring(5));
        labels.set(idxMax, dates.get(idxMax).substring(5));

        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.invalidate();
    }
}
