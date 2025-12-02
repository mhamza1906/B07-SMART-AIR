package com.example.smart_air;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class ProviderViewChildSummaryActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    private String childId;

    private boolean showSymptoms, showTriggers, showRescue,
            showController, showPEF, showTriage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.provider_view_child_summary);

        drawerLayout = findViewById(R.id.drawerLayout);

        childId = getIntent().getStringExtra("childID");
        String providerUsername = getIntent().getStringExtra("providerUsername");

        if (childId == null || providerUsername == null) {
            Toast.makeText(this, "Missing child/provider info.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        readVisibilityExtras();

        ImageView btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(findViewById(R.id.navView)));

        NavigationView nav = findViewById(R.id.navView);
        nav.setItemIconTintList(null);
        nav.setItemTextColor(ColorStateList.valueOf(Color.BLACK));


        setupMenu();

        checkAndLoadPage(showRescue,
                "Rescue Medication Summary",
                RescueLogsSummaryActivity.class);
    }

    private void readVisibilityExtras() {
        showSymptoms = getIntent().getBooleanExtra("summary_symptoms", false);
        showTriggers = getIntent().getBooleanExtra("summary_triggers", false);
        showRescue = getIntent().getBooleanExtra("summary_rescue", false);
        showController = getIntent().getBooleanExtra("summary_controller", false);
        showPEF = getIntent().getBooleanExtra("summary_pef", false);
        showTriage = getIntent().getBooleanExtra("summary_triage", false);
    }

    private void setupMenu() {
        NavigationView nav = findViewById(R.id.navView);

        nav.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.navSymptoms) {
                checkAndLoadPage(showSymptoms, "Symptoms Summary",
                        SymptomSummaryActivity.class);

            } else if (id == R.id.navTriggers) {
                checkAndLoadPage(showTriggers, "Trigger Summary",
                        TriggerSummaryActivity.class);

            } else if (id == R.id.navRescueLogs) {
                checkAndLoadPage(showRescue, "Rescue Medication Summary",
                        RescueLogsSummaryActivity.class);

            } else if (id == R.id.navController) {
                checkAndLoadPage(showController, "Controller Adherence Summary",
                        ControllerAdherenceSummaryActivity.class);

            } else if (id == R.id.navPEF) {
                checkAndLoadPage(showPEF, "Peak Flow Summary",
                        PEFSummaryActivity.class);

            } else if (id == R.id.navTriage) {
                checkAndLoadPage(showTriage, "Triage Incidents Summary",
                        TriageIncidentSummaryActivity.class);
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void checkAndLoadPage(boolean allowed, String title, Class<?> target) {
        if (!allowed) {
            Toast.makeText(this, title + " is not shared by parent.", Toast.LENGTH_SHORT).show();
            loadNAView(title);
            return;
        }

        loadPage(title, target);
    }

    private void loadNAView(String titleText) {
        FrameLayout container = findViewById(R.id.rightContent);
        container.removeAllViews();

        View page = LayoutInflater.from(this)
                .inflate(R.layout.provider_view_child_summary_na, container, false);

        TextView txt = page.findViewById(R.id.txtUnavailableTitle);
        txt.setText(getString(R.string.not_applicable, titleText));

        container.addView(page);
    }

    private void loadPage(String titleText, Class<?> targetActivity) {

        FrameLayout container = findViewById(R.id.rightContent);
        container.removeAllViews();

        View page = LayoutInflater.from(this)
                .inflate(R.layout.provider_view_child_summary_page, container, false);

        TextView title = page.findViewById(R.id.txtTitle);
        Button start = page.findViewById(R.id.btnStart);

        title.setText(titleText);

        start.setOnClickListener(v -> {
            Intent i = new Intent(this, targetActivity);
            i.putExtra("childID", childId);
            i.putExtra("providerMode", true);
            startActivity(i);
        });

        container.addView(page);
    }
}
