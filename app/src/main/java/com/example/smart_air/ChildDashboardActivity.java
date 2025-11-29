package com.example.smart_air;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChildDashboardActivity extends AppCompatActivity {

    private View zonecolorview;
    private String childId;

    private TextView percentview;

    private TextView zoneTextView;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.child_dashboard);

        childId = getIntent().getStringExtra("childID");


        if(childId == null) {
            Toast.makeText(this,"User ID not found",Toast.LENGTH_SHORT).show();
            finish();
        }

        db = FirebaseFirestore.getInstance();
        zonecolorview = findViewById(R.id.dashboard_zone_color);
        percentview = findViewById(R.id.dashboard_zone_percentage);
        zoneTextView = findViewById(R.id.dashboard_zone_text);

        listenForTodayZone(childId);

        Button takeMedButton = (Button)findViewById(R.id.takemedbutton);
        takeMedButton.setOnClickListener(v -> {
            Intent childTakeMed = new Intent(ChildDashboardActivity.this, TakeMedicineActivity.class);
            childTakeMed.putExtra("childID",childId);
            startActivity(childTakeMed);
        });

        Button triageButton = (Button)findViewById(R.id.triagebutton);
        triageButton.setOnClickListener(v -> {
            Intent childTriage = new Intent(ChildDashboardActivity.this, TriageActivity.class);
            childTriage.putExtra("childID",childId);
            startActivity(childTriage);
        });

        Button streakButton = (Button)findViewById(R.id.streakbutton);
        streakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent childStreak = new Intent(ChildDashboardActivity.this, AchievementActivity.class);
                childStreak.putExtra("childID",childId);
                startActivity(childStreak);
            }
        });

        Button checkInButton = (Button)findViewById(R.id.checkinbutton);
        checkInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent childCheckIn = new Intent(ChildDashboardActivity.this, DailyCheckInActivity.class);
                childCheckIn.putExtra("childID",childId);
                startActivity(childCheckIn);
            }
        });

        Button childScheduleButton = (Button)findViewById(R.id.childschedulebutton);
        childScheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent childSchedule = new Intent(ChildDashboardActivity.this, ChildScheduleActivity.class);
                childSchedule.putExtra("childID",childId);
                startActivity(childSchedule);
            }
        });

        Button pefButton = (Button)findViewById(R.id.pefbutton);
        pefButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent childPEF = new Intent(ChildDashboardActivity.this, InputPEFActivity.class);
                childPEF.putExtra("childID",childId);
                startActivity(childPEF);
            }
        });

        Button childSignOutButton = (Button)findViewById(R.id.childsignoutbutton);
        childSignOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent childSignOut = new Intent(ChildDashboardActivity.this, ChildSignOutActivity.class);
                childSignOut.putExtra("childID",childId);
                startActivity(childSignOut);
            }
        });
    }

    private void listenForTodayZone(String userID) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        final DocumentReference todayRef = db.collection("PEF").document(userID).collection("log").document(today);

        todayRef.addSnapshotListener(this, (snapshot, e) -> {
            if (e != null) {
                Toast.makeText(ChildDashboardActivity.this, "Failed to load today's zone", Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                String zone = snapshot.getString("zone");
                Long percent = snapshot.getLong("percent");


                if (zone != null) {
                    zoneTextView.setText("Today's Zone: " + zone);

                    String lowerCaseZone = zone.toLowerCase();
                    if (lowerCaseZone.equals("green")) {
                        zonecolorview.setBackgroundColor(Color.GREEN);
                    } else if (lowerCaseZone.equals("yellow")) {
                        zonecolorview.setBackgroundColor(Color.YELLOW);
                    } else if (lowerCaseZone.equals("red")) {
                        zonecolorview.setBackgroundColor(Color.RED);
                    } else {
                        zonecolorview.setBackgroundColor(Color.GRAY);
                    }

                } else {
                    zoneTextView.setText("No Zone");
                    zonecolorview.setBackgroundColor(Color.GRAY);
                }

                if (percent != null) {
                    percentview.setText(percent + getString(R.string.zone_percentage_symbol));
                } else {
                    percentview.setText(R.string.null_zone_percentage);
                }

            } else {
                zoneTextView.setText("No Zone Yet");
                zonecolorview.setBackgroundColor(Color.GRAY);
                percentview.setText(R.string.null_zone_percentage);
            }
        });
    }
}

