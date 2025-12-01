package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class TakeMedicineActivityPost extends AppCompatActivity {

    private String medType;
    private String childID;
    private String date;
    private FirebaseFirestore db;
    private EditText postBreathRatingInput;
    private EditText doseCount;
    private String postCheck;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_medicine_post);
        childID = getIntent().getStringExtra("childID");
        medType = getIntent().getStringExtra("type");
        date = getIntent().getStringExtra("date");
        if(childID == null || childID.isEmpty()) {
            Toast.makeText(this,"User ID not found",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if(medType == null || medType.isEmpty()) {
            Toast.makeText(this,"Medication type not found",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if(date == null || date.isEmpty()) {
            Toast.makeText(this,"Date not found",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        db = FirebaseFirestore.getInstance();
        Button finishButton = findViewById(R.id.finish);
        RadioGroup postCheckButton = findViewById(R.id.post_checkup_radio);
        postBreathRatingInput = findViewById(R.id.post_breath_rating_input);
        doseCount = findViewById(R.id.dose_input);

        postCheckButton.setOnCheckedChangeListener((group, checkedId)->{
            RadioButton selectedButton = findViewById(checkedId);
            postCheck = selectedButton.getText().toString();
        });

        finishButton.setOnClickListener(v -> {
            Runnable navigateNext = () -> {
                Intent switchChildDashboard = new Intent(TakeMedicineActivityPost.this, ChildDashboardActivity.class);
                switchChildDashboard.putExtra("childID", childID);
                startActivity(switchChildDashboard);
                finish();
            };
            saveMedicineLog(navigateNext);
        });
    }

    private void saveMedicineLog(Runnable onSuccess){
        if (postCheck == null || postCheck.isEmpty()) {
            Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show();
            return;
        }
        String ratingStr = postBreathRatingInput.getText().toString().trim();
        if (TextUtils.isEmpty(ratingStr)) {
            Toast.makeText(this, "Please enter a breath rating", Toast.LENGTH_SHORT).show();
            return;
        }

        int postBreathRating;
        try {
            postBreathRating = Integer.parseInt(ratingStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for the rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (postBreathRating < 1) {
            Toast.makeText(this, "Breath rating must be greater than or equal to 1", Toast.LENGTH_SHORT).show();
            return;
        }

        String doseStr = doseCount.getText().toString().trim();
        if (TextUtils.isEmpty(doseStr)) {
            Toast.makeText(this, "Please enter the number of doses you took.", Toast.LENGTH_SHORT).show();
            return;
        }

        int doseNum;
        try {
            doseNum = Integer.parseInt(doseStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for the number of doses", Toast.LENGTH_SHORT).show();
            return;
        }

        if (doseNum < 1 ) {
            Toast.makeText(this, "Dose count must be at least 1", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> newDoseData = new HashMap<>();
        newDoseData.put("doseAmount", doseNum);
        newDoseData.put("post_breath_rating", postBreathRating);
        newDoseData.put("post_checkup", postCheck);

        final DocumentReference todayLogRef = db.collection("medlog").document(childID)
                .collection("log").document(date);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(todayLogRef);
            int doseId = 1;
            if (snapshot.exists() && snapshot.getData() != null) {
                Map<String, Object> dailyLog = snapshot.getData();
                if (dailyLog.containsKey(medType.toLowerCase())) {
                    Map<?, ?> medData = (Map<?, ?>) dailyLog.get(medType.toLowerCase());
                    if (medData != null) {
                        doseId = medData.size();
                    }
                }
            }

            String newDoseKey = "dose_" + doseId;
            Map<String, Object> doseMap = new HashMap<>();
            doseMap.put(newDoseKey, newDoseData);

            Map<String, Object> medTypeMap = new HashMap<>();
            medTypeMap.put(medType.toLowerCase(), doseMap);

            transaction.set(todayLogRef, medTypeMap, SetOptions.merge());
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(TakeMedicineActivityPost.this, "Your medicine log saved successfully", Toast.LENGTH_SHORT).show();
            if (onSuccess != null) {
                onSuccess.run();
            }
            updateStreaks(childID, medType, date);
            updateRescueRolling30Days(childID, date);
            updateWeeklyRescueUsage(childID, date);

            if (medType.equalsIgnoreCase("rescue")) {
                updateLastRescueUse(childID);
            }

        }).addOnFailureListener(e -> Toast.makeText(TakeMedicineActivityPost.this, "Failed to save medicine log: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }


    private void updateStreaks(String childID, String medType, String date) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference techDoc = db.collection("techniqueStreak")
                .document(childID)
                .collection("allCompletion")
                .document(date);

        techDoc.get().addOnSuccessListener(snap -> {

            boolean alreadyCompleted =
                    snap.exists() && Boolean.TRUE.equals(snap.getBoolean("completed"));

            if (!alreadyCompleted) {

                Map<String, Object> map = new HashMap<>();
                map.put("completed", true);
                techDoc.set(map, SetOptions.merge());

                DocumentReference streakRef =
                        db.collection("techniqueStreak").document(childID);

                streakRef.get().addOnSuccessListener(streakSnap -> {
                    long newCount = 1;
                    if (streakSnap.exists()) {
                        Long old = streakSnap.getLong("consecutive_days");
                        if (old != null) newCount = old + 1;
                    }

                    Map<String, Object> update = new HashMap<>();
                    update.put("consecutive_days", newCount);
                    update.put("last_day", date);

                    streakRef.set(update, SetOptions.merge());
                });
            }
        });

        if (TextUtils.isEmpty(medType) || !medType.equalsIgnoreCase("Controller"))
            return;

        DocumentReference todayPlanRef = db.collection("planned-schedule")
                .document(childID)
                .collection("schedules")
                .document(date);

        todayPlanRef.get().addOnSuccessListener(todaySnap -> {

            if (!todaySnap.exists()) return;

            Long controllerNumber = todaySnap.getLong("controllerNumber");
            Long completedNumber = todaySnap.getLong("completedNumber");

            if (controllerNumber == null) controllerNumber = 0L;
            if (completedNumber == null) completedNumber = 0L;

            long newCompleted = completedNumber + 1;

            boolean todayCompleted = newCompleted >= controllerNumber;

            Map<String, Object> updateToday = new HashMap<>();
            updateToday.put("completedNumber", newCompleted);
            updateToday.put("taken", todayCompleted);
            todayPlanRef.set(updateToday, SetOptions.merge());


            db.collection("planned-schedule")
                    .document(childID)
                    .collection("schedules")
                    .get()
                    .addOnSuccessListener(allDatesSnap -> {

                        String prevDate = null;

                        for (DocumentSnapshot d : allDatesSnap.getDocuments()) {
                            String id = d.getId();
                            if (id.compareTo(date) < 0) {
                                if (prevDate == null || id.compareTo(prevDate) > 0) {
                                    prevDate = id;
                                }
                            }
                        }

                        if (prevDate == null) {
                            applyControllerStreak(childID, date, false, todayCompleted);
                            return;
                        }

                        DocumentReference prevRef =
                                db.collection("planned-schedule")
                                        .document(childID)
                                        .collection("schedules")
                                        .document(prevDate);

                        prevRef.get().addOnSuccessListener(prevSnap -> {

                            Boolean prevTaken = prevSnap.getBoolean("taken");
                            boolean lastCompleted = (prevTaken != null && prevTaken);

                            applyControllerStreak(childID, date, lastCompleted, todayCompleted);
                        });
                    });
        });
    }


    private void applyControllerStreak(String childID,
                                       String date,
                                       boolean lastCompleted,
                                       boolean todayCompleted) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference streakRef =
                db.collection("controllerStreak").document(childID);

        streakRef.get().addOnSuccessListener(snap -> {

            long old = 0;
            if (snap.exists()) {
                Long t = snap.getLong("consecutive_days");
                if (t != null) old = t;
            }

            long newCount;

            if (!lastCompleted && !todayCompleted) {
                newCount = 0;
            } else if (!lastCompleted && todayCompleted) {
                newCount = 1;
            } else if (lastCompleted && !todayCompleted) {
                newCount = old;
            } else {
                newCount = old + 1;
            }

            Map<String, Object> up = new HashMap<>();
            up.put("consecutive_days", newCount);
            up.put("last_completed", lastCompleted);
            up.put("today_completed", todayCompleted);
            up.put("last_day", date);

            streakRef.set(up, SetOptions.merge());
        });
    }

    private void updateRescueRolling30Days(String childID, String todayDate) {
        List<String> last30Days = getLast30Days(todayDate);
        CollectionReference logRef = db.collection("medlog").document(childID).collection("log");

        logRef.get().addOnSuccessListener(snap -> {
            int count = 0;
            for (DocumentSnapshot d : snap.getDocuments()) {
                String date = d.getId();
                if (last30Days.contains(date)) {
                    Map<String, Object> data = d.getData();
                    if (data != null && data.containsKey("rescue")) {
                        count++;
                    }
                }
            }

            DocumentReference parentDoc = db.collection("medlog").document(childID);
            Map<String, Object> update = new HashMap<>();
            update.put("rescue_in_30_days", count);
            update.put("last_update_day", todayDate);
            parentDoc.set(update, SetOptions.merge());
        });
    }

    private List<String> getLast30Days(String today) {
        List<String> list = new ArrayList<>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(today);
            if (date == null) return list;

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            for (int i = 0; i < 30; i++) {
                String d = sdf.format(cal.getTime());
                list.add(d);
                cal.add(Calendar.DATE, -1);
            }
        } catch (Exception ignored) {}
        return list;
    }

    private void updateWeeklyRescueUsage(String childID, String todayDate) {
        List<String> last7Days = getLast7Days(todayDate);
        CollectionReference logRef = db.collection("medlog").document(childID).collection("log");

        logRef.get().addOnSuccessListener(snap -> {
            int count = 0;
            for (DocumentSnapshot d : snap.getDocuments()) {
                String date = d.getId();
                if (last7Days.contains(date)) {
                    Map<String, Object> data = d.getData();
                    if (data != null && data.containsKey("rescue")) {
                        count++;
                    }
                }
            }

            DocumentReference parentDoc = db.collection("medlog").document(childID);
            Map<String, Object> update = new HashMap<>();
            update.put("rescue_use_in_last_7_days", count);
            parentDoc.set(update, SetOptions.merge());
        });
    }

    private List<String> getLast7Days(String today) {
        List<String> list = new ArrayList<>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(today);
            if (date == null) return list;

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            for (int i = 0; i < 7; i++) {
                String d = sdf.format(cal.getTime());
                list.add(d);
                cal.add(Calendar.DATE, -1);
            }
        } catch (Exception ignored) {}
        return list;
    }

    private void updateLastRescueUse(String childID) {
        DocumentReference parentDoc = db.collection("medlog").document(childID);
        Map<String, Object> update = new HashMap<>();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        update.put("last_rescue_use", timestamp);
        parentDoc.set(update, SetOptions.merge());
    }
}
