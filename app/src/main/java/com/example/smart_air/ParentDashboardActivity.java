package com.example.smart_air;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ParentDashboardActivity extends AppCompatActivity {

    private static final String TAG = "ParentDashboardActivity";


    private String parentId;
    private String parentEmail;
    private String parentFName;
    private String parentLName;
    private String parentUsername;
    private TextView txtAvatar;
    private final List<String> cachedChildIds = new ArrayList<>();
    private final List<String> cachedChildNames = new ArrayList<>();

    // RecyclerView fields
    private FirebaseFirestore db;
    private RecyclerView childSummariesRecyclerView;
    private ChildSummaryAdapter adapter;
    private List<ChildSummary> childSummaryList;
    private TextView shareContentPlaceholder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.parent_dashboard);


        txtAvatar = findViewById(R.id.txtUserAvatar);
        parentId = getIntent().getStringExtra("parentID");
        if (parentId == null) {
            Toast.makeText(this, "User ID missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        shareContentPlaceholder = findViewById(R.id.share_content_placeholder);
        childSummariesRecyclerView = findViewById(R.id.ChildSummaries);


        db = FirebaseFirestore.getInstance();
        setupRecyclerView();
        fetchChildData(parentId);


        loadParentInfoFromDatabase();
        loadChildrenForSelector();
        setupBottomNavigation();


        Button btnSelectChild = findViewById(R.id.btnSelectChild);
        btnSelectChild.setOnClickListener(v -> showChildSelectorBottomSheet());

        Button btnCreateChild = findViewById(R.id.btnCreateChild);
        btnCreateChild.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChildSignUpActivity.class);
            intent.putExtra("parentID", parentId);
            startActivity(intent);
        });

        findViewById(R.id.tabMyChildren).performClick();

    }

    private void setupRecyclerView() {
        childSummaryList = new ArrayList<>();
        adapter = new ChildSummaryAdapter(childSummaryList);
        childSummariesRecyclerView.setAdapter(adapter);
        childSummariesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }


    @SuppressLint("NotifyDataSetChanged")
    private void fetchChildData(String parentId) {
        db.collection("parent-child").document(parentId).collection("child")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        childSummaryList.clear();
                        if (task.getResult().isEmpty()) {
                            adapter.notifyDataSetChanged();
                            return;
                        }
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String childId = document.getId();
                            String fname = document.getString("fName");
                            String lname = document.getString("lName");
                            String birthday = document.getString("birthday");

                            List<String> nameParts = new ArrayList<>();
                            if (fname != null && !fname.isEmpty()) nameParts.add(fname);
                            if (lname != null && !lname.isEmpty()) nameParts.add(lname);
                            String fullName = String.join(" ", nameParts);

                            fetchChildDetails(childId, fullName, birthday);
                        }
                    } else {
                        Log.w(TAG, "Error getting child documents: ", task.getException());
                        Toast.makeText(ParentDashboardActivity.this, "Failed to load child data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchChildDetails(String childId, String fullName, String birthday) {

        db.collection("medlog").document(childId).get().addOnCompleteListener(medLogTask -> {
            String lastRescueTime = "N/A";
            int weeklyRescueCount = 0;

            if (medLogTask.isSuccessful() && medLogTask.getResult() != null && medLogTask.getResult().exists()) {
                DocumentSnapshot medLogDoc = medLogTask.getResult();

                // Format the last rescue time string for display
                String lastUseTimestamp = medLogDoc.getString("last_rescue_use");
                if (lastUseTimestamp != null && !lastUseTimestamp.isEmpty()) {
                    String[] parts = lastUseTimestamp.split(" ");
                    if (parts.length >= 2) {
                        String datePart = parts[0];
                        String timePart = parts[1];
                        // Remove seconds for a cleaner look
                        if (timePart.lastIndexOf(':') > 0) {
                            timePart = timePart.substring(0, timePart.lastIndexOf(':'));
                        }
                        lastRescueTime = "Date: " + datePart + "  Time: " + timePart;
                    } else {
                        lastRescueTime = lastUseTimestamp; // Fallback to original string
                    }
                } else {
                    lastRescueTime = "N/A";
                }

                //updating weekly rescue use
                String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                updateWeeklyRescueUsage(childId, todayDate);

                Long weeklyCount = medLogDoc.getLong("rescue_use_in_last_7_days");
                if (weeklyCount != null) weeklyRescueCount = weeklyCount.intValue();
            }

            final String finalLastRescueTime = lastRescueTime;
            final int finalWeeklyRescueCount = weeklyRescueCount;

            // Get graph display preference
            db.collection("PEF").document(childId).get().addOnCompleteListener(prefTask -> {
                int durationDays = 7; // Default to 7 days
                if (prefTask.isSuccessful() && prefTask.getResult() != null && prefTask.getResult().exists()) {
                    Long duration = prefTask.getResult().getLong("graph_day_range");
                    if (duration != null) {
                        durationDays = duration.intValue();
                    }
                }

                List<String> dateList = getLastNDays(durationDays);


                db.collection("PEF").document(childId).collection("log")
                        .whereIn(FieldPath.documentId(), dateList)
                        .get().addOnCompleteListener(graphDataTask -> {

                            Map<String, Float> dailyPercents = new HashMap<>();
                            if (graphDataTask.isSuccessful()) {
                                for (QueryDocumentSnapshot logDoc : graphDataTask.getResult()) {
                                    Double percent = logDoc.getDouble("percent");
                                    if (percent != null) {
                                        dailyPercents.put(logDoc.getId(), percent.floatValue());
                                    }
                                }
                            }

                            List<Float> graphData = new ArrayList<>();
                            for (String date : dateList) {
                                graphData.add(dailyPercents.getOrDefault(date, 0f)); //Add 0 if no data for a day
                            }
                            Collections.reverse(graphData); //Ensure oldest to newest


                            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                            db.collection("PEF").document(childId).collection("log").document(todayDate)
                                    .get()
                                    .addOnCompleteListener(pefTask -> {
                                        String todayZoneColor = "#808080"; // Default to Gray
                                        if (pefTask.isSuccessful() && pefTask.getResult() != null && pefTask.getResult().exists()) {
                                            String zone = pefTask.getResult().getString("zone");
                                            if (zone != null) {
                                                switch (zone.toLowerCase()) {
                                                    case "green": todayZoneColor = "#4CAF50"; break;
                                                    case "yellow": todayZoneColor = "#FFEB3B"; break;
                                                    case "red": todayZoneColor = "#F44336"; break;
                                                }
                                            }
                                        }


                                        ChildSummary summary = new ChildSummary(fullName, todayZoneColor, finalLastRescueTime, finalWeeklyRescueCount, birthday, graphData);
                                        childSummaryList.add(summary);
                                        adapter.notifyDataSetChanged();
                                    });
                        });
            });
        });
    }

    private List<String> getLastNDays(int n) {
        List<String> list = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < n; i++) {
            list.add(sdf.format(cal.getTime()));
            cal.add(Calendar.DATE, -1);
        }
        return list;
    }

    private void updateWeeklyRescueUsage(String childID, String todayDate) {
        List<String> last7Days = getLast7Days(todayDate);
        CollectionReference logRef = db.collection("medlog").document(childID).collection("log");

        logRef.get().addOnSuccessListener(snap -> {
            int count = 0;
            for (DocumentSnapshot d : snap.getDocuments()) {
                String date = d.getId();
                if (last7Days.contains(date)) {
                    Map<String, Object> data = d.getData();
                    if (data != null && data.containsKey("rescue")) {
                        count++;
                    }
                }
            }

            DocumentReference parentDoc = db.collection("medlog").document(childID);
            Map<String, Object> update = new HashMap<>();
            update.put("rescue_use_in_last_7_days", count);
            parentDoc.set(update, SetOptions.merge());
        });
    }

    private List<String> getLast7Days(String today) {
        List<String> list = new ArrayList<>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(today);
            if (date == null) return list;

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            for (int i = 0; i < 7; i++) {
                String d = sdf.format(cal.getTime());
                list.add(d);
                cal.add(Calendar.DATE, -1);
            }
        } catch (Exception ignored) {}
        return list;
    }


    private void setupBottomNavigation() {
        View myChildrenTab = findViewById(R.id.tabMyChildren);
        View shareTab = findViewById(R.id.tabShare);

        myChildrenTab.setOnClickListener(v -> {
            childSummariesRecyclerView.setVisibility(View.VISIBLE);
            shareContentPlaceholder.setVisibility(View.GONE);
            findViewById(R.id.btnSelectChild).setVisibility(View.VISIBLE);
            findViewById(R.id.btnCreateChild).setVisibility(View.VISIBLE);


        });

        shareTab.setOnClickListener(v -> {
            childSummariesRecyclerView.setVisibility(View.GONE);
            shareContentPlaceholder.setVisibility(View.GONE);
            findViewById(R.id.btnSelectChild).setVisibility(View.GONE);
            findViewById(R.id.btnCreateChild).setVisibility(View.GONE);
        });

    }



    private void loadParentInfoFromDatabase() {
        FirebaseDatabase.getInstance().getReference("users")
                .child(parentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        parentEmail = snapshot.child("email").getValue(String.class);
                        parentFName = snapshot.child("fName").getValue(String.class);
                        parentLName = snapshot.child("lName").getValue(String.class);
                        parentUsername = snapshot.child("username").getValue(String.class);
                        setupUserAvatar();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void setupUserAvatar() {
        String initials = "U";
        if (parentFName != null && parentLName != null && !parentFName.isEmpty())
            initials = ("" + parentFName.charAt(0) + parentLName.charAt(0)).toUpperCase();
        txtAvatar.setText(initials);
        txtAvatar.setOnClickListener(v -> showUserInfoBottomSheet());
    }

    private void showUserInfoBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.bottomsheet_user_info, null);
        dialog.setContentView(view);
        String fullNameText = getString(R.string.display_full_name, parentFName != null ? parentFName : "N/A", parentLName != null ? parentLName : "N/A");
        ((TextView) view.findViewById(R.id.txtUserFullName)).setText(fullNameText);
        ((TextView) view.findViewById(R.id.txtUserEmail)).setText(getString(R.string.display_email, parentEmail));
        ((TextView) view.findViewById(R.id.txtUsername)).setText(getString(R.string.display_username, parentUsername));
        view.findViewById(R.id.btnSignOut).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivityView.class));
            finish();
        });
        dialog.show();
    }

    private void loadChildrenForSelector() {
        FirebaseFirestore.getInstance()
                .collection("parent-child")
                .document(parentId)
                .collection("child")
                .get()
                .addOnSuccessListener(snap -> {
                    cachedChildIds.clear();
                    cachedChildNames.clear();
                    for (DocumentSnapshot doc : snap) {
                        cachedChildIds.add(doc.getId());
                        cachedChildNames.add(doc.getString("username"));
                    }
                });
    }

    private void showChildSelectorBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        @SuppressLint("InflateParams")
        View sheetView = getLayoutInflater().inflate(R.layout.bottomsheet_child_selector, null);
        RecyclerView recycler = sheetView.findViewById(R.id.recyclerChildSelector);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(new ChildrenAdapter(cachedChildIds, cachedChildNames, childId -> {
            dialog.dismiss();
            showChildOptions(childId);
        }));
        dialog.setContentView(sheetView);
        dialog.show();
    }

    public void showChildOptions(String childId) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        TextView manage = buildOption("Manage Child Activities 〉");
        TextView summary = buildOption("View Child Summary       〉");
        TextView dashboard = buildOption("Go To Child Dashboard    〉");
        layout.addView(manage);
        layout.addView(summary);
        layout.addView(dashboard);
        dialog.setContentView(layout);
        manage.setOnClickListener(v -> {
            Intent it = new Intent(this, ManageChildActivity.class);
            it.putExtra("childID", childId);
            startActivity(it);
            dialog.dismiss();
        });
        summary.setOnClickListener(v -> {
            Intent it = new Intent(this, ViewChildSummaryActivity.class);
            it.putExtra("childID", childId);
            startActivity(it);
            dialog.dismiss();
        });
        dashboard.setOnClickListener(v -> {
            Intent it = new Intent(this, ChildDashboardActivity.class);
            it.putExtra("childID", childId);
            startActivity(it);
            dialog.dismiss();
        });
        dialog.show();
    }

    private TextView buildOption(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(18);
        tv.setPadding(16, 32, 16, 32);
        return tv;
    }
}
