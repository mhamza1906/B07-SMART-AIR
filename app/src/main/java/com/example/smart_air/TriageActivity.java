package com.example.smart_air;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore; // --- FIX: Import Firestore ---

import java.util.Date; // --- FIX: Import Date for timestamp ---
import java.util.HashMap; // --- FIX: Import HashMap to create data object ---
import java.util.Map; // --- FIX: Import Map ---

public class TriageActivity extends AppCompatActivity {

    // UI Elements
    private CheckBox chkBreathingBreaks, chkHardToBreathe, chkLipColorChange;
    private Button btnEmergencyCall;
    private TextView txtActionPlan;
    private EditText editCurrentPEF, editMedTime;
    private RadioGroup rgRescueMed;

    private DatabaseReference mUserRef; // For Realtime DB (user profile)
    private FirebaseFirestore mFirestore; // --- FIX: Add a Firestore instance variable ---
    private FirebaseUser currentUser;

    // Logic
    private CountDownTimer tenMinuteTimer;
    private boolean isEmergencyState = false;
    private String triageIncidentId; // --- FIX: To store the unique ID for this specific incident ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triage);

        // Initialize UI
        chkBreathingBreaks = findViewById(R.id.chkBreathingBreaks);
        chkHardToBreathe = findViewById(R.id.chkHardToBreathe);
        chkLipColorChange = findViewById(R.id.chkLipColorChange);
        btnEmergencyCall = findViewById(R.id.btnEmergencyCall);
        txtActionPlan = findViewById(R.id.txtActionPlan);
        editCurrentPEF = findViewById(R.id.editCurrentPEF);
        editMedTime = findViewById(R.id.editMedTime);
        rgRescueMed = findViewById(R.id.rgRescueMed);

        // Initialize Firebase
        // Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance(); // --- FIX: Initialize Firestore ---
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Error: No user logged in.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mUserRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        // --- CORE FUNCTIONALITY ---

        // --- FIX: Log the start of the triage session to Firestore ---
        logTriageEventToFirestore(); // false because emergency services have not been called yet

        // 1. Immediately notify parent and start the 10-minute timer
        notifyParent("TriageModeStarted");
        startTenMinuteTimer();

        // ... (rest of your onCreate method is the same)
        setupRedFlagListeners();
        setupMedicationListener();
        btnEmergencyCall.setOnClickListener(v -> {
            // Log that the user is calling emergency services
            updateTriageLogWithEmergencyCall();
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:911"));
            startActivity(intent);
        });
        fetchUserDataAndDetermineActionPlan();
    }

    // --- FIX: NEW METHOD TO LOG THE TRIAGE EVENT ---
    private void logTriageEventToFirestore() {
        if (currentUser == null) return;

        // Create a data object to store the triage information
        Map<String, Object> triageLog = new HashMap<>();
        triageLog.put("userId", currentUser.getUid());
        triageLog.put("timestamp", new Date()); // Uses the current date and time
        triageLog.put("emergencyServicesCalled", false);

        // A unique ID for this specific incident is generated automatically by .document()
        triageIncidentId = mFirestore.collection("triage_incidents").document().getId();

        mFirestore.collection("triage_incidents").document(triageIncidentId)
                .set(triageLog)
                .addOnSuccessListener(aVoid -> Log.d("TriageActivity", "Triage event logged successfully."))
                .addOnFailureListener(e -> Log.w("TriageActivity", "Error logging triage event.", e));
    }

    // --- FIX: NEW METHOD TO UPDATE THE LOG WHEN EMERGENCY IS CALLED ---
    private void updateTriageLogWithEmergencyCall() {
        if (triageIncidentId != null && !triageIncidentId.isEmpty()) {
            mFirestore.collection("triage_incidents").document(triageIncidentId)
                    .update("emergencyServicesCalled", true)
                    .addOnSuccessListener(aVoid -> Log.d("TriageActivity", "Triage log updated for emergency call."))
                    .addOnFailureListener(e -> Log.w("TriageActivity", "Error updating triage log.", e));
        }
    }


    private void triggerEmergencyState(String reason) {
        isEmergencyState = true;

        if (tenMinuteTimer != null) {
            tenMinuteTimer.cancel();
        }

        btnEmergencyCall.setVisibility(View.VISIBLE);
        txtActionPlan.setVisibility(View.GONE);

        // --- FIX: Update the existing Firestore log to reflect the emergency state ---
        updateTriageLogWithEmergencyCall();

        notifyParent("EmergencyStateTriggered");
        Toast.makeText(this, "Emergency state triggered: " + reason, Toast.LENGTH_LONG).show();
    }

    // ... (The rest of your TriageActivity.java methods remain the same)
    private void setupRedFlagListeners() {
        View.OnClickListener redFlagListener = v -> checkRedFlagStatus();
        chkBreathingBreaks.setOnClickListener(redFlagListener);
        chkHardToBreathe.setOnClickListener(redFlagListener);
        chkLipColorChange.setOnClickListener(redFlagListener);
    }

    private void setupMedicationListener() {
        rgRescueMed.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbMedYes) {
                editMedTime.setVisibility(View.VISIBLE);
            } else {
                editMedTime.setVisibility(View.GONE);
            }
        });
    }

    private void checkRedFlagStatus() {
        if (chkBreathingBreaks.isChecked() || chkHardToBreathe.isChecked() || chkLipColorChange.isChecked()) {
            if (!isEmergencyState) {
                triggerEmergencyState("Red flag symptom checked.");
            }
        }
    }

    private void startTenMinuteTimer() {
        tenMinuteTimer = new CountDownTimer(10 * 60 * 1000, 1000) { // 10 minutes
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (!isEmergencyState) {
                    triggerEmergencyState("Condition not improving after 10 minutes.");
                }
            }
        }.start();
    }

    private void fetchUserDataAndDetermineActionPlan() {
        mUserRef.child("pefPersonalBest").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Integer personalBest = dataSnapshot.getValue(Integer.class);
                    if (personalBest != null && personalBest > 0) {
                        try {
                            int currentPEF = Integer.parseInt(editCurrentPEF.getText().toString());
                            double pefPercentage = ((double) currentPEF / personalBest) * 100;
                            String zone = getPEFZone(pefPercentage);
                            displayActionPlanForZone(zone);
                        } catch (NumberFormatException e) {
                            txtActionPlan.setText(R.string.prompt_enter_current_pef);
                            txtActionPlan.setVisibility(View.VISIBLE);
                        }
                    } else {
                        txtActionPlan.setText(R.string.pb_not_set);
                        txtActionPlan.setVisibility(View.VISIBLE);
                    }
                } else {
                    txtActionPlan.setText(R.string.pb_not_set);
                    txtActionPlan.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TriageActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getPEFZone(double percentage) {
        if (percentage >= 80) return "Green";
        if (percentage >= 50) return "Yellow";
        return "Red";
    }

    private void displayActionPlanForZone(String zone) {
        txtActionPlan.setVisibility(View.VISIBLE);
        switch (zone) {
            case "Green":
                txtActionPlan.setText(R.string.green_zone);
                break;
            case "Yellow":
                txtActionPlan.setText(R.string.yellow_zone);
                break;
            case "Red":
                txtActionPlan.setText(R.string.red_zone);
                if (!isEmergencyState) {
                    triggerEmergencyState("PEF is in the Red Zone.");
                }
                break;
        }
    }

    private void notifyParent(String eventType) {
        mUserRef.child("triageEvents").push().setValue(eventType + " at " + System.currentTimeMillis());
        Toast.makeText(this, "Parent has been notified.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tenMinuteTimer != null) {
            tenMinuteTimer.cancel();
        }
    }
}
