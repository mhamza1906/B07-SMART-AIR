package com.example.smart_air;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class ManageChildActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private String childID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_child);

        childID = getIntent().getStringExtra("childID");

        if (childID == null || childID.isEmpty()) {
            Toast.makeText(this,"User ID not found",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navView = findViewById(R.id.navView);
        navView.setItemIconTintList(null);
        navView.setItemTextColor(ColorStateList.valueOf(Color.BLACK));


        findViewById(R.id.btnMenu).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );


        navView.setNavigationItemSelectedListener(item -> {
            handleMenuClick(item);
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });


        loadPage(R.layout.manage_child_page, ConfigurePEFSettingsActivity.class,
                "Press START to configure Personal Best (PB) for this child.");
    }

    private void handleMenuClick(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.navPB) {
            loadPage(R.layout.manage_child_page,
                    ConfigurePEFSettingsActivity.class,
                    "Press START to configure PEF Settings for this child.");

        } else if (id == R.id.navSchedule) {
            loadPage(R.layout.manage_child_page,
                    CreateScheduleActivity.class,
                    "Press START to create planned controller schedule for this child.");

        } else if (id == R.id.navInventory) {
            loadPage(R.layout.manage_child_page,
                    ManageInventoryActivity.class,
                    "Press START to manage medicine inventory for this child.");

        } else if (id == R.id.navRewards) {
            loadPage(R.layout.manage_child_page,
                    ConfigureRewardsActivity.class,
                    "Press START to configure badges condition for this child.");
        }else if(id == R.id.navDailyCheckin){
            loadPage(R.layout.manage_child_page,
                    ParentDailyCheckin.class,
                    "Press START to perform daily check-in for this child.");
        }

    }

    private void loadPage(int layoutResId, Class<?> targetActivity, String titleText) {

        View container = findViewById(R.id.rightContent);
        ((android.widget.FrameLayout) container).removeAllViews();

        View page = LayoutInflater.from(this).inflate(layoutResId, null);

        TextView title = page.findViewById(R.id.txtTitle);
        Button start = page.findViewById(R.id.btnStart);

        title.setText(titleText);

        start.setOnClickListener(v -> {
            Intent intent = new Intent(this, targetActivity);
            intent.putExtra("childID", childID);
            startActivity(intent);
        });

        ((android.widget.FrameLayout) container).addView(page);
    }

}
