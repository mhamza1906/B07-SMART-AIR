package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProviderDashboardActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.provider_dashboard);

        String providerId = getIntent().getStringExtra("providerID");

        if(providerId == null) {
            Toast.makeText(this,"User ID not found",Toast.LENGTH_SHORT).show();
            finish();
        }

        Button providerSignOut = (Button)findViewById(R.id.providersignoutbutton);
        providerSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent providerSignOut = new Intent(ProviderDashboardActivity.this, ProviderSignOutActivity.class);
                providerSignOut.putExtra("providerID",providerId);
                startActivity(providerSignOut);
            }
        });

        Button providerNotif = (Button)findViewById(R.id.providernotifbutton);
        providerNotif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent providerNotif = new Intent(ProviderDashboardActivity.this, ProviderNotifActivity.class);
                providerNotif.putExtra("providerID",providerId);
                startActivity(providerNotif);
            }
        });
    }
}
