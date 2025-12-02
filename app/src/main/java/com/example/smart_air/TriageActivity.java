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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
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
            // First, calculate the PEF zone and save it to the class variable
            calculateAndSavePefZone();
            // Second, attempt to display the UI (this will do nothing if in an emergency)
            determineActionPlanFromPEF();
            // Third, update the Firestore log with the newly calculated data
            updateLogWithCurrentState(childId, "PEFEntered");
        });

        rgRescueMed.setOnCheckedChangeListener((group, checkedId) -> updateLogWithCurrentState(childId, "MedicationStatusChanged"));

        btnEmergencyCall.setOnClickListener(v -> {
            // When the button is pressed, update the log to confirm the call
            if (currentIncidentId != null) {
                Map<String, Object> callConfirmation = new HashMap<>();
                callConfirmation.put("emergencyServicesCalled", true);
                mFirestore.collection("triage_incidents").document(childId)
                        .collection("incident_log").document(currentIncidentId)
                        .set(callConfirmation, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Log.d("TriageActivity", "Emergency call confirmed in log."))
                        .addOnFailureListener(e -> Log.e("TriageActivity", "Error confirming emergency call.", e));
            }

            // Then, open the dialer
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
                    currentIncidentId = documentReference.getId();
                    Log.d("TriageActivity", "New incident created with ID: " + currentIncidentId);

                    String message = "Child has started a triage session.";
                    createParentAlert(childId, "TriageModeStarted", message);

                    checkFrequentRescueUse(childId);
                })
                .addOnFailureListener(e -> Log.e("TriageActivity", "Error creating incident.", e));
    }

    private void checkFrequentRescueUse(String childId) {
        Log.d("TriageActivity", "Checking for frequent rescue use...");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -3);
        Date threeHoursAgo = cal.getTime();

        Query recentIncidentsQuery = mFirestore.collection("triage_incidents")
                .document(childId)
                .collection("incident_log")
                .whereGreaterThan("timestamp", threeHoursAgo);

        recentIncidentsQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                int incidentCount = task.getResult().size();
                Log.d("TriageActivity", "Found " + incidentCount + " incidents in the last 3 hours.");

                if (incidentCount >= 3) {
                    String message = "Child has had " + incidentCount + " rescue incidents in the last 3 hours.";
                    createParentAlert(childId, "FrequentRescueUse", message);
                    Toast.makeText(this, "URGENT: Your parent has been sent an urgent alert.", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.w("TriageActivity", "Failed to query for recent incidents.", task.getException());
            }
        });
    }

    private void triggerEmergencyState(String childId, String reason) {
        if (isEmergencyState) return;
        isEmergencyState = true;

        if (tenMinuteTimer != null) {
            tenMinuteTimer.cancel();
        }
        btnEmergencyCall.setVisibility(View.VISIBLE);
        txtActionPlan.setVisibility(View.GONE);

        Map<String, Object> emergencyUpdate = new HashMap<>();
        emergencyUpdate.put("eventType", "EmergencyStateTriggered");
        emergencyUpdate.put("emergencyReason", reason);

        if (currentIncidentId != null) {
            mFirestore.collection("triage_incidents").document(childId)
                    .collection("incident_log").document(currentIncidentId)
                    .set(emergencyUpdate, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d("TriageActivity", "Log updated for EMERGENCY state.");
                        String message = "Child is in an emergency state due to: " + reason;
                        createParentAlert(childId, "EmergencyStateTriggered", message);
                    })
                    .addOnFailureListener(e -> Log.e("TriageActivity", "Error updating log for emergency.", e));
        }
        Toast.makeText(this, "Emergency state triggered: " + reason, Toast.LENGTH_LONG).show();
    }

    private void createParentAlert(String childId, String alertType, String message) {
        DatabaseReference childUserRef = FirebaseDatabase.getInstance().getReference("users").child(childId);

        childUserRef.child("parentId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String parentId = snapshot.getValue(String.class);

                    if (parentId != null && !parentId.isEmpty()) {
                        Map<String, Object> alertData = new HashMap<>();
                        alertData.put("childId", childId);
                        alertData.put("parentId", parentId);
                        alertData.put("alertType", alertType);
                        alertData.put("message", message);
                        alertData.put("timestamp", new Date());
                        alertData.put("isRead", false);

                        mFirestore.collection("parent_alerts").add(alertData)
                                .addOnSuccessListener(aVoid -> Log.d("TriageActivity", "Alert of type '" + alertType + "' created for parent."))
                                .addOnFailureListener(e -> Log.e("TriageActivity", "Failed to create parent alert.", e));
                    } else {
                        Log.e("TriageActivity", "Parent ID field is null or empty for child: " + childId);
                    }
                } else {
                    Log.e("TriageActivity", "Could not find parentId for child: " + childId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TriageActivity", "Database error when finding parentId: " + error.getMessage());
            }
        });
    }

    private void updateLogWithCurrentState(String childId, String eventReason) {
        if (currentIncidentId == null || childId == null) {
            Log.w("TriageActivity", "Cannot update log: incident ID or child ID is missing.");
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("tookRescueMed", rgRescueMed.getCheckedRadioButtonId() == R.id.rbMedYes);
        String pefValue = editCurrentPEF.getText().toString();
        if (!pefValue.isEmpty()) {
            try {
                updates.put("enteredPEF", Integer.parseInt(pefValue));
                updates.put("pefZone", currentPefZone);
            } catch (NumberFormatException e) {
                updates.put("enteredPEF", "Invalid value: " + pefValue);
            }
        }
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

    private void calculateAndSavePefZone() {
        if (personalBestPEF == null || personalBestPEF <= 0) {
            currentPefZone = "PB not set";
            return;
        }
        String currentPEFString = editCurrentPEF.getText().toString();
        if (currentPEFString.isEmpty()) {
            currentPefZone = "Not provided";
            return;
        }
        try {
            int currentPEF = Integer.parseInt(currentPEFString);
            double pefPercentage = ((double) currentPEF / personalBestPEF) * 100;
            currentPefZone = getPEFZone(pefPercentage);
        } catch (NumberFormatException e) {
            currentPefZone = "Invalid input";
        }
    }

    @SuppressLint("SetTextI18n")
    private void determineActionPlanFromPEF() {
        if (isEmergencyState) {
            txtActionPlan.setVisibility(View.GONE);
            return;
        }
        if (currentPefZone != null) {
            displayActionPlanForZone(currentPefZone);
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
        };
        tenMinuteTimer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tenMinuteTimer != null) {
            tenMinuteTimer.cancel();
        }
    }
}
