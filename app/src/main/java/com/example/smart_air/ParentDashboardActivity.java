package com.example.smart_air;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
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

    private final List<String> cachedChildIds = new ArrayList<>();
    private final List<String> cachedChildNames = new ArrayList<>();

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
        android.view.View view = getLayoutInflater().inflate(R.layout.bottomsheet_user_info, null);

        dialog.setContentView(view);

        String fullNameText = getString(
                R.string.display_full_name,
                parentFName != null ? parentFName : "N/A",
                parentLName != null ? parentLName : "N/A"
        );

        ((TextView) view.findViewById(R.id.txtUserFullName))
                .setText(fullNameText);

        ((TextView) view.findViewById(R.id.txtUserEmail))
                .setText(getString(R.string.display_email, parentEmail));

        ((TextView) view.findViewById(R.id.txtUsername))
                .setText(getString(R.string.display_username, parentUsername));

        view.findViewById(R.id.btnSignOut).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivityView.class));
            finish();
        });

        dialog.show();
    }


    private void setupBottomNavigation() {

        LinearLayout tabMyChildren = findViewById(R.id.tabMyChildren);
        LinearLayout tabShare = findViewById(R.id.tabShare);

        Button btnSelectChild = findViewById(R.id.btnSelectChild);
        Button btnCreateChild = findViewById(R.id.btnCreateChild);

        FrameLayout container = findViewById(R.id.containerParentDashboard);


        setTabSelected(true);

        tabMyChildren.setOnClickListener(v -> {
            setTabSelected(true);


            btnSelectChild.setVisibility(View.VISIBLE);
            btnCreateChild.setVisibility(View.VISIBLE);

            container.removeAllViews();
            // TODO: Finish Share
        });

        tabShare.setOnClickListener(v -> {
            setTabSelected(false);


            btnSelectChild.setVisibility(View.GONE);
            btnCreateChild.setVisibility(View.GONE);

            container.removeAllViews();


            View shareView = getLayoutInflater().inflate(R.layout.parent_dashboard_share_page, container, false);
            container.addView(shareView);
        });
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
        android.view.View sheetView = getLayoutInflater()
                .inflate(R.layout.bottomsheet_child_selector, null);

        RecyclerView recycler = sheetView.findViewById(R.id.recyclerChildSelector);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        recycler.setAdapter(new ChildrenAdapter(
                cachedChildIds,
                cachedChildNames,
                childId -> {
                    dialog.dismiss();
                    showChildOptions(childId);
                }
        ));

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
            Intent it = new Intent(this, ChildSummaryActivity.class);
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

    private void setTabSelected(boolean isMyChildren) {

        TextView myChildrenLabel = findViewById(R.id.tabMyChildrenLabel);
        ImageView myChildrenIcon = findViewById(R.id.tabMyChildrenIcon);

        TextView shareLabel = findViewById(R.id.tabShareLabel);
        ImageView shareIcon = findViewById(R.id.tabShareIcon);

        if (isMyChildren) {
            myChildrenLabel.setTextColor(getColor(R.color.tab_selected));
            myChildrenIcon.setAlpha(1f);

            shareLabel.setTextColor(getColor(R.color.tab_unselected));
            shareIcon.setAlpha(0.4f);
        } else {
            shareLabel.setTextColor(getColor(R.color.tab_selected));
            shareIcon.setAlpha(1f);

            myChildrenLabel.setTextColor(getColor(R.color.tab_unselected));
            myChildrenIcon.setAlpha(0.4f);
        }
    }


    private TextView buildOption(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(18);
        tv.setPadding(16, 32, 16, 32);
        return tv;
    }
}
