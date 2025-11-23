package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ParentDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.parent_dashboard);

        Button linkButton = (Button)findViewById(R.id.linkchildbutton);
        linkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ParentDashboardActivity.this, LinkChildActivity.class));
            }
        });

        Button signoutButton = (Button)findViewById(R.id.signoutbutton);
        signoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ParentDashboardActivity.this, ParentSignOutActivity.class));
            }
        });

        Button shareButton = (Button)findViewById(R.id.sharebutton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ParentDashboardActivity.this, SharetoProviderActivity.class));
            }
        });

        Button alertButton = (Button)findViewById(R.id.alertbutton);
        alertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ParentDashboardActivity.this, ParentAlertActivity.class));
            }
        });

        Button addparentButton = (Button)findViewById(R.id.addparentbutton);
        addparentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ParentDashboardActivity.this, AddParentActivity.class));
            }
        });

        Button ScheduleButton = (Button)findViewById(R.id.schedulebutton);
        ScheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ParentDashboardActivity.this, ParentScheduleActivity.class));
            }
        });
    }

}
