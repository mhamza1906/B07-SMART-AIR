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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TriageActivity extends AppCompatActivity {

    // UI Elements
    private CheckBox chkBreathingBreaks, chkHardToBreathe, chkLipColorChange;
    private Button btnEmergencyCall;
    private TextView txtActionPlan;
    private EditText editCurrentPEF, editMedTime;
    private RadioGroup rgRescueMed;

    // Firebase
    private DatabaseReference mUserRef;     // For reading user profile from Realtime DB
    private FirebaseFirestore mFirestore;   // For writing incident logs to Firestore

    // Logic
    private CountDownTimer tenMinuteTimer;
    private boolean isEmergencyState = false;
    private String currentIncidentId; // Stores the Document ID for the current rescue event

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

        // Initialize Firestore
        mFirestore = FirebaseFirestore.getInstance();

        // Get the childID from the Intent, as it's passed from the ChildDashboard
        final String childId = getIntent().getStringExtra("childID");

        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Error: Child ID not found. Cannot start triage.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // This reference is ONLY for reading the user's PEF from Realtime DB
        mUserRef = FirebaseDatabase.getInstance().getReference("users").child(childId);

        // --- CORE FUNCTIONALITY ---

        // 1. Log the start of the triage session to Firestore AND trigger parent alert
        triggerParentAlert(childId, "TriageModeStarted");
        startTenMinuteTimer();

        // 2. Setup UI listeners
        setupRedFlagListeners(childId);
        setupMedicationListener();
        btnEmergencyCall.setOnClickListener(v -> {
            triggerParentAlert(childId, "EmergencyStateTriggered");
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:911"));
            try {
                startActivity(intent);
            } catch (SecurityException e) {
                Log.e("TriageActivity", "Dialer could not be opened.", e);
                Toast.makeText(this, "Could not open dialer.", Toast.LENGTH_SHORT).show();
            }
        });

        fetchUserDataAndDetermineActionPlan(childId);
    }

    /**
     * Creates or updates a rescue document in the user's incident_log subcollection in Firestore.
     * This action is detected by a Cloud Function to send notifications.
     */
    private void triggerParentAlert(String childId, String eventType) {
        if (childId == null || childId.isEmpty()) return;

        Map<String, Object> incidentData = new HashMap<>();
        incidentData.put("timestamp", new Date());
        incidentData.put("eventType", eventType);
        incidentData.put("emergencyServicesCalled", eventType.equals("EmergencyStateTriggered"));

        // --- FIX: Reference the new Firestore structure ---
        // Path: triage_incidents -> {userId} -> incident_log -> {new_rescue_id}
        if (currentIncidentId == null) {
            // This is the first event for this session, so create a new rescue document
            mFirestore.collection("triage_incidents").document(childId)
                    .collection("incident_log").add(incidentData)
                    .addOnSuccessListener(documentReference -> {
                        currentIncidentId = documentReference.getId(); // Save the new rescue ID
                        Log.d("TriageActivity", "New rescue event created in Firestore with ID: " + currentIncidentId);
                        Toast.makeText(this, "Parent has been notified.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Log.e("TriageActivity", "Error creating rescue event.", e));
        } else {
            // An incident is already in progress, just update the existing rescue document
            mFirestore.collection("triage_incidents").document(childId)
                    .collection("incident_log").document(currentIncidentId)
                    .update(incidentData) // or .set(incidentData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d("TriageActivity", "Rescue event updated for emergency.");
                        Toast.makeText(this, "Parent has been re-notified of the emergency.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Log.e("TriageActivity", "Error updating rescue event.", e));
        }
    }

    private void triggerEmergencyState(String childId, String reason) {
        if (isEmergencyState) return;
        isEmergencyState = true;

        if (tenMinuteTimer != null) {
            tenMinuteTimer.cancel();
        }

        btnEmergencyCall.setVisibility(View.VISIBLE);
        txtActionPlan.setVisibility(View.GONE);

        // Update the Firestore log with the emergency reason
        triggerParentAlert(childId, "EmergencyStateTriggered");

        Toast.makeText(this, "Emergency state triggered: " + reason, Toast.LENGTH_LONG).show();
    }

    // --- FIX: Pass childId to the listeners so they can use it ---
    private void setupRedFlagListeners(String childId) {
        View.OnClickListener redFlagListener = v -> checkRedFlagStatus(childId);
        chkBreathingBreaks.setOnClickListener(redFlagListener);
        chkHardToBreathe.setOnClickListener(redFlagListener);
        chkLipColorChange.setOnClickListener(redFlagListener);
    }

    private void checkRedFlagStatus(String childId) {
        if (chkBreathingBreaks.isChecked() || chkHardToBreathe.isChecked() || chkLipColorChange.isChecked()) {
            if (!isEmergencyState) {
                triggerEmergencyState(childId, "Red flag symptom checked.");
            }
        }
    }

    // Other methods that need childId have been updated.
    // The rest of your methods are correct and do not need changes.
    // ... (startTenMinuteTimer, displayActionPlanForZone, etc.)

    private void setupMedicationListener() {
        rgRescueMed.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbMedYes) {
                editMedTime.setVisibility(View.VISIBLE);
            } else {
                editMedTime.setVisibility(View.GONE);
            }
        });
    }

    private void startTenMinuteTimer() {
        final String childId = getIntent().getStringExtra("childID");
        tenMinuteTimer = new CountDownTimer(10 * 60 * 1000, 1000) { // 10 minutes
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (!isEmergencyState) {
                    triggerEmergencyState(childId, "Condition not improving after 10 minutes.");
                }
            }
        }.start();
    }

    private void fetchUserDataAndDetermineActionPlan(String childId) {
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
                            displayActionPlanForZone(childId, zone);
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

    private void displayActionPlanForZone(String childId, String zone) {
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
                    triggerEmergencyState(childId, "PEF is in the Red Zone.");
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tenMinuteTimer != null) {
            tenMinuteTimer.cancel();
        }
    }
}
