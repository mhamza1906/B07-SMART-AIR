package com.example.smart_air;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParentScheduleActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    private FirebaseFirestore firestore;
    TextView chosenDate;
    Calendar calendar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_schedule);
        calendar = Calendar.getInstance();

        String parentID = getIntent().getStringExtra("parentID");

        firestore = FirebaseFirestore.getInstance();
        Spinner childSpinner = (Spinner) findViewById(R.id.childspinner);
        CollectionReference childCollection = firestore.collection("parent-child").document(parentID).collection("child");

        List<String> children = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),R.layout.activity_parent_schedule,children);
        adapter.setDropDownViewResource(R.layout.activity_parent_schedule);
        childSpinner.setAdapter(adapter);

        childCollection.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String child = document.getString("fName")+" "+document.getString("lName");
                        children.add(child);
                    }
                    adapter.notifyDataSetChanged();
                }
            }
        });

        chosenDate = (TextView)findViewById(R.id.ChosenDate);
        Button pickDateButton = (Button)findViewById(R.id.pickDate);
        pickDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerFragment newFragment = new DatePickerFragment();
                newFragment.show(getSupportFragmentManager(),"datePicker");
            }
        });

        Button setDateButton = (Button)findViewById(R.id.setDate);
        setDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create document in "planned-schedule" collection, according to "childID" document with put()
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH)+1;
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                Map<String, Object> calData = new HashMap<>();
                // calData.put( data here );
                calData.put("date",Integer.toString(year)+"-"+Integer.toString(month)+"-"+Integer.toString(day));
                calData.put("taken",false);
            }
        });
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        calendar.set(Calendar.YEAR,year);
        calendar.set(Calendar.MONTH,month);
        calendar.set(Calendar.DAY_OF_MONTH,day);
        String selectedDate = DateFormat.getDateInstance(DateFormat.LONG).format(calendar.getTime());
        chosenDate.setText(selectedDate);
    }
}
