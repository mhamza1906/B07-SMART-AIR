package com.example.smart_air;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChildBadgeLoader {

    public static void loadBadges(LinearLayout layout, String childId) {
        layout.removeAllViews();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        loadTechniqueStreakIcon(db, layout, childId);
        loadControllerStreakIcon(db, layout, childId);

        loadBadgeIcons(db, layout, childId);
    }


    private static void loadTechniqueStreakIcon(FirebaseFirestore db,
                                                LinearLayout layout,
                                                String childId) {

        DocumentReference streakRef =
                db.collection("techniqueStreak").document(childId);

        streakRef.get().addOnSuccessListener(snap -> {
            long days = 0;
            if (snap.exists()) {
                Long v = snap.getLong("consecutive_days");
                if (v != null) days = v;
            }

            if (days > 0) {
                addIconWithAnim(
                        layout,
                        R.drawable.technique_streak,
                        R.anim.flame_pulse
                );
            }
        });
    }

    private static void loadControllerStreakIcon(FirebaseFirestore db,
                                                 LinearLayout layout,
                                                 String childId) {

        DocumentReference streakRef =
                db.collection("controllerStreak").document(childId);

        streakRef.get().addOnSuccessListener(snap -> {
            long days = 0;
            if (snap.exists()) {
                Long v = snap.getLong("consecutive_days");
                if (v != null) days = v;
            }

            if (days > 0) {
                addIconWithAnim(
                        layout,
                        R.drawable.controller_streak,
                        R.anim.flame_pulse
                );
            }
        });
    }

    private static void loadBadgeIcons(FirebaseFirestore db,
                                       LinearLayout layout,
                                       String childId) {

        db.collection("badges")
                .document(childId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    if (Boolean.TRUE.equals(doc.getBoolean("allBadges.budding_star"))) {
                        addIconWithAnim(
                                layout,
                                R.drawable.budding_star,
                                R.anim.budding_star_glow
                        );
                    }

                    if (Boolean.TRUE.equals(doc.getBoolean("allBadges.shining_star"))) {
                        addIconWithAnim(
                                layout,
                                R.drawable.shining_star,
                                R.anim.shining_star_twinkle
                        );
                    }

                    if (Boolean.TRUE.equals(doc.getBoolean("allBadges.lucky_star"))) {
                        addIconWithAnim(
                                layout,
                                R.drawable.lucky_star,
                                R.anim.lucky_star_sparkle
                        );
                    }
                });
    }


    private static void addIconWithAnim(LinearLayout layout,
                                        int drawableId,
                                        int animId) {

        ImageView img = new ImageView(layout.getContext());
        img.setImageResource(drawableId);

        int size = (int) (layout.getResources().getDisplayMetrics().density * 42);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(size, size);
        params.setMargins(8, 4, 16, 4);

        img.setLayoutParams(params);
        layout.addView(img);

        Animation anim = AnimationUtils.loadAnimation(layout.getContext(), animId);
        img.startAnimation(anim);
    }
}
