package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ProviderDashboardActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.provider_dashboard);

        Button providerSignOut = (Button)findViewById(R.id.providersignoutbutton);
        providerSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProviderDashboardActivity.this,ProviderSignOutActivity.class));
            }
        });

        Button providerNotif = (Button)findViewById(R.id.providernotifbutton);
        providerNotif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProviderDashboardActivity.this,ProviderNotifActivity.class));
            }
        });
    }
}
