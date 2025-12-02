package com.example.smart_air;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementActivity extends AppCompatActivity {

    private String childID;
    private FirebaseFirestore db;

    // Streak views
    private ImageView imgTechniqueStreak;
    private TextView txtTechniqueStreak;
    private ImageView imgControllerStreak;
    private TextView txtControllerStreak;

    // Badge views
    private TextView titleBuddingStar;
    private ImageView imgBuddingStar;
    private TextView txtBuddingStarDesc;

    private TextView titleShiningStar;
    private ImageView imgShiningStar;
    private TextView txtShiningStarDesc;

    private TextView titleLuckyStar;
    private ImageView imgLuckyStar;
    private TextView txtLuckyStarDesc;

    // Animations
    private Animation flameAnim;
    private Animation buddingAnim;
    private Animation shiningAnim;
    private Animation luckyAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.achievements);

        childID = getIntent().getStringExtra("childID");
        if (childID == null || childID.isEmpty()) {
            Toast.makeText(this, "Child ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        bindViews();
        loadAnimations();

        setupStreaksUI();
        setupBadgesUI();
    }

    private void bindViews() {
        imgTechniqueStreak = findViewById(R.id.imgTechniqueStreak);
        txtTechniqueStreak = findViewById(R.id.txtTechniqueStreak);
        imgControllerStreak = findViewById(R.id.imgControllerStreak);
        txtControllerStreak = findViewById(R.id.txtControllerStreak);

        titleBuddingStar = findViewById(R.id.titleBuddingStar);
        imgBuddingStar = findViewById(R.id.imgBuddingStar);
        txtBuddingStarDesc = findViewById(R.id.txtBuddingStarDesc);

        titleShiningStar = findViewById(R.id.titleShiningStar);
        imgShiningStar = findViewById(R.id.imgShiningStar);
        txtShiningStarDesc = findViewById(R.id.txtShiningStarDesc);

        titleLuckyStar = findViewById(R.id.titleLuckyStar);
        imgLuckyStar = findViewById(R.id.imgLuckyStar);
        txtLuckyStarDesc = findViewById(R.id.txtLuckyStarDesc);
    }

    private void loadAnimations() {
        flameAnim = AnimationUtils.loadAnimation(this, R.anim.flame_pulse);
        buddingAnim = AnimationUtils.loadAnimation(this, R.anim.budding_star_glow);
        shiningAnim = AnimationUtils.loadAnimation(this, R.anim.shining_star_twinkle);
        luckyAnim = AnimationUtils.loadAnimation(this, R.anim.lucky_star_sparkle);
    }


    private void setupStreaksUI() {
        loadTechniqueStreak();
        loadControllerStreak();
    }


    private void loadTechniqueStreak() {
        final DocumentReference streakRef =
                db.collection("techniqueStreak").document(childID);

        streakRef.get().addOnSuccessListener(snap -> {
            long days = 0;

            if (snap.exists()) {
                Long v = snap.getLong("consecutive_days");
                if (v != null) days = v;
            } else {
                Map<String, Object> init = new HashMap<>();
                init.put("consecutive_days", 0L);
                streakRef.set(init, SetOptions.merge());
            }

            if (days <= 0) {
                imgTechniqueStreak.clearAnimation();
                imgTechniqueStreak.setImageResource(R.drawable.empty_streak);
                txtTechniqueStreak.setText(getString(R.string.tech_streak_interrupted));
            } else {
                imgTechniqueStreak.setImageResource(R.drawable.technique_streak);
                imgTechniqueStreak.startAnimation(flameAnim);
                txtTechniqueStreak.setText(
                        getString(R.string.tech_streak_success, days)
                );
            }

        }).addOnFailureListener(e -> {
            imgTechniqueStreak.clearAnimation();
            imgTechniqueStreak.setImageResource(R.drawable.empty_streak);
            txtTechniqueStreak.setText(getString(R.string.tech_streak_unable_to_load));
        });
    }


    private void loadControllerStreak() {
        final DocumentReference streakRef =
                db.collection("controllerStreak").document(childID);

        streakRef.get().addOnSuccessListener(snap -> {
            long days = 0;

            if (snap.exists()) {
                Long v = snap.getLong("consecutive_days");
                if (v != null) days = v;
            } else {
                Map<String, Object> init = new HashMap<>();
                init.put("consecutive_days", 0L);
                streakRef.set(init, SetOptions.merge());
            }

            if (days <= 0) {
                imgControllerStreak.clearAnimation();
                imgControllerStreak.setImageResource(R.drawable.empty_streak);
                txtControllerStreak.setText(getString(R.string.controller_streak_interrupted));
            } else {
                imgControllerStreak.setImageResource(R.drawable.controller_streak);
                imgControllerStreak.startAnimation(flameAnim);
                txtControllerStreak.setText(
                        getString(R.string.controller_streak_success, days)
                );
            }

        }).addOnFailureListener(e -> {
            imgControllerStreak.clearAnimation();
            imgControllerStreak.setImageResource(R.drawable.empty_streak);
            txtControllerStreak.setText(getString(R.string.controller_streak_unable_to_load));
        });
    }


    private void setupBadgesUI() {

        titleBuddingStar.setText(getString(R.string.badge_budding_star_title));
        titleShiningStar.setText(getString(R.string.badge_shining_star_title));
        titleLuckyStar.setText(getString(R.string.badge_lucky_star_title));

        DocumentReference badgeRef =
                db.collection("badges").document(childID);

        badgeRef.get().addOnSuccessListener(snap -> {

            long buddingN = 1;
            long shiningN = 10;
            long luckyN = 4;

            if (snap.exists()) {
                Long v1 = snap.getLong("budding_star");
                Long v2 = snap.getLong("shining_star");
                Long v3 = snap.getLong("lucky_star");
                if (v1 != null && v1 >= 1) buddingN = v1;
                if (v2 != null && v2 >= 1) shiningN = v2;
                if (v3 != null && v3 >= 1) luckyN = v3;
            }

            setupBuddingStarBadge(buddingN);
            setupShiningStarBadge(shiningN);
            setupLuckyStarBadge(luckyN);

        }).addOnFailureListener(e -> {
            setupBuddingStarBadge(1);
            setupShiningStarBadge(10);
            setupLuckyStarBadge(4);
        });
    }


    private void setupBuddingStarBadge(long weeksThreshold) {

        CollectionReference schedulesCol = db.collection("planned-schedule")
                .document(childID)
                .collection("schedules");

        schedulesCol.get().addOnSuccessListener(snap -> {

            List<DocumentSnapshot> docs = new ArrayList<>(snap.getDocuments());
            docs.sort(Comparator.comparing(DocumentSnapshot::getId));

            int windowDays = (int) (7 * weeksThreshold);
            boolean unlocked = false;

            if (docs.size() >= windowDays) {
                for (int start = 0; start + windowDays - 1 < docs.size(); start++) {

                    boolean allTaken = true;

                    for (int i = 0; i < windowDays; i++) {
                        DocumentSnapshot d = docs.get(start + i);
                        Boolean taken = d.getBoolean("taken");
                        if (taken == null || !taken) {
                            allTaken = false;
                            break;
                        }
                    }

                    if (allTaken) {
                        unlocked = true;
                        break;
                    }
                }
            }


            updateBadgeCompletion("budding_star", unlocked);

            if (unlocked) {
                imgBuddingStar.setImageResource(R.drawable.budding_star);
                imgBuddingStar.startAnimation(buddingAnim);
                txtBuddingStarDesc.setText(
                        getString(R.string.budding_star_unlocked, weeksThreshold)
                );
            } else {
                imgBuddingStar.clearAnimation();
                imgBuddingStar.setImageResource(R.drawable.budding_star_empty);
                txtBuddingStarDesc.setText(
                        getString(R.string.budding_star_locked, weeksThreshold)
                );
            }

        }).addOnFailureListener(e -> {
            imgBuddingStar.clearAnimation();
            imgBuddingStar.setImageResource(R.drawable.budding_star_empty);
            txtBuddingStarDesc.setText(getString(R.string.budding_star_load_error));
        });
    }


    private void setupShiningStarBadge(long sessionsThreshold) {

        DocumentReference streakRef =
                db.collection("techniqueStreak").document(childID);

        streakRef.get().addOnSuccessListener(snap -> {
            long days = 0;
            if (snap.exists()) {
                Long v = snap.getLong("consecutive_days");
                if (v != null) days = v;
            }

            boolean unlocked = days >= sessionsThreshold;


            updateBadgeCompletion("shining_star", unlocked);

            if (unlocked) {
                imgShiningStar.setImageResource(R.drawable.shining_star);
                imgShiningStar.startAnimation(shiningAnim);
                txtShiningStarDesc.setText(
                        getString(R.string.shining_star_unlocked, sessionsThreshold)
                );
            } else {
                imgShiningStar.clearAnimation();
                imgShiningStar.setImageResource(R.drawable.shining_star_empty);
                txtShiningStarDesc.setText(
                        getString(R.string.shining_star_locked, sessionsThreshold)
                );
            }

        }).addOnFailureListener(e -> {
            imgShiningStar.clearAnimation();
            imgShiningStar.setImageResource(R.drawable.shining_star_empty);
            txtShiningStarDesc.setText(getString(R.string.shining_star_load_error));
        });
    }


    private void setupLuckyStarBadge(long maxRescueDays) {

        updateRescueCountersIfExpired(() -> {

            DocumentReference medlogParent =
                    db.collection("medlog").document(childID);

            medlogParent.get().addOnSuccessListener(snap -> {

                long rescueIn30 = 0;
                if (snap.exists()) {
                    Long v = snap.getLong("rescue_in_30_days");
                    if (v != null) rescueIn30 = v;
                }

                boolean unlocked = (rescueIn30 <= maxRescueDays && rescueIn30 >= 0);

                updateBadgeCompletion("lucky_star", unlocked);

                if (unlocked) {
                    imgLuckyStar.setImageResource(R.drawable.lucky_star);
                    imgLuckyStar.startAnimation(luckyAnim);
                    txtLuckyStarDesc.setText(
                            getString(R.string.lucky_star_unlocked, rescueIn30)
                    );
                } else {
                    imgLuckyStar.clearAnimation();
                    imgLuckyStar.setImageResource(R.drawable.lucky_star_empty);
                    txtLuckyStarDesc.setText(
                            getString(R.string.lucky_star_locked, maxRescueDays)
                    );
                }

            }).addOnFailureListener(e -> {
                imgLuckyStar.clearAnimation();
                imgLuckyStar.setImageResource(R.drawable.lucky_star_empty);
                txtLuckyStarDesc.setText(getString(R.string.lucky_star_load_error));
            });

        });
    }


    private void updateBadgeCompletion(String badgeKey, boolean unlocked) {

        DocumentReference badgeDoc =
                db.collection("badges").document(childID);

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> badgeMap = new HashMap<>();

        badgeMap.put(badgeKey, unlocked);
        data.put("allBadges", badgeMap);


        badgeDoc.set(data, SetOptions.merge());
    }

    private void updateRescueCountersIfExpired(Runnable onDone) {
        DocumentReference medLogRef =
                db.collection("medlog").document(childID);

        medLogRef.get().addOnSuccessListener(snap -> {

            if (!snap.exists()) {

                Map<String, Object> init = new HashMap<>();
                init.put("last_update_day", getToday());
                init.put("rescue_use_in_last_7_days", 0);
                init.put("rescue_in_30_days", 0);
                medLogRef.set(init, SetOptions.merge()).addOnSuccessListener(r -> onDone.run());
                return;
            }

            String lastUpdate = snap.getString("last_update_day");
            if (lastUpdate == null) lastUpdate = getToday();

            long rescue7 = 0;
            long rescue30 = 0;

            Long v7 = snap.getLong("rescue_use_in_last_7_days");
            Long v30 = snap.getLong("rescue_in_30_days");

            if (v7 != null) rescue7 = v7;
            if (v30 != null) rescue30 = v30;

            int daysDiff = getDaysBetween(lastUpdate, getToday());

            boolean updated = false;

            if (daysDiff > 7) {
                rescue7 = 0;
                updated = true;
            }
            if (daysDiff > 30) {
                rescue30 = 0;
            }

            if (updated) {
                Map<String, Object> map = new HashMap<>();
                map.put("last_update_day", getToday());
                map.put("rescue_use_in_last_7_days", rescue7);
                map.put("rescue_in_30_days", rescue30);

                medLogRef.set(map, SetOptions.merge())
                        .addOnSuccessListener(r -> onDone.run())
                        .addOnFailureListener(e -> onDone.run());
            } else {

                onDone.run();
            }

        }).addOnFailureListener(e -> onDone.run());
    }

    private String getToday() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    private int getDaysBetween(String d1, String d2) {
        try {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());

            java.util.Date date1 = sdf.parse(d1);
            java.util.Date date2 = sdf.parse(d2);

            if (date1 == null || date2 == null) return 0;

            long diffMillis = date2.getTime() - date1.getTime();
            long diffDays = diffMillis / (1000 * 60 * 60 * 24);

            return (int) diffDays;

        } catch (Exception e) {
            return 0;
        }
    }




}
