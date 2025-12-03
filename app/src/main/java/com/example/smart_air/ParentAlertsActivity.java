package com.example.smart_air;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ParentAlertsActivity extends AppCompatActivity {

    private static final String TAG = "ParentAlertsActivity";

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private RecyclerView alertsRecyclerView;
    private AlertAdapter alertAdapter;
    private List<Alert> alertList;
    private TextView noAlertsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parents_alerts);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        alertsRecyclerView = findViewById(R.id.alertsRecyclerView);
        noAlertsTextView = findViewById(R.id.noAlertsTextView);

        alertList = new ArrayList<>();
        alertAdapter = new AlertAdapter(alertList);

        alertsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        alertsRecyclerView.setAdapter(alertAdapter);


        fetchParentAlerts();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchParentAlerts() {
        String parentId = currentUser.getUid();

        db.collection("parent_alerts")
                .whereEqualTo("parentID", parentId)
//                .orderBy("timestamp", Query.Direction.DESCENDING) // Show newest alerts first
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        noAlertsTextView.setVisibility(View.VISIBLE);
                        alertsRecyclerView.setVisibility(View.GONE);
                        Log.d(TAG, "No alerts found for parent: " + parentId);
                    } else {
                        noAlertsTextView.setVisibility(View.GONE);
                        alertsRecyclerView.setVisibility(View.VISIBLE);

                        alertList.clear(); // Clear old list before adding new data
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Alert alert = document.toObject(Alert.class);
                            alertList.add(alert);
                        }
                        alertAdapter.notifyDataSetChanged(); // Refresh the list
                        Log.d(TAG, "Successfully loaded " + alertList.size() + " alerts.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching alerts: ", e);
                    noAlertsTextView.setText(R.string.fail_load_alerts);
                    noAlertsTextView.setVisibility(View.VISIBLE);
                });
    }
}
