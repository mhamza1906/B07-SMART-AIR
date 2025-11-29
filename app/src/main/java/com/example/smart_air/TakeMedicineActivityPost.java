package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup; // Import RadioGroup
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TakeMedicineActivityPost extends AppCompatActivity {

    private String medType;
    private String childId;
    private String date;
    private FirebaseFirestore db;
    private RadioGroup postCheckButton;
    private EditText postBreathRatingInput;
    private EditText doseCount;
    private String postCheck;
    private int postBreathRating;
    private int doseNum;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_medicine_post);
        childId = getIntent().getStringExtra("childID");
        medType = getIntent().getStringExtra("type");
        date = getIntent().getStringExtra("date");
        if(childId == null || childId.isEmpty()) {
            Toast.makeText(this,"User ID not found",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if(medType == null || medType.isEmpty()) {
            Toast.makeText(this,"User ID not found",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if(date == null || date.isEmpty()) {
            Toast.makeText(this,"User ID not found",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        db = FirebaseFirestore.getInstance();
        Button finishButton = (Button)findViewById(R.id.finish);
        postCheckButton = (RadioGroup)findViewById(R.id.post_checkup_radio);
        postBreathRatingInput = (EditText)findViewById(R.id.post_breath_rating_input);
        doseCount = (EditText)findViewById(R.id.dose_input);

        postCheckButton.setOnCheckedChangeListener((group, checkedId)->{
            RadioButton selectedButton = findViewById(checkedId);
            postCheck = selectedButton.getText().toString();
        });

        finishButton.setOnClickListener(v -> {
            Runnable navigateNext = () -> {
                Intent switchchildDashboard = new Intent(TakeMedicineActivityPost.this, childDashboardActivity.class);

                switchchildDashboard.putExtra("child_id", childId);

                startActivity(switchchildDashboard);
                finish();
            };
            saveMedicineLog(navigateNext);
        });
    }

    private void saveMedicineLog(Runnable onSuccess){
        if (postCheck == null || postCheck.isEmpty()) {
            Toast.makeText(this, "Please select a medicine type", Toast.LENGTH_SHORT).show();
            return;
        }
        String ratingStr = postBreathRatingInput.getText().toString().trim();
        if (TextUtils.isEmpty(ratingStr)) {
            Toast.makeText(this, "Please enter a breath rating", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            postBreathRating = Integer.parseInt(ratingStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for the rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (postBreathRating < 1 || postBreathRating > 10) {
            Toast.makeText(this, "Breath rating must be between 1 and 10", Toast.LENGTH_SHORT).show();
            return;
        }

        String doseStr = doseCount.getText().toString().trim();
        if (TextUtils.isEmpty(ratingStr)) {
            Toast.makeText(this, "Please enter a breath rating", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            doseNum = Integer.parseInt(ratingStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for the number of doses", Toast.LENGTH_SHORT).show();
            return;
        }

        if (doseNum < 1 ) {
            Toast.makeText(this, "Breath rating must be between 1 and 10", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> newDoseData = new HashMap<>();
        newDoseData.put("doesAmount", doseNum);
        newDoseData.put("post_breath_rating", postBreathRating);
        newDoseData.put("post_checkup", postCheck);

        final DocumentReference todayLogRef = db.collection("medley").document(childId)
                .collection("log").document(date);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(todayLogRef);

            Map<String, Object> dailyLog = snapshot.getData();
            Map<String, Object> medicationMap = (Map<String, Object>) dailyLog.get(medType.toLowerCase());
            int doseId = medicationMap.size() + 1;

            String newDoseKey = ".dose_" + doseId;

            Map<String, Object> doseMap = new HashMap<>();
            doseMap.put(newDoseKey, newDoseData);

            Map<String, Object> medTypeMap = new HashMap<>();
            medTypeMap.put(medType.toLowerCase(), doseMap);

            transaction.set(todayLogRef, medTypeMap, SetOptions.merge());

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(TakeMedicineActivityPost.this, "Medicine log saved successfully", Toast.LENGTH_SHORT).show();
            if (onSuccess != null) {
                onSuccess.run();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(TakeMedicineActivityPost.this, "Failed to save medicine log: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });

    }
}