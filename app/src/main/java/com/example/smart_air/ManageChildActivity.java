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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class ManageChildActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_child);

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


        loadPage(R.layout.manage_child_page, ConfigurePBActivity.class, "Configure Personal Best");
    }

    private void handleMenuClick(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.navPB) {
            loadPage(R.layout.manage_child_page,
                    ConfigurePBActivity.class,
                    "Configure Personal Best");

        } else if (id == R.id.navSchedule) {
            loadPage(R.layout.manage_child_page,
                    CreateScheduleActivity.class,
                    "Create Schedule");

        } else if (id == R.id.navInventory) {
            loadPage(R.layout.manage_child_page,
                    ManageInventoryActivity.class,
                    "Manage Inventory");

        } else if (id == R.id.navRewards) {
            loadPage(R.layout.manage_child_page,
                    ConfigureRewardsActivity.class,
                    "Configure Rewards");
        }
    }


    /**
     * 将右侧内容替换为一个简单页面，并让 Start 按钮跳转到对应 Activity
     */
    private void loadPage(int layoutResId, Class<?> targetActivity, String titleText) {

        View container = findViewById(R.id.rightContent);
        ((android.widget.FrameLayout) container).removeAllViews();

        View page = LayoutInflater.from(this).inflate(layoutResId, null);

        TextView title = page.findViewById(R.id.txtTitle);
        Button start = page.findViewById(R.id.btnStart);

        title.setText(titleText);

        start.setOnClickListener(v -> {
            Intent intent = new Intent(this, targetActivity);
            startActivity(intent);
        });

        ((android.widget.FrameLayout) container).addView(page);
    }

}
