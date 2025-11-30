package com.example.smart_air;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class TechniqueHelperActivity extends AppCompatActivity {

    private String medType;
    private String childID;
    private String date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_container);

        childID = getIntent().getStringExtra("childID");
        medType = getIntent().getStringExtra("type");
        date = getIntent().getStringExtra("date");

        if (savedInstanceState == null) {
            loadFragment(new Step1Fragment());
        }
    }

    public String getChildID() { return childID; }
    public String getMedType() { return medType; }
    public String getDate() { return date; }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainerView, fragment)
                .commit();
    }

    public void finishHelper() {
        finish();
    }
}
