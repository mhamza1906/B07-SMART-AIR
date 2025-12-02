package com.example.smart_air;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ChildDashboardActivity extends AppCompatActivity {

    private View zoneColorView;
    private TextView percentView;
    private TextView zoneTextView;
    private String childId;
    TextView txtAvatar;
    String childfName;
    String childlName;
    String childUsername;
    String childDOB;

    private com.google.firebase.firestore.ListenerRegistration zoneListener;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.child_dashboard);

        childId = getIntent().getStringExtra("childID");
        if (childId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        loadParentInfoFromDatabase();

        txtAvatar = findViewById(R.id.txtChildAvatar);
        txtAvatar.setOnClickListener(v -> showUserInfoBottomSheet());

        findViewById(R.id.imgMenu).setOnClickListener(v -> openMenuBottomSheet());

        zoneColorView = findViewById(R.id.dashboard_zone_color);
        percentView = findViewById(R.id.dashboard_zone_percentage);
        zoneTextView = findViewById(R.id.dashboard_zone_text);

        listenForTodayZone(childId);

        setButtonListeners();
    }


    private void loadParentInfoFromDatabase() {
        FirebaseDatabase.getInstance().getReference("users")
                .child(childId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        childDOB = snapshot.child("birthday").getValue(String.class);
                        childfName = snapshot.child("fName").getValue(String.class);
                        childlName = snapshot.child("lName").getValue(String.class);
                        childUsername = snapshot.child("username").getValue(String.class);
                        setupUserAvatar();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }



    private void setupUserAvatar() {
        String initials = "U";
        if (childfName != null && childlName != null && !childfName.isEmpty())
            initials = ("" + childfName.charAt(0) + childlName.charAt(0)).toUpperCase();
        txtAvatar.setText(initials);
        txtAvatar.setOnClickListener(v -> showUserInfoBottomSheet());
    }

    private void showUserInfoBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.bottomsheet_child_info, null);
        dialog.setContentView(view);
        String fullNameText = getString(R.string.display_full_name, childfName != null ? childfName : "N/A", childlName != null ? childlName : "N/A");
        ((TextView) view.findViewById(R.id.txtUserFullName)).setText(fullNameText);
        ((TextView) view.findViewById(R.id.txtUserDOB)).setText(getString(R.string.dob, childDOB));
        ((TextView) view.findViewById(R.id.txtUsername)).setText(getString(R.string.display_username, childUsername));

        LinearLayout badgeLayout = view.findViewById(R.id.layoutChildBadges);
        ChildBadgeLoader.loadBadges(badgeLayout, childId);

        view.findViewById(R.id.btnSignOutChild).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivityView.class));
            finish();
        });
        dialog.show();
    }

    private void setButtonListeners() {

        findViewById(R.id.triagebutton).setOnClickListener(v -> {
            Intent i = new Intent(this, TriageActivity.class);
            i.putExtra("childID", childId);
            startActivity(i);
        });

    }

    private void openMenuBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        ViewGroup root = findViewById(android.R.id.content);

        View view = getLayoutInflater().inflate(
                R.layout.bottomsheet_child_menu,
                root,
                false
        );

        dialog.setContentView(view);

        setMenuItem(view, R.id.menuTakeMed,
                R.drawable.take_medicine,
                getString(R.string.menu_take_medicine),
                TakeMedicineActivity.class);

        setMenuItem(view, R.id.menuMedLog,
                R.drawable.medicine_log,
                getString(R.string.menu_medicine_log),
                MedlogActivity.class);

        setMenuItem(view, R.id.menuDailyCheckin,
                R.drawable.perform_daily_check_in,
                getString(R.string.menu_daily_checkin),
                DailyCheckInActivity.class);

        setMenuItem(view, R.id.menuPEF,
                R.drawable.peak_flow,
                getString(R.string.menu_enter_pef),
                InputPEFActivity.class);

        setMenuItem(view, R.id.menuSchedule,
                R.drawable.create_schedule,
                getString(R.string.menu_schedule),
                ChildScheduleActivity.class);

        setMenuItem(view, R.id.menuStreak,
                R.drawable.configure_rewards,
                getString(R.string.menu_achievements),
                AchievementActivity.class);

        dialog.show();
    }


    private void setMenuItem(View parent, int includeId, int iconDrawable, String label, Class<?> target) {

        View item = parent.findViewById(includeId);
        ImageView icon = item.findViewById(R.id.imgMenuIcon);
        TextView text = item.findViewById(R.id.txtMenuLabel);

        icon.setImageResource(iconDrawable);
        icon.setImageTintList(null);
        text.setText(label);

        item.setOnClickListener(v -> {
            Intent i = new Intent(this, target);
            i.putExtra("childID", childId);
            startActivity(i);
        });
    }


    private void listenForTodayZone(String userID) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        final DocumentReference todayRef = db.collection("PEF")
                .document(userID)
                .collection("log")
                .document(today);

        // Assign the listener to the variable, and remove 'this' from the call
        zoneListener = todayRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) return;

            if (snapshot != null && snapshot.exists()) {
                String zone = snapshot.getString("zone");
                Long percent = snapshot.getLong("percent");

                if (zone != null) {
                    zoneTextView.setText(
                            getString(R.string.dashboard_zone_label_in_java, zone)
                    );
                } else {
                    zoneTextView.setText(R.string.dashboard_zone_no_zone);
                }

                if (zone != null) {
                    switch (zone.toLowerCase()) {
                        case "green":
                            zoneColorView.setBackgroundColor(Color.GREEN);
                            break;
                        case "yellow":
                            zoneColorView.setBackgroundColor(Color.YELLOW);
                            break;
                        case "red":
                            zoneColorView.setBackgroundColor(Color.RED);
                            break;
                        default:
                            zoneColorView.setBackgroundColor(Color.GRAY);
                            break;
                    }
                } else {
                    zoneColorView.setBackgroundColor(Color.GRAY);
                }

                if (percent != null) {
                    percentView.setText(
                            getString(R.string.dashboard_zone_percent, percent)
                    );
                } else {
                    percentView.setText(R.string.dashboard_zone_percent_null);
                }

            } else {
                zoneTextView.setText(R.string.dashboard_zone_no_data);
                zoneColorView.setBackgroundColor(Color.GRAY);
                percentView.setText(R.string.dashboard_zone_percent_null);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadParentInfoFromDatabase();
        listenForTodayZone(childId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (zoneListener != null) {
            zoneListener.remove(); // Stop listening when the activity is no longer visible
        }
    }

}