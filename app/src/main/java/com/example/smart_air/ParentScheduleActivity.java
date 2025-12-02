package com.example.smart_air;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ParentScheduleActivity extends AppCompatActivity
        implements DatePickerDialog.OnDateSetListener {

    private FirebaseFirestore firestore;
    private TextView chosenDate;
    private Calendar calendar;
    private String childID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_schedule);


        childID = getIntent().getStringExtra("childID");

        if (childID == null || childID.isEmpty()) {
            Toast.makeText(this, "Child ID missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        calendar = Calendar.getInstance();
        firestore = FirebaseFirestore.getInstance();

        chosenDate = findViewById(R.id.ChosenDate);
        TextView controllerNumber = findViewById(R.id.controllerNumberText);
        Button pickDateButton = findViewById(R.id.pickDate);
        Button increment = findViewById(R.id.incrementButton);
        Button decrement = findViewById(R.id.decrementButton);
        Button setDateButton = findViewById(R.id.setDate);


        pickDateButton.setOnClickListener(v -> {
            DatePickerFragment newFragment = new DatePickerFragment();
            newFragment.show(getSupportFragmentManager(), "datePicker");
        });

        increment.setOnClickListener(v -> {
            int n = Integer.parseInt(controllerNumber.getText().toString());
            controllerNumber.setText(String.valueOf(n + 1));
        });

        decrement.setOnClickListener(v -> {
            int n = Integer.parseInt(controllerNumber.getText().toString());
            if (n > 1) controllerNumber.setText(String.valueOf(n - 1));
            else Toast.makeText(this, "Cannot set below 1", Toast.LENGTH_SHORT).show();
        });

        setDateButton.setOnClickListener(v -> saveSchedule(controllerNumber));
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {

        calendar.set(year, month, day);

        chosenDate.setText(DateFormat.getDateInstance(DateFormat.LONG).format(calendar.getTime()));
    }

    private void saveSchedule(TextView controllerNumberText) {


        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar selected = (Calendar) calendar.clone();
        selected.set(Calendar.HOUR_OF_DAY, 0);
        selected.set(Calendar.MINUTE, 0);
        selected.set(Calendar.SECOND, 0);
        selected.set(Calendar.MILLISECOND, 0);


        if (selected.before(today)) {
            Toast.makeText(this, "You cannot create schedule for the past!", Toast.LENGTH_LONG).show();
            return;
        }

        int year = selected.get(Calendar.YEAR);
        int month = selected.get(Calendar.MONTH) + 1;
        int day = selected.get(Calendar.DAY_OF_MONTH);

        String dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month, day);


        Map<String, Object> data = new HashMap<>();
        data.put("taken", false);
        data.put("controllerNumber", Integer.parseInt(controllerNumberText.getText().toString()));

        firestore.collection("planned-schedule")
                .document(childID)
                .collection("Schedules")
                .document(dateKey)
                .set(data)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Saved for " + dateKey, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}
