package com.example.smart_air;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class ViewChildSummaryActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private String childID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_child_summary);

        childID = getIntent().getStringExtra("childID");

        if (childID == null) {
            Toast.makeText(this, "Child ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navView = findViewById(R.id.navView);

        navView.setItemIconTintList(null);
        navView.setItemTextColor(ColorStateList.valueOf(Color.BLACK));

        findViewById(R.id.btnMenu).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        navView.setNavigationItemSelectedListener(item -> {
            handleMenuClick(item);
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });


        loadPage(
                "Press START to view this child's rescue medication usag, " +
                        "and decide whether to share this summary with your healthcare provider",
                RescueLogsSummaryActivity.class
        );
    }

    private void handleMenuClick(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.navRescueLogs) {
            loadPage("Press START to view rescue medication logs, " +
                    "and decide whether to share this summary with your healthcare provider", RescueLogsSummaryActivity.class);

        } else if (id == R.id.navController) {
            loadPage("Press START to view controller adherence summary," +
                    "and decide whether to share this summary with your healthcare provider", ControllerAdherenceSummaryActivity.class);

        } else if (id == R.id.navSymptoms) {
            loadPage("Press START to view symptom reports, " +
                    "and decide whether to share this summary with your healthcare provider", SymptomSummaryActivity.class);

        } else if (id == R.id.navTriggers) {
            loadPage("Press START to view recorded triggers, " +
                    "and decide whether to share this summary with your healthcare provider", TriggerSummaryActivity.class);

        } else if (id == R.id.navPEF) {
            loadPage("Press START to view peak-flow history, " +
                    "and decide whether to share this summary with your healthcare provider", PEFSummaryActivity.class);

        } else if (id == R.id.navTriage) {
            loadPage("Press START to view triage incidents, " +
                    "and decide whether to share this summary with your healthcare provider", TriageIncidentSummaryActivity.class);

        } else if (id == R.id.navHistoryBrowser){
            loadPage("Press START to use History Browser", HistoryBrowserActivity.class);
        }
    }

    private void loadPage(String titleText, Class<?> targetActivity) {

        FrameLayout container = findViewById(R.id.rightContent);
        container.removeAllViews();

        View page = LayoutInflater.from(this)
                .inflate(R.layout.view_child_summary_page, container, false);

        TextView titleTextView = page.findViewById(R.id.txtTitle);
        Button startBtn = page.findViewById(R.id.btnStart);

        titleTextView.setText(titleText);

        startBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, targetActivity);
            intent.putExtra("childID", childID);
            startActivity(intent);
        });

        container.addView(page);
    }
}
