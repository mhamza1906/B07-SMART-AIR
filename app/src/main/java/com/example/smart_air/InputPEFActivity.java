package com.example.smart_air;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

public class InputPEFActivity extends AppCompatActivity {

    private String userID;
    private FirebaseFirestore db;
    private EditText pefInput;
    private Button enterPEFBtn;
    private TextView todayZoneText;
    private TextView pbViewer;
    private View zoneColorView;
    private TextView percentView;
    private TextView sharedProviderText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.child_pef);

        db = FirebaseFirestore.getInstance();

        pefInput = findViewById(R.id.pefinput);
        enterPEFBtn = findViewById(R.id.enterpefbtn);
        todayZoneText = findViewById(R.id.todayzone);
        pbViewer = findViewById(R.id.pbviewer);
        zoneColorView = findViewById(R.id.zonecolor);
        percentView = findViewById(R.id.zone_percentage);


        userID = getIntent().getStringExtra("childID");


        if (userID != null && !userID.isEmpty()) {
            loadOrCreatePEFNode(userID);
            displayPB(userID);
            listenForTodayZone(userID);

        } else {
            Toast.makeText(this, "User ID not found. Cannot load data.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        enterPEFBtn.setOnClickListener(v -> submitPEF());
    }

    private void submitPEF() {
        String enteredStr = pefInput.getText().toString().trim();
        if (enteredStr.isEmpty()) {
            Toast.makeText(this, "Invalid PEF Entry", Toast.LENGTH_SHORT).show();
            return;
        }

        int enteredValue;
        try {
            enteredValue = Integer.parseInt(enteredStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid PEF Entry", Toast.LENGTH_SHORT).show();
            return;
        }

        if (enteredValue < 50 || enteredValue > 900) {
            Toast.makeText(this, "Invalid PEF Entry", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference userDocRef = db.collection("PEF").document(userID);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DocumentReference todayLogRef = userDocRef.collection("log").document(today);


        userDocRef.get().addOnSuccessListener(userDoc -> {
            long pb = 0;
            if (userDoc.exists() && userDoc.contains("PB")) {
                pb = userDoc.getLong("PB");
            }


            final long finalPB;
            if (pb > 0) {
                finalPB = pb;
            } else {
                finalPB = 900;
            }


            todayLogRef.get().addOnSuccessListener(logDoc -> {
                long currentDailyHigh = 0;
                if (logDoc.exists() && logDoc.contains("value")) {
                    currentDailyHigh = logDoc.getLong("value");
                }

                if (enteredValue > currentDailyHigh) {
                    int percent = (int) Math.round(((double) enteredValue / finalPB) * 100);
                    String zone;
                    if (percent >= 80) zone = "green";
                    else if (percent >= 50) zone = "yellow";
                    else zone = "red";

                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("value", enteredValue);
                    updateData.put("percent", percent);
                    updateData.put("zone", zone);
                    if (!logDoc.exists()) {
                        updateData.put("stored_PB", finalPB);
                    }

                    todayLogRef.set(updateData, SetOptions.merge()).addOnSuccessListener(aVoid -> {
                        Toast.makeText(InputPEFActivity.this, "PEF value saved!", Toast.LENGTH_SHORT).show();
                        pefInput.setText("");
                    }).addOnFailureListener(e -> Toast.makeText(InputPEFActivity.this, "Failed to save PEF.", Toast.LENGTH_SHORT).show());
                } else {
                    pefInput.setText("");
                }


            }).addOnFailureListener(e -> Toast.makeText(InputPEFActivity.this, "Could not read daily log.", Toast.LENGTH_SHORT).show());
        }).addOnFailureListener(e -> Toast.makeText(InputPEFActivity.this, "Could not verify PB.", Toast.LENGTH_SHORT).show());
    }

    private void loadOrCreatePEFNode(String userID) {
        DocumentReference userDocRef = db.collection("PEF").document(userID);
        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (!document.exists()) {
                    Map<String, Object> defaultPEF = new HashMap<>();
                    defaultPEF.put("PB", 900); // Corrected to uppercase 'PB'
                    defaultPEF.put("graph_day_range", 7);

                    userDocRef.set(defaultPEF).addOnSuccessListener(aVoid ->
                            Toast.makeText(InputPEFActivity.this, "PEF node created for user.", Toast.LENGTH_SHORT).show()
                    ).addOnFailureListener(e ->
                            Toast.makeText(InputPEFActivity.this, "Failed to create PEF node.", Toast.LENGTH_SHORT).show()
                    );
                }
            } else {
                Toast.makeText(InputPEFActivity.this, "Error checking user PEF data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForTodayZone(String userID) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        final DocumentReference todayRef = db.collection("PEF").document(userID).collection("log").document(today);

        todayRef.addSnapshotListener(this, (snapshot, e) -> {
            if (e != null) {
                Toast.makeText(InputPEFActivity.this, "Failed to load today's zone", Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                String zone = snapshot.getString("zone");
                Long percent = snapshot.getLong("percent");

                if (zone != null) {
                    todayZoneText.setText(getString(R.string.today_pef_zone) + " " + zone);

                    String lowerCaseZone = zone.toLowerCase();
                    if (lowerCaseZone.equals("green")) {
                        zoneColorView.setBackgroundColor(Color.GREEN);
                    } else if (lowerCaseZone.equals("yellow")) {
                        zoneColorView.setBackgroundColor(Color.YELLOW);
                    } else if (lowerCaseZone.equals("red")) {
                        zoneColorView.setBackgroundColor(Color.RED);
                    } else {
                        zoneColorView.setBackgroundColor(Color.GRAY);
                    }

                } else {
                    todayZoneText.setText(R.string.no_zone);
                    zoneColorView.setBackgroundColor(Color.GRAY);
                }

                if (percent != null) {
                    percentView.setText(percent + getString(R.string.zone_percentage_symbol));
                } else {
                    percentView.setText(R.string.null_zone_percentage);
                }

            } else {
                todayZoneText.setText(R.string.no_zone_yet);
                zoneColorView.setBackgroundColor(Color.GRAY);
                percentView.setText(R.string.null_zone_percentage);
            }
        });
    }

    private void displayPB(String userID) {
        final DocumentReference userDocRef = db.collection("PEF").document(userID);

        userDocRef.addSnapshotListener(this, (snapshot, e) -> {
            if (e != null) {
                Toast.makeText(InputPEFActivity.this, "Failed to load PB.", Toast.LENGTH_SHORT).show();
                pbViewer.setText(getString(R.string.pb_pef));
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                Long pb = snapshot.getLong("PB");
                if (pb != null && pb > 0) {
                    pbViewer.setText("PB: " + pb);
                } else {
                    pbViewer.setText(getString(R.string.pb_pef));
                }
            } else {
                pbViewer.setText(getString(R.string.pb_pef));
            }
        });
    }
}


