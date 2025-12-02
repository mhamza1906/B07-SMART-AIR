package com.example.smart_air;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class TriageIncidentSummaryActivity extends AppCompatActivity {

    private ScatterChart chart;
    private FirebaseFirestore db;
    private String childId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triage_timeline);

        chart = findViewById(R.id.triageScatterChart);
        CheckBox chkShareTriage = findViewById(R.id.chkShareTriage);

        db = FirebaseFirestore.getInstance();
        childId = getIntent().getStringExtra("childID");

        setupChart();
        loadTriageEvents();


        loadTriageShareSetting(chkShareTriage);


        chkShareTriage.setOnCheckedChangeListener((buttonView, checked) ->
                updateProviderShareSetting(checked)
        );

        boolean providerMode = getIntent().getBooleanExtra("providerMode", false);
        if (providerMode) {
            chkShareTriage.setVisibility(View.GONE);
        }
    }

    private void loadTriageShareSetting(CheckBox chk) {
        db.collection("child-provider-share")
                .document(childId)
                .collection("providers")
                .get()
                .addOnSuccessListener(snap -> {

                    boolean triageShared = false;

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Boolean val = doc.getBoolean("summary_visibility.triage");
                        if (val != null && val) {
                            triageShared = true;
                            break;
                        }
                    }

                    chk.setChecked(triageShared);
                });
    }


    private void setupChart() {

        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);


        chart.setExtraOffsets(0f, 10f, 20f, 20f);
        chart.setMinOffset(0f);
        chart.setClipValuesToContent(false);
        chart.setClipToPadding(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(12f);
        xAxis.setLabelRotationAngle(45);
        xAxis.setAvoidFirstLastClipping(true);

        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf =
                    new SimpleDateFormat("MM-dd HH:mm", Locale.US);

            @Override
            public String getFormattedValue(float value) {
                return sdf.format(new Date((long) value));
            }
        });

        YAxis left = chart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setAxisMaximum(5f);
        left.setGranularity(1f);

        left.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                switch ((int) value) {
                    case 1: return "Green";
                    case 2: return "Yellow";
                    case 3: return "Red";
                    case 4: return "Emergency";
                }
                return "";
            }
        });

        chart.getAxisRight().setEnabled(false);
    }



    private void loadTriageEvents() {

        db.collection("triage_incidents").document(childId)
                .collection("incident_log")
                .get()
                .addOnSuccessListener(snap -> {

                    List<DocumentSnapshot> docs = new ArrayList<>(snap.getDocuments());
                    docs.sort(Comparator.comparing(d -> d.getDate("timestamp")));

                    List<IScatterDataSet> dataSets = new ArrayList<>();

                    for (DocumentSnapshot doc : docs) {

                        Date ts = doc.getDate("timestamp");
                        if (ts == null) continue;

                        long x = ts.getTime();
                        String zone = doc.getString("pefZone");
                        Boolean emergency = doc.getBoolean("emergencyServicesCalled");

                        int y = zoneToY(zone, emergency);

                        boolean redFlag =
                                getBool(doc, "redFlagsPresent.unableToTalk") ||
                                        getBool(doc, "redFlagsPresent.hardToBreathe") ||
                                        getBool(doc, "redFlagsPresent.lipColorChange");

                        Entry e = new Entry(x, y);

                        ScatterDataSet ds = new ScatterDataSet(
                                java.util.Collections.singletonList(e),
                                ""
                        );

                        ds.setDrawValues(false);
                        ds.setScatterShape(ScatterChart.ScatterShape.CIRCLE);

                        ds.setScatterShapeSize(redFlag ? 65f : 45f);

                        if (Boolean.TRUE.equals(emergency)) {
                            ds.setColor(Color.parseColor("#8E0000"));
                        } else {
                            ds.setColor(getColorForZone(zone));
                        }

                        dataSets.add(ds);
                    }

                    ScatterData data = new ScatterData(dataSets);
                    chart.setData(data);
                    chart.invalidate();
                });
    }


    private int getColorForZone(String zone) {
        if (Objects.equals(zone, "Not yet calculated")) return Color.BLACK;

        switch (zone) {
            case "Green":
                return Color.parseColor("#2E7D32");  // dark green
            case "Yellow":
                return Color.parseColor("#F9A825");  // dark yellow
            case "Red":
                return Color.parseColor("#C62828");  // strong red
        }
        return Color.BLACK;
    }


    private boolean getBool(DocumentSnapshot doc, String path) {
        Boolean b = doc.getBoolean(path);
        return b != null && b;
    }

    private int zoneToY(String zone, Boolean emergency) {

        if (emergency != null && emergency) return 4;

        if (zone == null) return 0;

        switch (zone) {
            case "Green": return 1;
            case "Yellow": return 2;
            case "Red": return 3;
        }

        return 0;
    }


    private void updateProviderShareSetting(boolean share) {

        db.collection("child-provider-share")
                .document(childId)
                .collection("providers")
                .get()
                .addOnSuccessListener(snap -> {

                    if (snap.isEmpty()) {
                        Toast.makeText(
                                TriageIncidentSummaryActivity.this,
                                "This toggle will not be saved. Please link to a provider first.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    for (DocumentSnapshot doc : snap.getDocuments()) {

                        Map<String, Object> update = new HashMap<>();
                        Map<String, Object> visibility = new HashMap<>();

                        visibility.put("triage", share);
                        update.put("summary_visibility", visibility);

                        doc.getReference().set(update, SetOptions.merge());
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("ShareUpdate", "Failed updating triage share setting", e)
                );
    }


}
