package com.example.smart_air;

import android.os.Bundle;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChildScheduleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_schedule);

        FirebaseFirestore firebase = FirebaseFirestore.getInstance();
        String childID = getIntent().getStringExtra("childID");

        if (childID == null || childID.isEmpty()) {
            Toast.makeText(this, "Child ID missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        RecyclerView scheduleRecycler = findViewById(R.id.scheduleRecycler);
        scheduleRecycler.setLayoutManager(new LinearLayoutManager(this));

        firebase.collection("planned-schedule")
                .document(childID)
                .collection("Schedules")
                .orderBy(FieldPath.documentId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        List<String> dates = new ArrayList<>();
                        List<String> controllers = new ArrayList<>();
                        List<String> takenList = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {


                            String date = document.getId();


                            Long controllerNumLong = document.getLong("controllerNumber");
                            int controllerNum = controllerNumLong != null ? controllerNumLong.intValue() : 0;

                            Boolean taken = document.getBoolean("taken");
                            boolean takenVal = taken != null && taken;

                            dates.add(date);
                            controllers.add(String.valueOf(controllerNum));
                            takenList.add(takenVal ? "Yes" : "No");
                        }

                        ScheduleAdapter scheduleAdapter =
                                new ScheduleAdapter(dates, controllers, takenList);
                        scheduleRecycler.setAdapter(scheduleAdapter);
                    }
                });
    }
}

