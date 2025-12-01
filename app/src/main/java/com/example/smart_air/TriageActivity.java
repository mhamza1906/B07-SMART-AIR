package com.example.smart_air;

import android.annotation.SuppressLint;
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

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TriageActivity extends AppCompatActivity {

    // UI Elements
    private CheckBox chkBreathingBreaks, chkHardToBreathe, chkLipColorChange;
    private Button btnEmergencyCall;
    private TextView txtActionPlan;
    private EditText editCurrentPEF;
    private RadioGroup rgRescueMed;

    private FirebaseFirestore mFirestore;

    // Logic
    private CountDownTimer tenMinuteTimer;
    private boolean isEmergencyState = false;
    private String currentIncidentId; // Stores the Document ID for the current rescue event
    private String currentPefZone = "Not yet calculated";
    private Integer personalBestPEF = null;

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
        rgRescueMed = findViewById(R.id.rgRescueMed);
        Button btnEnterPEF = findViewById(R.id.btnEnterPEF);

        mFirestore = FirebaseFirestore.getInstance();
        final String childId = getIntent().getStringExtra("childID");

        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Error: Child ID not found. Cannot start triage.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // --- CORE FUNCTIONALITY ---
        // 1. Create the initial incident log in Firestore when the activity starts.
        createInitialIncidentLog(childId);
        startTenMinuteTimer(childId);

        // 2. Setup UI listeners
        setupRedFlagListeners(childId);

        btnEnterPEF.setOnClickListener(v -> {
            determineActionPlanFromPEF(); // Calculate zone and display it
            updateLogWithCurrentState(childId, "PEFEntered"); // Save the new PEF value
        });

        rgRescueMed.setOnCheckedChangeListener((group, checkedId) -> updateLogWithCurrentState(childId, "MedicationStatusChanged"));

        btnEmergencyCall.setOnClickListener(v -> {
            if (currentIncidentId != null) {
                Map<String, Object> callConfirmation = new HashMap<>();
                callConfirmation.put("emergencyServicesCalled", true);

                mFirestore.collection("triage_incidents").document(childId)
                        .collection("incident_log").document(currentIncidentId)
                        .set(callConfirmation, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Log.d("TriageActivity", "Emergency call confirmed in log."))
                        .addOnFailureListener(e -> Log.e("TriageActivity", "Error confirming emergency call.", e));
            }
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:911"));
            try {
                startActivity(intent);
            } catch (SecurityException e) {
                Log.e("TriageActivity", "Dialer could not be opened.", e);
                Toast.makeText(this, "Could not open dialer.", Toast.LENGTH_SHORT).show();
            }
        });

        fetchPersonalBestPEF(childId);
    }

    /**
     * Creates the initial incident log document when the triage session starts.
     */
    private void createInitialIncidentLog(String childId) {
        Map<String, Object> incidentData = new HashMap<>();
        incidentData.put("timestamp", new Date());
        incidentData.put("eventType", "TriageModeStarted");
        incidentData.put("emergencyServicesCalled", false);
        incidentData.put("enteredPEF", "Not provided");
        incidentData.put("pefZone", "Not yet calculated");
        incidentData.put("tookRescueMed", rgRescueMed.getCheckedRadioButtonId() == R.id.rbMedYes);

        Map<String, Boolean> redFlags = new HashMap<>();
        redFlags.put("unableToTalk", chkBreathingBreaks.isChecked());
        redFlags.put("hardToBreathe", chkHardToBreathe.isChecked());
        redFlags.put("lipColorChange", chkLipColorChange.isChecked());
        incidentData.put("redFlagsPresent", redFlags);

        mFirestore.collection("triage_incidents").document(childId)
                .collection("incident_log").add(incidentData)
                .addOnSuccessListener(documentReference -> {
                    currentIncidentId = documentReference.getId(); // Save the ID for later updates
                    Log.d("TriageActivity", "New incident created with ID: " + currentIncidentId);
                    Toast.makeText(this, "Parent has been notified.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Log.e("TriageActivity", "Error creating incident.", e));
    }

    /**
     * Gathers the current state of UI elements and updates the Firestore document.
     */
    private void updateLogWithCurrentState(String childId, String eventReason) {
        if (currentIncidentId == null || childId == null) {
            Log.w("TriageActivity", "Cannot update log: incident ID or child ID is missing.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();

        // Capture medication status
        updates.put("tookRescueMed", rgRescueMed.getCheckedRadioButtonId() == R.id.rbMedYes);

        // Capture PEF and Zone
        String pefValue = editCurrentPEF.getText().toString();
        if (!pefValue.isEmpty()) {
            try {
                updates.put("enteredPEF", Integer.parseInt(pefValue));
                updates.put("pefZone", currentPefZone);
            } catch (NumberFormatException e) {
                updates.put("enteredPEF", "Invalid value: " + pefValue);
            }
        }

        // Capture red flag status
        Map<String, Boolean> redFlags = new HashMap<>();
        redFlags.put("unableToTalk", chkBreathingBreaks.isChecked());
        redFlags.put("hardToBreathe", chkHardToBreathe.isChecked());
        redFlags.put("lipColorChange", chkLipColorChange.isChecked());
        updates.put("redFlagsPresent", redFlags);

        mFirestore.collection("triage_incidents").document(childId)
                .collection("incident_log").document(currentIncidentId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("TriageActivity", "Log updated for reason: " + eventReason))
                .addOnFailureListener(e -> Log.e("TriageActivity", "Error updating log.", e));
    }

    private void triggerEmergencyState(String childId, String reason) {
        if (isEmergencyState) return;
        isEmergencyState = true;

        if (tenMinuteTimer != null) {
            tenMinuteTimer.cancel();
        }

        btnEmergencyCall.setVisibility(View.VISIBLE);
        txtActionPlan.setVisibility(View.GONE);

        // Update the log with the emergency event
        Map<String, Object> emergencyUpdate = new HashMap<>();
        emergencyUpdate.put("eventType", "EmergencyStateTriggered");
        emergencyUpdate.put("emergencyReason", reason);
        emergencyUpdate.put("lastUpdateTime", new Date());

        if (currentIncidentId != null) {
            mFirestore.collection("triage_incidents").document(childId)
                    .collection("incident_log").document(currentIncidentId)
                    .set(emergencyUpdate, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d("TriageActivity", "Log updated for EMERGENCY state."))
                    .addOnFailureListener(e -> Log.e("TriageActivity", "Error updating log for emergency.", e));
        }

        Toast.makeText(this, "Emergency state triggered: " + reason, Toast.LENGTH_LONG).show();
    }

    private void fetchPersonalBestPEF(String childId) {
        DocumentReference userPefDocRef = mFirestore.collection("PEF").document(childId);
        userPefDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    Long pb = document.getLong("PB");
                    if (pb != null && pb > 0) {
                        personalBestPEF = pb.intValue();
                    } else {
                        txtActionPlan.setText(R.string.pb_not_set);
                        txtActionPlan.setVisibility(View.VISIBLE);
                    }
                } else {
                    txtActionPlan.setText(R.string.pb_not_set);
                    txtActionPlan.setVisibility(View.VISIBLE);
                }
            } else {
                Log.e("TriageActivity", "Failed to fetch PEF data.", task.getException());
                Toast.makeText(TriageActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void determineActionPlanFromPEF() {
        if (personalBestPEF == null) {
            Toast.makeText(this, "Personal Best PEF not loaded yet. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (personalBestPEF <= 0) {
            txtActionPlan.setText(R.string.pb_not_set);
            txtActionPlan.setVisibility(View.VISIBLE);
            return;
        }

        String currentPEFString = editCurrentPEF.getText().toString();
        if (currentPEFString.isEmpty()) {
            txtActionPlan.setText(R.string.prompt_enter_current_pef);
            txtActionPlan.setVisibility(View.VISIBLE);
            currentPefZone = "Not provided";
            return;
        }
        try {
            int currentPEF = Integer.parseInt(currentPEFString);
            double pefPercentage = ((double) currentPEF / personalBestPEF) * 100;
            String zone = getPEFZone(pefPercentage);
            currentPefZone = zone;
            displayActionPlanForZone(zone);
        } catch (NumberFormatException e) {
            txtActionPlan.setText("Please enter a valid number.");
            txtActionPlan.setVisibility(View.VISIBLE);
            currentPefZone = "Invalid input";
        }
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
                break;
        }
    }

    private void setupRedFlagListeners(String childId) {
        View.OnClickListener redFlagListener = v -> {
            updateLogWithCurrentState(childId, "RedFlagChanged");
            checkRedFlagStatus(childId);
        };
        chkBreathingBreaks.setOnClickListener(redFlagListener);
        chkHardToBreathe.setOnClickListener(redFlagListener);
        chkLipColorChange.setOnClickListener(redFlagListener);
    }

    private void checkRedFlagStatus(String childId) {
        if (chkBreathingBreaks.isChecked() || chkHardToBreathe.isChecked() || chkLipColorChange.isChecked()) {
            // Only trigger the emergency state, logging is now handled by the listener
            triggerEmergencyState(childId, "Red flag symptom checked.");
        }
    }

    private void startTenMinuteTimer(String childId) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tenMinuteTimer != null) {
            tenMinuteTimer.cancel();
        }
    }
}
