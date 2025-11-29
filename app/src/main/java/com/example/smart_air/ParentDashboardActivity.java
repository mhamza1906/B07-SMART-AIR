package com.example.smart_air;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ParentDashboardActivity extends AppCompatActivity {

    private String parentId;
    private String parentEmail;
    private String parentFName;
    private String parentLName;
    private String parentUsername;
    private TextView txtAvatar;
    private List<String> cachedChildIds = new ArrayList<>();
    private List<String> cachedChildNames = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.parent_dashboard);

        txtAvatar = findViewById(R.id.txtUserAvatar);

        parentId = getIntent().getStringExtra("parentID");

        if (parentId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadFragmentMyChildren();

        setupBottomNavigation();

        loadParentInfoFromDatabase(); // query information from Realtime Database

    }

    // query information from Realtime Database
    private void loadParentInfoFromDatabase() {
        FirebaseDatabase.getInstance().getReference("users")
                .child(parentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            Toast.makeText(ParentDashboardActivity.this,
                                    "User data not found",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        parentEmail = snapshot.child("email").getValue(String.class);
                        parentFName = snapshot.child("fName").getValue(String.class);
                        parentLName = snapshot.child("lName").getValue(String.class);
                        parentUsername = snapshot.child("username").getValue(String.class);

                        if (parentEmail == null) parentEmail = "";
                        if (parentFName == null) parentFName = "";
                        if (parentLName == null) parentLName = "";
                        if (parentUsername == null) parentUsername = "";

                        setupUserAvatar();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ParentDashboardActivity.this,
                                "Database error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Set User Profile
    private void setupUserAvatar() {
        // deal with empty name
        if (parentFName.isEmpty() || parentLName.isEmpty()) {
            txtAvatar.setText("U");
        } else {
            String initials = ("" +
                    parentFName.charAt(0) +
                    parentLName.charAt(0)).toUpperCase();
            txtAvatar.setText(initials);
        }

        // popup: show user information
        txtAvatar.setOnClickListener(v -> showUserInfoBottomSheet());
    }


    // popup: show user information
    private void showUserInfoBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.bottomsheet_user_info, null, false);

        dialog.setContentView(view);

        TextView txtFullName = view.findViewById(R.id.txtUserFullName);
        TextView txtEmail = view.findViewById(R.id.txtUserEmail);
        TextView txtUsername = view.findViewById(R.id.txtUsername);
        TextView txtFName = view.findViewById(R.id.txtFirstName);
        TextView txtLName = view.findViewById(R.id.txtLastName);
        Button btnSignOut = view.findViewById(R.id.btnSignOut);

        String fullName = (parentFName + " " + parentLName).trim();

        txtFullName.setText(fullName.isEmpty() ? "Unknown User" : fullName);
        txtEmail.setText(getString(R.string.display_email, parentEmail));
        txtUsername.setText(getString(R.string.display_username, parentUsername));
        txtFName.setText(getString(R.string.display_fName, parentFName));
        txtLName.setText(getString(R.string.display_lName, parentLName));


        btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            dialog.dismiss();

            Intent intent = new Intent(ParentDashboardActivity.this, LoginActivityView.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });



        dialog.show();
    }

    private void setupBottomNavigation() {

        findViewById(R.id.tabMyChildren).setOnClickListener(v -> loadFragmentMyChildren());

        findViewById(R.id.tabShare).setOnClickListener(v -> {
            FrameLayout container = findViewById(R.id.containerParentDashboard);
            container.removeAllViews();
            TextView text = new TextView(this);
            text.setText(R.string.dummy_share_page);
            text.setTextSize(20);
            container.addView(text);
        });
    }


    private void loadFragmentMyChildren() {
        @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.children_profile, null, false);
        FrameLayout container = findViewById(R.id.containerParentDashboard);
        Button btnSelectChild = view.findViewById(R.id.btnSelectChild);
        btnSelectChild.setOnClickListener(v -> showChildSelector());
        container.removeAllViews();
        container.addView(view);

        setupChildrenProfiles(view);
    }

    private void showChildSelector() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.bottomsheet_child_selector, null, false);
        dialog.setContentView(view);

        RecyclerView recycler = view.findViewById(R.id.recyclerChildSelector);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        recycler.setAdapter(new ChildrenAdapter(
                ParentDashboardActivity.this,
                cachedChildIds,
                cachedChildNames
        ));

        dialog.show();
    }

    private void setupChildrenProfiles(View root) {

        RecyclerView recycler = root.findViewById(R.id.recyclerChildren);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        List<String> childNames = new ArrayList<>();
        List<String> childIds = new ArrayList<>();


        FirebaseFirestore.getInstance()
                .collection("parent-child")
                .document(parentId)
                .collection("child")
                .get()
                .addOnSuccessListener(snap -> {

                    for (DocumentSnapshot doc : snap) {
                        String username = doc.getString("username");
                        childNames.add(username);
                        childIds.add(doc.getId());
                    }

                    cachedChildIds = childIds;
                    cachedChildNames = childNames;

                    recycler.setAdapter(new ChildrenAdapter(ParentDashboardActivity.this, childIds, childNames));
                });
        Button btnSelectChild = root.findViewById(R.id.btnSelectChild);
        btnSelectChild.setOnClickListener(v -> showChildSelector());

        Button btnCreateChild = root.findViewById(R.id.btnCreateChild);

        btnCreateChild.setOnClickListener(v -> {
            Intent intent = new Intent(ParentDashboardActivity.this, ChildSignUpActivity.class);
            intent.putExtra("parentID", parentId);
            startActivity(intent);
        });
    }

    public void showChildOptions(String childId) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        TextView manage = buildOption("Manage Child Activities");
        TextView summary = buildOption("View Child Profile Summary");
        TextView dashboard = buildOption("Navigate To Child Dashboard");

        layout.addView(manage);
        layout.addView(summary);
        layout.addView(dashboard);

        dialog.setContentView(layout);

        manage.setOnClickListener(v -> {
            Intent i = new Intent(this, ManageChildActivity.class);
            i.putExtra("childID", childId);
            startActivity(i);
        });

        summary.setOnClickListener(v -> {
            Intent i = new Intent(this, ChildSummaryActivity.class);
            i.putExtra("childID", childId);
            startActivity(i);
        });

        dashboard.setOnClickListener(v -> {
            Intent i = new Intent(this, ChildDashboardActivity.class);
            i.putExtra("childID", childId);
            startActivity(i);
        });

        dialog.show();
    }

    private TextView buildOption(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(18);
        t.setPadding(16, 32, 16, 32);
        return t;
    }



}


