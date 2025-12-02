package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChildScheduleActivity extends AppCompatActivity {
    private FirebaseFirestore firebase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_schedule);

        firebase = FirebaseFirestore.getInstance();
        String childID = getIntent().getStringExtra("childID");

        RecyclerView scheduleRecycler = (RecyclerView)findViewById(R.id.scheduleRecycler);
        scheduleRecycler.setLayoutManager(new LinearLayoutManager(this));

        firebase.collection("planned-schedule").document(childID).collection("Schedules").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()) {
                            List<String> dates = new ArrayList<>();
                            List<String> controllers = new ArrayList<>();
                            List<String> takenList = new ArrayList<>();
                            for(QueryDocumentSnapshot document : task.getResult()) {
                                String date = document.getId();
                                int controllerNum = (int) document.get("controllerNum");
                                boolean taken = (boolean) document.get("taken");
                                dates.add(date);
                                controllers.add(Integer.toString(controllerNum));
                                if(taken) {
                                    takenList.add("Yes");
                                } else {
                                    takenList.add("No");
                                }
                            }
                            ScheduleAdapter schedule = new ScheduleAdapter(dates,controllers,takenList);
                            scheduleRecycler.setAdapter(schedule);
                        }
                    }
                });
    }
}
