package com.example.smart_air;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MedlogActivity extends AppCompatActivity {

    private TableLayout tableLayout;
    private FirebaseFirestore db;
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medlog);

        tableLayout = findViewById(R.id.medlog_table);
        db = FirebaseFirestore.getInstance();

        childId = getIntent().getStringExtra("childID");
        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Child ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchMedlogData();
    }

    private void fetchMedlogData() {
        db.collection("medlog").document(childId).collection("log")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<MedLogEntry> medLogEntries = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String date = document.getId();
                            Map<String, Object> data = document.getData();
                            if (data == null) continue;

                            for (Map.Entry<String, Object> typeEntry : data.entrySet()) {
                                String medType = typeEntry.getKey();
                                if (!(typeEntry.getValue() instanceof Map)) continue;
                                Map<String, Object> doses = (Map<String, Object>) typeEntry.getValue();

                                for (Map.Entry<String, Object> doseEntry : doses.entrySet()) {
                                    if (!(doseEntry.getValue() instanceof Map)) continue;
                                    Map<String, Object> doseData = (Map<String, Object>) doseEntry.getValue();

                                    try {
                                        String time = (String) doseData.get("time");
                                        if (time == null) time = "N/A";

                                        Number preRatingNum = (Number) doseData.get("pre_breath_rating");
                                        int preBreathRating = (preRatingNum != null) ? preRatingNum.intValue() : 0;

                                        Number postRatingNum = (Number) doseData.get("post_breath_rating");
                                        int postBreathRating = (postRatingNum != null) ? postRatingNum.intValue() : 0;

                                        Number doseNum = (Number) doseData.get("doseAmount");
                                        int doseAmount = (doseNum != null) ? doseNum.intValue() : 0;

                                        String postCheck = (String) doseData.get("post_checkup");
                                        if (postCheck == null) postCheck = "N/A";

                                        medLogEntries.add(new MedLogEntry(date, time, medType, doseAmount, preBreathRating, postBreathRating, postCheck));
                                    } catch (Exception e) {
                                        Toast.makeText(MedlogActivity.this, "Error parsing a log entry.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                        if (medLogEntries.isEmpty()) {
                            Toast.makeText(MedlogActivity.this, "Medication log is empty.", Toast.LENGTH_SHORT).show();
                        }
                        // Sort entries by date (descending), then time (descending)
                        medLogEntries.sort((e1, e2) -> {
                            int dateCompare = e2.getDate().compareTo(e1.getDate()); // Most recent date first
                            if (dateCompare != 0) return dateCompare;
                            return e2.getTime().compareTo(e1.getTime()); // Most recent time first
                        });
                        populateTable(medLogEntries);
                        Toast.makeText(MedlogActivity.this, "Error getting documents: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void populateTable(List<MedLogEntry> medLogEntries) {
        // Clear existing rows, except the header
        while (tableLayout.getChildCount() > 1) {
            tableLayout.removeView(tableLayout.getChildAt(1));
        }

        for (MedLogEntry entry : medLogEntries) {
            TableRow row = new TableRow(this);

            TextView date = new TextView(this);
            date.setText(entry.getDate());
            date.setPadding(8, 8, 24, 8);
            row.addView(date);

            TextView time = new TextView(this);
            time.setText(entry.getTime());
            time.setPadding(8, 8, 24, 8);
            row.addView(time);

            TextView type = new TextView(this);
            type.setText(entry.getType());
            type.setPadding(8, 8, 24, 8);
            row.addView(type);

            TextView doses = new TextView(this);
            doses.setText(String.valueOf(entry.getNumberOfDoses()));
            doses.setPadding(8, 8, 8, 8);
            doses.setGravity(Gravity.CENTER);
            row.addView(doses);

            TextView preBreath = new TextView(this);
            preBreath.setText(String.valueOf(entry.getPreBreathRating()));
            preBreath.setPadding(8, 8, 8, 8);
            preBreath.setGravity(Gravity.CENTER);
            row.addView(preBreath);

            TextView postBreath = new TextView(this);
            postBreath.setText(String.valueOf(entry.getPostBreathRating()));
            postBreath.setPadding(8, 8, 8, 8);
            postBreath.setGravity(Gravity.CENTER);
            row.addView(postBreath);

            TextView postCheck = new TextView(this);
            postCheck.setText(entry.getPostCheck());
            postCheck.setPadding(8, 8, 8, 8);
            postCheck.setGravity(Gravity.CENTER);
            row.addView(postCheck);

            tableLayout.addView(row);
        }
    }
}
