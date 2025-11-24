package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ParentDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.parent_dashboard);

        String parentId = getIntent().getStringExtra("parentID");

        if(parentId == null) {
            Toast.makeText(this,"User ID not found",Toast.LENGTH_SHORT).show();
            finish();
        }

        Button linkButton = (Button)findViewById(R.id.linkchildbutton);
        linkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent parentLink = new Intent(ParentDashboardActivity.this, LinkChildActivity.class);
                parentLink.putExtra("parentID",parentId);
                startActivity(parentLink);
            }
        });

        Button signoutButton = (Button)findViewById(R.id.signoutbutton);
        signoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent parentSignOut = new Intent(ParentDashboardActivity.this, ParentSignOutActivity.class);
                parentSignOut.putExtra("parentID",parentId);
                startActivity(parentSignOut);
            }
        });

        Button shareButton = (Button)findViewById(R.id.sharebutton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent parentShare = new Intent(ParentDashboardActivity.this, SharetoProviderActivity.class);
                parentShare.putExtra("parentID",parentId);
                startActivity(parentShare);
            }
        });

        Button alertButton = (Button)findViewById(R.id.alertbutton);
        alertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent parentAlert = new Intent(ParentDashboardActivity.this, ParentAlertActivity.class);
                parentAlert.putExtra("parentID",parentId);
                startActivity(parentAlert);
            }
        });

        Button addparentButton = (Button)findViewById(R.id.addparentbutton);
        addparentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent parentAdd = new Intent(ParentDashboardActivity.this, AddParentActivity.class);
                parentAdd.putExtra("parentID",parentId);
                startActivity(parentAdd);
            }
        });

        Button ScheduleButton = (Button)findViewById(R.id.schedulebutton);
        ScheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent parentSchedule = new Intent(ParentDashboardActivity.this, ParentScheduleActivity.class);
                parentSchedule.putExtra("parentID",parentId);
                startActivity(parentSchedule);
            }
        });
    }

}
