package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup; // Import RadioGroup
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TakeMedicineActivity extends AppCompatActivity {

    private String medType;
    private String childId;
    private int preBreathRating;
    private FirebaseFirestore db;

    private RadioGroup medTypeButton;
    private EditText preBreathRatingInput;
    private String date;
    private Button continueButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_medicine);
        childId = getIntent().getStringExtra("childID");
        if(childId == null || childId.isEmpty()) {
            Toast.makeText(this,"User ID not found",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        db = FirebaseFirestore.getInstance();
        medTypeButton = findViewById(R.id.medicine_type);
        preBreathRatingInput = findViewById(R.id.post_breath_rating_input);
        continueButton = (Button)findViewById(R.id.continue_button);

        medTypeButton.setOnCheckedChangeListener((group, checkedId)->{
            RadioButton selectedButton = findViewById(checkedId);
            medType = selectedButton.getText().toString();
        });


        continueButton.setOnClickListener(v -> {
            Runnable navigateNext = () -> {
                Intent switchTechniqueHelper = new Intent(TakeMedicineActivity.this, techniqueHelper.class);

                switchTechniqueHelper.putExtra("date", date);
                switchTechniqueHelper.putExtra("child_id", childId);
                switchTechniqueHelper.putExtra("type", medType);

                startActivity(switchTechniqueHelper);
                finish();
            };

            // Call saveMedicineLog and pass the navigation logic as the callback
            saveMedicineLog(navigateNext);
        });

    }

    private void saveMedicineLog(Runnable onSuccess) {
        String ratingStr = preBreathRatingInput.getText().toString().trim();

        if (medType == null || medType.isEmpty()) {
            Toast.makeText(this, "Please select a medicine type", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(ratingStr)) {
            Toast.makeText(this, "Please enter a breath rating", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            preBreathRating = Integer.parseInt(ratingStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for the rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (preBreathRating < 1 || preBreathRating > 10) {
            Toast.makeText(this, "Breath rating must be between 1 and 10", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String timeStr = timeFormat.format(new Date());

        Map<String, Object> newDoseData = new HashMap<>();
        newDoseData.put("time", timeStr);
        newDoseData.put("pre_breath_rating", preBreathRating);

        String todayDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        date = todayDateStr;

        final DocumentReference todayLogRef = db.collection("medley").document(childId)
                .collection("log").document(todayDateStr);



        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(todayLogRef);

            int nextDoseNum = 1;
            if (snapshot.exists()) {
                Map<String, Object> dailyLog = snapshot.getData();
                Map<String, Object> medicationMap = (Map<String, Object>) dailyLog.get(medType.toLowerCase());
                if (medicationMap != null) {
                    nextDoseNum = medicationMap.size() + 1;
                }
            }
            String newDoseKey = ".dose_" + nextDoseNum;

            Map<String, Object> doseMap = new HashMap<>();
            doseMap.put(newDoseKey, newDoseData);

            Map<String, Object> medTypeMap = new HashMap<>();
            medTypeMap.put(medType.toLowerCase(), doseMap);

            transaction.set(todayLogRef, medTypeMap, SetOptions.merge());

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(TakeMedicineActivity.this, "Medicine log saved successfully", Toast.LENGTH_SHORT).show();
            if (onSuccess != null) {
                onSuccess.run();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(TakeMedicineActivity.this, "Failed to save medicine log: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
