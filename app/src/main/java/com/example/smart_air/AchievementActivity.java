package com.example.smart_air;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AchievementActivity extends AppCompatActivity {

    // Temporary mock values (simulate Firestore results)
    private int techniqueStreak = 5;         // pretend user has 5 technique streak
    private int controllerStreak = 3;        // pretend user has 3 controller streak

    private boolean hasBuddingStar = true;   // pretend user unlocked this badge
    private boolean hasShiningStar = true;   // pretend user unlocked this badge
    private boolean hasLuckyStar = true;     // pretend user unlocked this badge

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.achievements);

        setupStreaks();
        setupBadges();
    }

    private void setupStreaks() {

        ImageView imgTech = findViewById(R.id.imgTechniqueStreak);
        ImageView imgController = findViewById(R.id.imgControllerStreak);

        TextView txtTech = findViewById(R.id.txtTechniqueStreak);
        TextView txtController = findViewById(R.id.txtControllerStreak);

        // Apply streak text
        txtTech.setText(
                getString(R.string.technique_streak_text, techniqueStreak)
        );
        txtController.setText(
                getString(R.string.controller_streak_text, controllerStreak)
        );

        // Load flame animation
        Animation flame = AnimationUtils.loadAnimation(this, R.anim.flame_pulse);

        // Technique streak icon
        if (techniqueStreak > 0) {
            imgTech.setImageResource(R.drawable.technique_streak);
            imgTech.startAnimation(flame);
        } else {
            imgTech.clearAnimation();
            imgTech.setImageResource(R.drawable.empty_streak);
        }

        // Controller streak icon
        if (controllerStreak > 0) {
            imgController.setImageResource(R.drawable.controller_streak);
            imgController.startAnimation(flame);
        } else {
            imgController.clearAnimation();
            imgController.setImageResource(R.drawable.empty_streak);
        }
    }

    private void setupBadges() {

        ImageView imgBudding = findViewById(R.id.imgBuddingStar);
        ImageView imgShining = findViewById(R.id.imgShiningStar);
        ImageView imgLucky = findViewById(R.id.imgLuckyStar);

        // Load star animations
        Animation buddingAnim = AnimationUtils.loadAnimation(this, R.anim.budding_star_glow);
        Animation shiningAnim = AnimationUtils.loadAnimation(this, R.anim.shining_star_twinkle);
        Animation luckyAnim = AnimationUtils.loadAnimation(this, R.anim.lucky_star_sparkle);

        // Budding Star
        if (hasBuddingStar) {
            imgBudding.setVisibility(ImageView.VISIBLE);
            imgBudding.startAnimation(buddingAnim);
        }

        // Shining Star
        if (hasShiningStar) {
            imgShining.setVisibility(ImageView.VISIBLE);
            imgShining.startAnimation(shiningAnim);
        }

        // Lucky Star
        if (hasLuckyStar) {
            imgLucky.setVisibility(ImageView.VISIBLE);
            imgLucky.startAnimation(luckyAnim);
        }
    }
}
