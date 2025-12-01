package com.example.smart_air;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ConfigurePEFSettingsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String childId;

    private TextView textCurrentPbValue;
    private EditText editTextPbValue;
    private Button btnSavePb;

    private RadioGroup radioGroupGraphRange;
    private RadioButton radio7Days;
    private RadioButton radio30Days;
    private Button btnSaveGraphSetting;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configure_pef_settings);

        childId = getIntent().getStringExtra("childID");
        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Child ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        // Initialize views
        textCurrentPbValue = findViewById(R.id.text_current_pb_value);
        editTextPbValue = findViewById(R.id.edit_text_pb_value);
        btnSavePb = findViewById(R.id.btn_save_pb);

        radioGroupGraphRange = findViewById(R.id.radio_group_graph_range);
        radio7Days = findViewById(R.id.radio_7_days);
        radio30Days = findViewById(R.id.radio_30_days);
        btnSaveGraphSetting = findViewById(R.id.btn_save_graph_setting);

        // Load current settings from Firestore
        loadCurrentSettings();

        // Set listeners
        btnSavePb.setOnClickListener(v -> savePersonalBest());
        btnSaveGraphSetting.setOnClickListener(v -> saveGraphSetting());
    }

    private void loadCurrentSettings() {
        DocumentReference pefDocRef = db.collection("PEF").document(childId);

        pefDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Load Personal Best
                Long pbValue = documentSnapshot.getLong("PB");
                if (pbValue != null) {
                    textCurrentPbValue.setText(String.valueOf(pbValue));
                }

                // Load Graph Range Preference
                Long graphRange = documentSnapshot.getLong("graph_day_range");
                if (graphRange != null) {
                    if (graphRange == 30) {
                        radio30Days.setChecked(true);
                    } else {
                        radio7Days.setChecked(true); // Default to 7 days
                    }
                } else {
                    radio7Days.setChecked(true); // Default if field doesn't exist
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load settings.", Toast.LENGTH_SHORT).show();
        });
    }

    private void savePersonalBest() {
        String pbString = editTextPbValue.getText().toString();
        if (pbString.isEmpty()) {
            Toast.makeText(this, "Please enter a value.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int pbValue = Integer.parseInt(pbString);

            // Add the range check
            if (pbValue < 50 || pbValue > 900) {
                Toast.makeText(this, "Please enter a value within a reasonable range (50-900).", Toast.LENGTH_LONG).show();
                return;
            }

            DocumentReference pefDocRef = db.collection("PEF").document(childId);

            // First, get the document to check if it exists
            pefDocRef.get().addOnSuccessListener(documentSnapshot -> {
                Map<String, Object> pbUpdate = new HashMap<>();
                pbUpdate.put("PB", pbValue);

                // If the document does not exist, also set the default graph range
                if (!documentSnapshot.exists()) {
                    pbUpdate.put("graph_day_range", 7);
                }

                // Now, perform the write operation with the correctly prepared map
                pefDocRef.set(pbUpdate, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Personal Best saved successfully!", Toast.LENGTH_SHORT).show();
                            textCurrentPbValue.setText(String.valueOf(pbValue)); // Update the UI
                            editTextPbValue.setText(""); // Clear the input
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to save Personal Best.", Toast.LENGTH_SHORT).show());

            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Error checking document before save.", Toast.LENGTH_SHORT).show();
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveGraphSetting() {
        int selectedId = radioGroupGraphRange.getCheckedRadioButtonId();
        int durationDays = 7; // Default to 7

        if (selectedId == R.id.radio_30_days) {
            durationDays = 30;
        }

        DocumentReference pefDocRef = db.collection("PEF").document(childId);
        final int finalDurationDays = durationDays;

        // First, get the document to check if it exists
        pefDocRef.get().addOnSuccessListener(documentSnapshot -> {
            Map<String, Object> rangeUpdate = new HashMap<>();
            rangeUpdate.put("graph_day_range", finalDurationDays);

            // If the document does not exist, also set a default PB value
            if (!documentSnapshot.exists()) {
                rangeUpdate.put("PB", 900);
            }

            // Now, perform the write operation
            pefDocRef.set(rangeUpdate, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Graph setting saved successfully!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save graph setting.", Toast.LENGTH_SHORT).show());

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error checking document before save.", Toast.LENGTH_SHORT).show();
        });
    }
}
