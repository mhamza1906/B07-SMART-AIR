package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ChildDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.child_dashboard);

        Button takeMedButton = (Button)findViewById(R.id.takemedbutton);
        takeMedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChildDashboardActivity.this,TakeMedicineActivity.class));
            }
        });

        Button triageButton = (Button)findViewById(R.id.triagebutton);
        triageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChildDashboardActivity.this,TriageActivity.class));
            }
        });

        Button streakButton = (Button)findViewById(R.id.streakbutton);
        streakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChildDashboardActivity.this,StreakActivity.class));
            }
        });

        Button checkInButton = (Button)findViewById(R.id.checkinbutton);
        checkInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChildDashboardActivity.this,DailyCheckInActivity.class));
            }
        });

        Button childScheduleButton = (Button)findViewById(R.id.childschedulebutton);
        childScheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChildDashboardActivity.this,ChildScheduleActivity.class));
            }
        });

        Button pefButton = (Button)findViewById(R.id.pefbutton);
        pefButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChildDashboardActivity.this,PEFZoneActivity.class));
            }
        });

        Button childSignOutButton = (Button)findViewById(R.id.childsignoutbutton);
        childSignOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChildDashboardActivity.this, ChildSignOutActivity.class));
            }
        });
    }
}

