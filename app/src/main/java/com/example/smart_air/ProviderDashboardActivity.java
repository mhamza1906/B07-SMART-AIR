package com.example.smart_air;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.*;

import java.util.*;

public class ProviderDashboardActivity extends AppCompatActivity {

    private String providerUsername;
    private String providerID;
    private String providerEmail;
    private String providerLName;
    private String providerFName;
    private TextView txtAvatar;
    private FirebaseFirestore db;

    private ProviderChildAdapter adapter;
    private final List<ProviderChildItem> childList = new ArrayList<>();

    private static final long SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.provider_dashboard);

        txtAvatar = findViewById(R.id.txtUserAvatar);

        providerID = getIntent().getStringExtra("providerID");
        if (providerID == null) {
            Toast.makeText(this, "User ID missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        fetchProviderUsername();
        loadProviderInfoFromDatabase();

        ImageView imgNotification = findViewById(R.id.imgNotification);
        imgNotification.setOnClickListener(v -> openNotificationPage());


        RecyclerView rv = findViewById(R.id.recyclerProviderChildren);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProviderChildAdapter(childList, this::openChildSummary);
        rv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (providerUsername != null) {
            loadLinkedChildren();
        }
    }

    private void loadProviderInfoFromDatabase() {
        FirebaseDatabase.getInstance().getReference("users")
                .child(providerID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        providerEmail = snapshot.child("email").getValue(String.class);
                        providerFName = snapshot.child("fName").getValue(String.class);
                        providerLName = snapshot.child("lName").getValue(String.class);
                        providerUsername = snapshot.child("username").getValue(String.class);
                        setupUserAvatar();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void setupUserAvatar() {
        String initials = "U";
        if (providerFName != null && providerLName != null && !providerFName.isEmpty())
            initials = ("" + providerFName.charAt(0) + providerLName.charAt(0)).toUpperCase();
        txtAvatar.setText(initials);
        txtAvatar.setOnClickListener(v -> showUserInfoBottomSheet());
    }

    private void showUserInfoBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.bottomsheet_user_info, null);
        dialog.setContentView(view);
        String fullNameText = getString(R.string.display_full_name, providerFName != null ? providerFName : "N/A", providerLName != null ? providerLName : "N/A");
        ((TextView) view.findViewById(R.id.txtUserFullName)).setText(fullNameText);
        ((TextView) view.findViewById(R.id.txtUserEmail)).setText(getString(R.string.display_email, providerEmail));
        ((TextView) view.findViewById(R.id.txtUsername)).setText(getString(R.string.display_username, providerUsername));
        view.findViewById(R.id.btnSignOut).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivityView.class));
            finish();
        });
        dialog.show();
    }

    private void fetchProviderUsername() {
        String myUid = FirebaseAuth.getInstance().getUid();

        if (myUid == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show();
            FirebaseAuth.getInstance().signOut();
            finish();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(myUid);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                providerUsername = snap.child("username").getValue(String.class);
                if (providerUsername != null) loadLinkedChildren();
            }

            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void loadLinkedChildren() {
        db.collection("provider-child-index")
                .document(providerUsername)
                .collection("children")
                .whereEqualTo("availability", true)
                .get()
                .addOnSuccessListener(snap -> {
                    childList.clear();
                    for (DocumentSnapshot doc : snap) {
                        String childId = doc.getString("childId");
                        Timestamp ts = doc.getTimestamp("generated_at");
                        if (childId == null || ts == null) continue;

                        long now = System.currentTimeMillis();
                        long generated = ts.toDate().getTime();

                        if (now - generated > SEVEN_DAYS_MILLIS) {
                            expireInvite(childId);
                            continue;
                        }

                        checkChildProviderShare(childId);
                    }
                });
    }

    private void expireInvite(String childId) {
        db.collection("provider-child-index")
                .document(providerUsername)
                .collection("children")
                .document(childId)
                .update("availability", false);

        db.collection("child-provider-share")
                .document(childId)
                .collection("providers")
                .document(providerUsername)
                .update("availability", false);
    }

    private void checkChildProviderShare(String childId) {
        db.collection("child-provider-share")
                .document(childId)
                .collection("providers")
                .document(providerUsername)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    Boolean availability = doc.getBoolean("availability");
                    Boolean used = doc.getBoolean("used");
                    Boolean checked = doc.getBoolean("code_checked");

                    if (availability == null || !availability) return;

                    if (Boolean.TRUE.equals(used) && Boolean.TRUE.equals(checked)) {
                        fetchChildName(childId);
                    }
                });
    }

    private void fetchChildName(String childId) {
        DatabaseReference usersRef =
                FirebaseDatabase.getInstance().getReference("users").child(childId);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                String name = snap.child("username").getValue(String.class);
                if (name == null) name = childId;

                childList.add(new ProviderChildItem(childId, name, providerUsername));
                adapter.notifyDataSetChanged();
            }

            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void openNotificationPage() {
        Intent intent = new Intent(this, ProviderNotificationActivity.class);
        intent.putExtra("providerUsername", providerUsername);
        startActivity(intent);
    }

    private void openChildSummary(ProviderChildItem item) {
        db.collection("child-provider-share")
                .document(item.childId)
                .collection("providers")
                .document(providerUsername)
                .get()
                .addOnSuccessListener(doc -> {
                    Intent i = new Intent(this, ProviderViewChildSummaryActivity.class);
                    i.putExtra("childID", item.childId);
                    i.putExtra("providerUsername", providerUsername);

                    Object obj = doc.get("summary_visibility");

                    Map<String, Boolean> vis = null;
                    if (obj instanceof Map) {
                        vis = (Map<String, Boolean>) obj;
                    }


                    if (vis != null) {
                        for (String k : vis.keySet()) {
                            i.putExtra("summary_" + k, vis.get(k));
                        }
                    }
                    startActivity(i);
                });
    }
}
