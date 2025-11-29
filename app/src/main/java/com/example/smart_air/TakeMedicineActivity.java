package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;



public class TakeMedicineActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent childTakeMed = new Intent(TakeMedicineActivity.this, TechniqueHelperActivity.class);
        startActivity(childTakeMed);



    }
}
