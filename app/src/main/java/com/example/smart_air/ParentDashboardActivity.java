package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ParentDashboardActivity extends AppCompatActivity {

    private static final String TAG = "ParentDashboardActivity";

    // Firestore and UI Components
    private FirebaseFirestore db;
    private RecyclerView childSummariesRecyclerView;
    private ChildSummaryAdapter adapter;
    private List<ChildSummary> childSummaryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.parent_dashboard);

        String parentId = getIntent().getStringExtra("parentID");

        if (parentId == null || parentId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Setup RecyclerView
        setupRecyclerView();

        // Fetch data from Firestore
        fetchChildData(parentId);

        // --- Keep existing button listeners ---
        Button linkButton = findViewById(R.id.linkchildbutton);
        linkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent parentLink = new Intent(ParentDashboardActivity.this, ChildSignUpActivity.class);
                parentLink.putExtra("parentID", parentId);
                startActivity(parentLink);
            }
        });

        Button signoutButton = findViewById(R.id.signoutbutton);
        signoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent parentSignOut = new Intent(ParentDashboardActivity.this, ParentSignOutActivity.class);
                parentSignOut.putExtra("parentID", parentId);
                startActivity(parentSignOut);
            }
        });

        Button shareButton = findViewById(R.id.sharebutton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent parentShare = new Intent(ParentDashboardActivity.this, SharetoProviderActivity.class);
                parentShare.putExtra("parentID", parentId);
                startActivity(parentShare);
            }
        });

        Button alertButton = findViewById(R.id.alertbutton);
        alertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent parentAlert = new Intent(ParentDashboardActivity.this, ParentAlertActivity.class);
                parentAlert.putExtra("parentID", parentId);
                startActivity(parentAlert);
            }
        });
    }

    private void setupRecyclerView() {
        childSummariesRecyclerView = findViewById(R.id.ChildSummaries);
        childSummaryList = new ArrayList<>();
        adapter = new ChildSummaryAdapter(childSummaryList);
        childSummariesRecyclerView.setAdapter(adapter);
        childSummariesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void fetchChildData(String parentId) {
        db.collection("parent-child").document(parentId).collection("child")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        childSummaryList.clear(); // Clear old data before adding new
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String childId = document.getId();
                            String fname = document.getString("fname");
                            String lname = document.getString("lname");
                            String birthday = document.getString("birthday");

                            // Build the full name safely
                            List<String> nameParts = new ArrayList<>();
                            if (fname != null && !fname.isEmpty()) {
                                nameParts.add(fname);
                            }
                            if (lname != null && !lname.isEmpty()) {
                                nameParts.add(lname);
                            }
                            String fullName = String.join(" ", nameParts);

                            // For each child, fetch their zone data for today
                            fetchZoneForChild(childId, fullName, birthday);
                        }
                    } else {
                        Log.w(TAG, "Error getting child documents: ", task.getException());
                        Toast.makeText(ParentDashboardActivity.this, "Failed to load child data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchZoneForChild(String childId, String fullName, String birthday) {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("PEF").document(childId).collection("log").document(todayDate)
                .get()
                .addOnCompleteListener(task -> {
                    String todayZoneColor = "#808080"; // Default to Gray

                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        String zone = task.getResult().getString("zone");
                        if (zone != null) {
                            switch (zone) {
                                case "Green":
                                    todayZoneColor = "#4CAF50";
                                    break;
                                case "Yellow":
                                    todayZoneColor = "#FFEB3B";
                                    break;
                                case "Red":
                                    todayZoneColor = "#F44336";
                                    break;
                            }
                        }
                    } else {
                        Log.d(TAG, "No PEF log found for today for child: " + childId);
                    }

                    // Using filler data for rescue and graph info as requested
                    String lastRescueTime = "N/A";
                    int weeklyRescueCount = 0;
                    List<Float> graphData = new ArrayList<>(Arrays.asList(450f, 460f, 455f, 470f, 465f, 480f, 475f));

                    ChildSummary summary = new ChildSummary(
                            fullName,
                            todayZoneColor,
                            lastRescueTime,
                            weeklyRescueCount,
                            birthday,
                            graphData
                    );
                    childSummaryList.add(summary);
                    adapter.notifyDataSetChanged(); // Refresh the RecyclerView
                });
    }
}
