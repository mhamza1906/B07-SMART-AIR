package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChildDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.child_dashboard);

        String childId = getIntent().getStringExtra("childID");

        if(childId == null) {
            Toast.makeText(this,"User ID not found",Toast.LENGTH_SHORT).show();
            finish();
        }

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
                Intent childPEF = new Intent(ChildDashboardActivity.this, PEFZoneActivity.class);
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
}

