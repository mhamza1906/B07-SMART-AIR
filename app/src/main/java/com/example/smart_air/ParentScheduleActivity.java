package com.example.smart_air;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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

public class ParentScheduleActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, AdapterView.OnItemSelectedListener {

    private FirebaseFirestore firestore;
    TextView chosenDate;
    Calendar calendar;
    Spinner childSpinner;
    String selectedChildID;
    String parentID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_schedule);

        calendar = Calendar.getInstance();

        firestore = FirebaseFirestore.getInstance();

        String childID = getIntent().getStringExtra("childID");
        DatabaseReference childUserRef = FirebaseDatabase.getInstance().getReference("users").child(childID);

        childUserRef.child("parentID").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    setParentID(snapshot.getValue(String.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ParentScheduleActivity","Database error when finding ParentID: "+error.getMessage());
            }
        });

        childSpinner = (Spinner) findViewById(R.id.childspinner);
        CollectionReference childCollection = firestore.collection("parent-child").document(parentID).collection("child");

        List<String> children = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),R.layout.activity_parent_schedule,children);
        adapter.setDropDownViewResource(R.layout.activity_parent_schedule);
        childSpinner.setAdapter(adapter);
        childSpinner.setOnItemSelectedListener(this);

        childCollection.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {
                    int i = 0;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        if(i==0) {
                            selectedChildID = document.getString("childID");
                        }
                        i += 1;
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

        TextView controllerNumber = (TextView)findViewById(R.id.controllerNumberText);
        Button increment = (Button)findViewById(R.id.incrementButton);
        increment.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View v) {
                                             int counter = Integer.parseInt(controllerNumber.getText().toString());
                                             counter += 1;
                                             controllerNumber.setText(String.valueOf(counter));
                                         }
                                     });

        Button decrement = (Button) findViewById(R.id.decrementButton);
        decrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int counter = Integer.parseInt(controllerNumber.getText().toString());
                if(counter > 1) {
                    counter -= 1;
                } else {
                    Toast controllerToast = Toast.makeText(getApplicationContext(),"Cannot set controller number to less than 1",Toast.LENGTH_LONG);
                    controllerToast.show();
                }
                controllerNumber.setText(String.valueOf(counter));
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
                String controllerNum = controllerNumber.getText().toString();
                calData.put("taken",false);
                calData.put("controllerNumber",Integer.parseInt(controllerNum));

                // Create document/overwrite existing document for this date
                firestore.collection("planned-schedule").document(selectedChildID).collection("Schedules")
                        .document(Integer.toString(year)+"-"+Integer.toString(month)+"-"+Integer.toString(day)).set(calData);
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Map<String, Object> child = (Map<String, Object>) parent.getItemAtPosition(position);
        selectedChildID = (String) child.get("childID");
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void setParentID(String chosenID) {
        parentID = chosenID;
    }
}
